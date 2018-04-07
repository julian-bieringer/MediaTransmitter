package at.jbiering.mediatransmitter.websocket;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

//Thanks to Nilhcem for the websocket service
//https://github.com/Nilhcem/android-websocket-example/blob/master/websockets-example/src/main/java/com/nilhcem/websockets/BaseSocketActivity.java

//instead of writing the service connection every time we want to use our service
//just write an base activity with the service connection and use this one instead
public class BaseWebSocketActivity extends AppCompatActivity {

    private final static String LOG_TAG = BaseWebSocketActivity.class.getSimpleName();

    protected WebSocketService webSocketService;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder serviceBinder) {
            Log.i(LOG_TAG, "connecting to ws service!");
            WebSocketService.WebSocketBinder binder =
                    (WebSocketService.WebSocketBinder) serviceBinder;
            webSocketService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(LOG_TAG, "disconnecting to ws service!");
            webSocketService = null;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, WebSocketService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        unbindService(serviceConnection);
        super.onStop();
    }
}
