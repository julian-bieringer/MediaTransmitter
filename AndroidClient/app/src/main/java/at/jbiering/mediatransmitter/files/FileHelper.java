package at.jbiering.mediatransmitter.files;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.UUID;

import at.jbiering.mediatransmitter.model.MediaFile;
import at.jbiering.mediatransmitter.model.OpenFile;

public class FileHelper {

    private static final String LOG_TAG = FileHelper.class.getSimpleName();

    public static String createFileInInternalStorage(String fileExtension,
                                                     int fileParts, int filePartSize,
                                                     Context applicationContext){
        //first give file a uuid to ensure it's name is unique
        //than calc a hash from the uuid with an hash algorithm
        //that is collision resistant. now you have a unique hash.
        //use the first two letters of the hash to create a two-folder-hierarchy,
        //the rest of the hash to name the file. -> ensures, that files are scattered
        //among multiple folders, so that no folder contains too many files at once

        String fileUuid = UUID.randomUUID().toString();
        String uuidHash = new String(Hex.encodeHex(DigestUtils.sha512(fileUuid)));

        String fileName = String.format("%s_tmp.%s",
                uuidHash.substring(2, uuidHash.length()-1),
                fileExtension);

        ContextWrapper cw = new ContextWrapper(applicationContext);

        //create first folder if it does not exist already
        File firstFolder = new File(cw.getExternalFilesDir(null),
                String.valueOf(uuidHash.charAt(0)));
        firstFolder.mkdir();

        //create second folder if it does not exist already
        File secondFolder = new File(firstFolder,  String.valueOf(uuidHash.charAt(1)));
        secondFolder.mkdir();

        File file = new File(secondFolder, fileName);

        Log.i(LOG_TAG, "attempting to write file to: " + file.getAbsolutePath());

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return file.getAbsolutePath();
    }

    public static void appendToFile(OpenFile openFile, int index, byte[] bytes, int filePartSize,
                                    Context applicationContext) {

        String filePath = openFile.getFilePath();
        int fileParts = openFile.getFileParts();

        File file = new File(filePath);

        FileOutputStream fos = null;
        try {
            //true parameter to signalise that we want to append to the file
            fos = new FileOutputStream(file, true);
            fos.write(bytes, 0, bytes.length);
            fos.flush();

            openFile.getFilePartsReceived()[index] = true;

            int blocksWritten = (int)(file.length()/filePartSize);
            openFile.getFilePartIndices()[blocksWritten-1] = index;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String calcMd5Checksum(OpenFile openFile, int partSize,
                                         Context applicationContext) {
        File file = new File(openFile.getFilePath());
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            byte[] buffer = new byte[partSize];

            int index = 0;

            MessageDigest md5Digest = DigestUtils.getMd5Digest();

            while((inputStream.read(buffer)) != -1){
                md5Digest.update(buffer);
            }

            byte[] md5sum = md5Digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String checkSum = bigInt.toString(16);

            checkSum = String.format("%32s", checkSum).replace(' ', '0');
            return checkSum;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void createOrderedFileFromTmpFile(Context applicationContext,
                                                    int filePartSize, OpenFile openFile) {

        File unorderedFile = new File(openFile.getFilePath());
        File orderedFile = new File(openFile.getFilePath().replace("_tmp", ""));
        int[] indices = openFile.getFilePartIndices();

        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            //true parameter to signalise that we want to append to the file
            fos = new FileOutputStream(orderedFile, true);

            for (int i = 0; i < indices.length; i++) {
                int indexInFile = findPositionInFileForBlockIndex(i, indices);
                byte[] block = new byte[filePartSize];

                fis = new FileInputStream(unorderedFile);
                BufferedInputStream bis = new BufferedInputStream(fis);

                bis.skip(filePartSize*indexInFile);
                bis.read(block, 0, block.length);

                fos.write(block, 0, block.length);
            }
            fos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fis.close();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        unorderedFile.delete();
        openFile.setFilePath(orderedFile.getAbsolutePath());
    }

    private static int findPositionInFileForBlockIndex(int i, int[] indices) {
        for (int j = 0; j < indices.length; j++) {
            if(indices[j] == i)
                return j;
        }
        return -1;
    }
}
