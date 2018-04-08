package at.jbiering.mediatransmitter.files;

import android.content.Context;
import android.content.ContextWrapper;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import at.jbiering.mediatransmitter.model.MediaFile;

public class FileHelper {

    public static String writeToInternalStorage(MediaFile mediaFile, Context applicationContext){
        //first give file a uuid to ensure it's name is unique
        //than calc a hash from the uuid with an hash algorithm
        //that is collision resistant. now you have a unique hash.
        //use the first two letters of the hash to create a two-folder-hierarchy,
        //the rest of the hash to name the file. -> ensures, that files are scattered
        //among multiple folders, so that no folder contains too many files at once

        String fileUuid = UUID.randomUUID().toString();
        String uuidHash = String.valueOf(DigestUtils.sha512(fileUuid));

        String fileName = String.format("%s.%s",
                mediaFile.getFileName(), mediaFile.getFileExtension());

        ContextWrapper cw = new ContextWrapper(applicationContext);

        String fullDirectoryPath = String
                .format("files/%s/%s", uuidHash.charAt(0), uuidHash.charAt(1));

        File directory = cw.getDir(fullDirectoryPath, Context.MODE_PRIVATE);

        File file = new File(directory, fileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(mediaFile.getBytes());
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

        return directory.getAbsolutePath();
    }
}
