package at.jbiering.mediatransmitter.websocket;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Parcelable;
import android.text.format.Formatter;
import android.util.Base64;
import android.util.Log;

import com.neovisionaries.ws.client.WebSocket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import at.jbiering.mediatransmitter.files.FileHelper;
import at.jbiering.mediatransmitter.model.Device;
import at.jbiering.mediatransmitter.model.MediaFile;
import at.jbiering.mediatransmitter.model.enums.Action;

import static android.content.Context.WIFI_SERVICE;

public class MessageHelper {

    private final static String LOG_TAG = MessageHelper.class.getSimpleName();

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

    public static void extractAndSaveFile(JSONObject reader, Context applicationContext) {
        try {
            String bytesBase64 = reader.getString("bytes_base64");
            byte[] bytes = Base64.decode(bytesBase64, Base64.DEFAULT);
            String fileName = reader.getString("file_name");
            String fileExtension = reader.getString("file_extension");
            MediaFile mediaFile = new MediaFile(bytes, fileName, fileExtension);
            FileHelper.writeToInternalStorage(mediaFile, applicationContext);
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
            } else if (action.equals(Action.RETRIEVE_FILE)){
                MessageHelper.extractAndSaveFile(reader, applicationContext);
            }
        }catch (JSONException ex){

        }
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
}
