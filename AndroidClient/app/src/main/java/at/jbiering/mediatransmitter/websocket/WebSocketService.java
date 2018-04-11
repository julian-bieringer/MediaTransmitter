package at.jbiering.mediatransmitter.websocket;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import at.jbiering.mediatransmitter.model.Device;
import at.jbiering.mediatransmitter.model.MediaFile;
import at.jbiering.mediatransmitter.model.enums.Action;
import at.jbiering.mediatransmitter.preferences.SharedPreferenceKeys;

//Thanks to Nilhcem for the websocket service
//https://github.com/Nilhcem/android-websocket-example/blob/master/websockets-example/src/main/java/com/nilhcem/websockets/WebSocketsService.java

public class WebSocketService extends Service {

    private final static String LOG_TAG = WebSocketService.class.getSimpleName();
    private final static String NETWORK_STATE_CHANGED = "networkStateChanged";

    private final static int WEBSOCKET_TIMEOUT_MS = 5000;

    public final IBinder binder = new WebSocketBinder();
    private WebSocket webSocket;
    private String userName;
    private Device currentDevice;
    private HashSet<Device> activeDevices;
    private CountDownLatch latch;

    //if internet goes down, stop websocket connection
    //if internet connection goes back online, reestablish websocket connection
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean networkAvailable = intent
                    .getBooleanExtra(NETWORK_STATE_CHANGED, false);

            if(networkAvailable)
                buildUpWebsocketConnection();
            else
                tearDownWebsocketConnection();
        }
    };

    private void buildUpWebsocketConnection() {
        String websocketConnectionUrl = readConnectionPathFromSharedPreferences();
        this.userName = readUserNameFromSharedPreferences();

        if(!StringUtils.isEmpty(websocketConnectionUrl) && !StringUtils.isEmpty(userName)) {
            WebSocketFactory factory = new WebSocketFactory();
            try {
                this.webSocket = factory
                        .createSocket(websocketConnectionUrl, WEBSOCKET_TIMEOUT_MS)
                        .setMaxPayloadSize(100*1024*1024);
                webSocket.addListener(new WebSocketAdapter() {

                    @Override
                    public void onTextMessage(final WebSocket websocket, final String text) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                MessageHelper.handleMessage(websocket, text, activeDevices,
                                        currentDevice, getApplicationContext());
                            }
                        }).start();
                    }

                    @Override
                    public void onError(WebSocket websocket,
                                        WebSocketException cause) {
                        Log.e(LOG_TAG, "ws: received error: " + cause.getCause().getMessage());
                    }

                    @Override
                    public void onConnected(final WebSocket websocket,
                                            Map<String, List<String>> headers) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                websocket.sendText(MessageHelper.createJsonAddMessage(userName));
                            }
                        }).start();
                    }

                    @Override
                    public void onDisconnected(WebSocket websocket,
                                               WebSocketFrame serverCloseFrame,
                                               WebSocketFrame clientCloseFrame,
                                               boolean closedByServer) {
                        Log.i(LOG_TAG, "ws: disconnected!");

                        //connection terminated, terminate service
                        latch.countDown();
                    }
                });
                webSocket.connect();
            } catch (IOException | WebSocketException e) {
                Log.e(LOG_TAG, "establishing ws connection failed with: " + e.getMessage());
            }
        }
    }

    public void refreshWebsocketServerSubscribers(){
        this.webSocket.sendText(MessageHelper.createJsonRetrieveSubscriberMessage());
    }

    private String readUserNameFromSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        String userName = sharedPreferences.getString(SharedPreferenceKeys.USER_NAME_KEY, "");

        return userName;
    }

    private String readConnectionPathFromSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        String serverIp = sharedPreferences
                .getString(SharedPreferenceKeys.SERVER_IP_KEY, "");
        String wsContextPath = sharedPreferences
                .getString(SharedPreferenceKeys.WEBSOCKET_CONTEXT_PATH_KEY, "");
        int serverPort = Integer
                .parseInt(sharedPreferences
                        .getString(SharedPreferenceKeys.SERVER_PORT_KEY, "-1"));

        //if user typed in context path beginning with '/', remove it as it will be added later
        wsContextPath = wsContextPath.startsWith("/") ? wsContextPath.substring(1) : wsContextPath;

        if (!StringUtils.isEmpty(serverIp)
                && !StringUtils.isEmpty(wsContextPath)
                && serverPort != -1) {
            return String.format("ws://%s:%d/%s", serverIp, serverPort, wsContextPath);
        }

        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LocalBroadcastManager
                .getInstance(this)
                .registerReceiver(messageReceiver, new IntentFilter(NETWORK_STATE_CHANGED));

        //start connection in new thread to avoid running it on main thread as a service
        //is run on the main thread
        Thread wsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                buildUpWebsocketConnection();
            }
        });
        wsThread.start();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        cleanService();
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        if(webSocket != null) {
            cleanService();
        }
        super.onDestroy();
    }

    private void cleanService(){
        Log.i(LOG_TAG, "starting clean up of service");
        LocalBroadcastManager
                .getInstance(this)
                .unregisterReceiver(messageReceiver);
        tearDownWebsocketConnection();
        this.webSocket = null;
    }

    @Override
    public IBinder onBind(Intent intent){
        Log.i(LOG_TAG, "binding to service...");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent){
        Log.i(LOG_TAG, "unbinding from service...");
        //return false because we don't like to have the service call onRebind when new clients
        //bind to it.
        //call onBind instead
        return false;
    }

    private void tearDownWebsocketConnection() {
        if(webSocket != null){
            latch = new CountDownLatch(1);
            webSocket.sendText(MessageHelper.createJsonRemoveMessage());

            //wait until server has closed ws connection
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendTextMessage(String text) {
        String jsonMessage = MessageHelper.createJsonTextMessage(text);
        webSocket.sendText(jsonMessage);
    }

    public void sendMediaFile(String fileExtension, Uri uri, long recipientId) {
        String uuid = UUID.randomUUID().toString();
        String jsonMessage = MessageHelper
                .createJsonCreateFileMessage(fileExtension, uri, recipientId, uuid,
                        getApplicationContext());
        webSocket.sendText(jsonMessage);
    }

    public final class WebSocketBinder extends Binder {
        public WebSocketService getService() {
            return WebSocketService.this;
        }
    }
}
