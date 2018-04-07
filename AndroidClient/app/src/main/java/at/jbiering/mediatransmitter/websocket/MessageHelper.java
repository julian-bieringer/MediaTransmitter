package at.jbiering.mediatransmitter.websocket;

import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

import at.jbiering.mediatransmitter.model.enums.Action;

public class MessageHelper {

    public static String createJsonAddMessage(String userName){

        String osType = "android";
        String osVersion = Build.VERSION.RELEASE;
        String deviceBrand = Build.MANUFACTURER;
        String modelDescription = Build.MODEL;

        JSONObject addMessage = new JSONObject();
        try {
            addMessage.put("action", Action.ADD.toString().toLowerCase());
            addMessage.put("name", userName);
            addMessage.put("type", deviceBrand);
            addMessage.put("modelDescription", modelDescription);
            addMessage.put("osType", osType);
            addMessage.put("osVersion", osVersion);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return addMessage.toString();
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
}
