package at.jbiering.mediatransmitter.websocket;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import com.neovisionaries.ws.client.WebSocket;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import at.jbiering.mediatransmitter.files.FileHelper;
import at.jbiering.mediatransmitter.model.Device;
import at.jbiering.mediatransmitter.model.OpenFile;
import at.jbiering.mediatransmitter.model.OpenTransmission;
import at.jbiering.mediatransmitter.model.enums.Action;

public class MessageHelper {

    private final static String LOG_TAG = MessageHelper.class.getSimpleName();

    //set file part size to 1kb
    private static final int FILE_PART_SIZE = 1024;

    //map for file sender with uri, recipient id and md5 checksum
    private static Map<String, OpenTransmission> openTransmissions;

    //map for file receiver with path to file
    private static Map<String, OpenFile> openFiles;

    public static String createJsonAddMessage(String userName){

        String osType = "android";
        String osVersion = Build.VERSION.RELEASE;
        String deviceBrand = Build.MANUFACTURER;
        String modelDescription = Build.MODEL;
        String ip = getLocalIpAddress();

        JSONObject addMessage = new JSONObject();
        try {
            addMessage.put("action", Action.ADD.toString().toLowerCase());
            addMessage.put("ip", ip);
            addMessage.put("name", userName);
            addMessage.put("type", deviceBrand);
            addMessage.put("modelDescription", modelDescription);
            addMessage.put("osType", osType);
            addMessage.put("osVersion", osVersion);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.i(LOG_TAG, "ws is sending: " + addMessage.toString());
        return addMessage.toString();
    }

    public static String createJsonTextMessage(String text){
        JSONObject addMessage = new JSONObject();
        try {
            addMessage.put("action", Action.SEND_CHAT_MESSAGE.toString().toLowerCase());
            addMessage.put("text_message", text);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.i(LOG_TAG, "ws is sending: " + addMessage.toString());
        return addMessage.toString();
    }

    public static void handleMessage(WebSocket websocket, String text,
                                     HashSet<Device> activeDevices, Device currentDevice,
                                     Context applicationContext) {
        try {
            JSONObject reader = new JSONObject(text);
            String actionString = reader.getString("action");
            Action action = Enum.valueOf(Action.class, actionString.toUpperCase());

            Log.i(LOG_TAG, "ws: received message with action [" + actionString + "]");

            String messageText = text.length() >= 200 ?
                    text.substring(0, 197) + "..." : text;

            Log.i(LOG_TAG, "ws: message as follows: " + messageText);

            if(action.equals(Action.ADD)){
                //device was registered by server, can now save the device with id
                currentDevice = MessageHelper.extractDeviceInfoFromJsonObject(reader);
            } else if (action.equals(Action.SUBSCRIBER_LIST_UPDATE_REQUIRED)){
                String retrieveMessage = MessageHelper.createJsonRetrieveSubscriberMessage();
                websocket.sendText(retrieveMessage);
            } else if (action.equals(Action.RETRIEVE_SUBSCRIBERS)){
                MessageHelper.retrieveActiveDevices(reader, activeDevices, applicationContext);
            } else if(action.equals(Action.CREATE_FILE_ACKNOWLEDGED)){
                MessageHelper.sendFileParts(reader, websocket, applicationContext);
            } else if(action.equals(Action.FILE_RECEIVED)){
                MessageHelper.sendEndFileMessage(reader, websocket);
            } else if(action.equals(Action.END_FILE_ACKNOWLEDGED)){
                OpenTransmission openTransmission = MessageHelper.removeOpenTransmission(reader);
                MessageHelper.broadcastFileAcknowledged(openTransmission, applicationContext);
                openTransmissions.remove(openTransmission);
            } else if(action.equals(Action.CREATE_FILE)){
                MessageHelper.openFile(reader, websocket, applicationContext);
            } else if(action.equals(Action.RETRIEVE_FILE_PART)){
                MessageHelper.writeFilePartToFile(reader, applicationContext, websocket);
            } else if(action.equals((Action.END_FILE_REQUEST))){
                MessageHelper.checkFileForCorruption(reader, applicationContext, websocket);
            }
        }catch (JSONException ex){

        }
    }

    private static void checkFileForCorruption(JSONObject jsonObject, Context applicationContext,
                                               WebSocket websocket) {
        try {
            String remoteMd5Checksum = jsonObject.getString("md5_checksum");
            String fileTransferUuid = jsonObject.getString("transfer_uuid");
            OpenFile openFile = openFiles.get(fileTransferUuid);

            String localMd5checkSum = FileHelper
                    .calcMd5Checksum(openFile, FILE_PART_SIZE, applicationContext);

            JSONObject endFileMessage = new JSONObject();
            endFileMessage.put("transfer_uuid", fileTransferUuid);

            if(remoteMd5Checksum.equals(localMd5checkSum)){
                endFileMessage
                        .put("action", Action.END_FILE_ACKNOWLEDGED.toString().toLowerCase());
            } else {
                endFileMessage
                        .put("action", Action.END_FILE_ERROR.toString().toLowerCase());
            }

            websocket.sendText(endFileMessage.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static void writeFilePartToFile(JSONObject jsonObject, Context applicationContext,
                                            WebSocket webSocket) {
        try {
            String fileTransferUuid = jsonObject.getString("transfer_uuid");
            int index = jsonObject.getInt("index");
            byte[] bytes = Base64
                    .decode(jsonObject.getString("bytes_base64"), Base64.DEFAULT);
            OpenFile openFile = openFiles.get(fileTransferUuid);

            synchronized (MessageHelper.class) {

                FileHelper
                        .appendToFile(openFile, index, bytes, FILE_PART_SIZE, applicationContext);

                if (fileFullyReceived(openFile)) {
                    FileHelper.createOrderedFileFromTmpFile(applicationContext, FILE_PART_SIZE,
                            openFile);
                    try {
                        JSONObject createFileReceivedMessage = new JSONObject();
                        createFileReceivedMessage.put("action",
                                Action.FILE_RECEIVED.toString().toLowerCase());
                        createFileReceivedMessage.put("transfer_uuid", fileTransferUuid);

                        webSocket.sendText(createFileReceivedMessage.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static boolean fileFullyReceived(OpenFile openFile) {

        boolean[] partsReceived = openFile.getFilePartsReceived();

        for (int i = 0; i < partsReceived.length; i++) {
            if(!partsReceived[i])
                return false;
        }
        return true;
    }

    private static void openFile(JSONObject jsonObject, WebSocket webSocket,
                                 Context applicationContext) {
        try {
            String fileTransferUuid = jsonObject.getString("transfer_uuid");
            String fileExtension = jsonObject.getString("file_extension");
            int fileParts = jsonObject.getInt("file_parts");
            String filePath = FileHelper
                    .createFileInInternalStorage(fileExtension, fileParts, FILE_PART_SIZE,
                            applicationContext);

            if(filePath != null){
                try {
                    JSONObject createFileAcknowledgeMessage = new JSONObject();
                    createFileAcknowledgeMessage.put("action",
                            Action.CREATE_FILE_ACKNOWLEDGED.toString().toLowerCase());
                    createFileAcknowledgeMessage.put("transfer_uuid", fileTransferUuid);

                    webSocket.sendText(createFileAcknowledgeMessage.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if(openFiles == null)
                openFiles = new HashMap<>();

            OpenFile openFile = new OpenFile(filePath, fileParts,
                    new boolean[fileParts], new int[fileParts]);
            openFiles.put(fileTransferUuid, openFile);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static OpenTransmission removeOpenTransmission(JSONObject jsonObject) {
        try {
            String fileTransferUuid = jsonObject.getString("transfer_uuid");
            OpenTransmission openTransmission = openTransmissions.get(fileTransferUuid);
            return openTransmission;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void broadcastFileAcknowledged(OpenTransmission openTransmission,
                                                  Context applicationContext) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(WebsocketBroadcastKeys.broadcastEndFileAcknowledgedAction);
        broadcastIntent
                .putExtra("transmission", openTransmission);
        applicationContext.sendBroadcast(broadcastIntent);
    }

    private static void sendEndFileMessage(JSONObject jsonObject, WebSocket websocket) {
        try {
            String fileTransferUuid = jsonObject.getString("transfer_uuid");
            OpenTransmission openTransmission = openTransmissions.get(fileTransferUuid);

            JSONObject endFileMessage = new JSONObject();
            endFileMessage.put("action", Action.END_FILE_REQUEST.toString().toLowerCase());
            endFileMessage.put("md5_checksum", openTransmission.getMd5Checksum());
            endFileMessage.put("transfer_uuid", fileTransferUuid);

            websocket.sendText(endFileMessage.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static void sendFileParts(JSONObject jsonObject, WebSocket webSocket,
                                      Context applicationContext) {
        try {
            String fileTransferUuid = jsonObject.getString("transfer_uuid");
            OpenTransmission openTransmission = openTransmissions.get(fileTransferUuid);

            InputStream inputStream = null;
            inputStream = applicationContext
                    .getContentResolver()
                    .openInputStream(openTransmission.getUri());

            int bufferSize = FILE_PART_SIZE;
            byte[] buffer = new byte[bufferSize];

            int index = 0;

            MessageDigest md5Digest = DigestUtils.getMd5Digest();

            while((inputStream.read(buffer)) != -1){
                md5Digest.update(buffer);
                sendFilePart(buffer, index, fileTransferUuid, webSocket);
                index++;
            }

            byte[] md5sum = md5Digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String checkSum = bigInt.toString(16);

            checkSum = String.format("%32s", checkSum).replace(' ', '0');
            openTransmission.setMd5Checksum(checkSum);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendFilePart(byte[] buffer,  int index,
                                     String fileTransferUuid, WebSocket webSocket) {
        JSONObject filePartMessage = new JSONObject();
        try {
            filePartMessage.put("action", Action.SEND_FILE_PART.toString().toLowerCase());
            filePartMessage.put("index", index);
            filePartMessage.put("transfer_uuid", fileTransferUuid);
            filePartMessage.put("bytes_base64", Base64
                    .encodeToString(buffer, Base64.DEFAULT));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        webSocket.sendText(filePartMessage.toString());
    }

    public static void retrieveActiveDevices(JSONObject reader, HashSet<Device> activeDevices,
                                             Context applicationContext) {
        //receive new list, so clear old one
        if(activeDevices == null)
            activeDevices = new HashSet<>();

        activeDevices.clear();

        try {
            JSONArray deviceArray = reader.getJSONArray("subscribers");

            for (int i = 0; i < deviceArray.length(); i++) {
                Device device = extractDeviceInfoFromJsonObject(deviceArray.getJSONObject(i));
                activeDevices.add(device);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(WebsocketBroadcastKeys.broadcastSubscribersReceivedAction);

        ArrayList<Device> activeDevicesList = new ArrayList<>();

        for(Device device : activeDevices)
            activeDevicesList.add(device);

        broadcastIntent
                .putParcelableArrayListExtra("active_devices", activeDevicesList);
        applicationContext.sendBroadcast(broadcastIntent);
    }

    public static String createJsonRemoveMessage(){
        JSONObject removeMessage = new JSONObject();
        try {
            removeMessage.put("action", Action.REMOVE.toString().toLowerCase());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return removeMessage.toString();
    }

    public static String createJsonRetrieveSubscriberMessage() {
        JSONObject retrieveMessage = new JSONObject();
        try {
            retrieveMessage.put("action", Action.RETRIEVE_SUBSCRIBERS.toString().toLowerCase());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return retrieveMessage.toString();
    }

    public static Device extractDeviceInfoFromJsonObject(JSONObject jsonObject)
            throws JSONException {
        long id = jsonObject.getInt("id");
        String ip = jsonObject.getString("ip");
        String name = jsonObject.getString("name");
        String status = jsonObject.getString("status");
        String modelDescription = jsonObject.getString("modelDescription");
        String type = jsonObject.getString("type");
        String osType = jsonObject.getString("osType");
        String osVersion = jsonObject.getString("osVersion");
        return new Device(id, ip, name, status, type, modelDescription, osType, osVersion);
    }

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                 en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
                     enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()  && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) {
            Log.e("IP Address", ex.toString());
        }
        return null;
    }

    public static String createJsonCreateFileMessage(String fileExtension, Uri uri,
                                                     long recipientId, String fileTransferUuid,
                                                     Context applicationContext) {

        if(openTransmissions == null)
            openTransmissions = new HashMap<>();

        OpenTransmission openTransmission = new OpenTransmission(uri, recipientId);
        openTransmissions.put(fileTransferUuid, openTransmission);


        try {
            InputStream inputStream = applicationContext.getContentResolver().openInputStream(uri);
            long size = inputStream.available();
            int fileParts = (int) Math.ceil((((double)size)/FILE_PART_SIZE));

            JSONObject createFileMessage = new JSONObject();
            createFileMessage.put("action", Action.CREATE_FILE.toString().toLowerCase());
            createFileMessage.put("recipient_id", recipientId);
            createFileMessage.put("file_extension", fileExtension);
            createFileMessage.put("file_parts", fileParts);
            createFileMessage.put("transfer_uuid", fileTransferUuid);

            Log.i(LOG_TAG, "ws is sending: " + createFileMessage.toString());
            return createFileMessage.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
