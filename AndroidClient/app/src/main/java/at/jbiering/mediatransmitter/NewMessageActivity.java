package at.jbiering.mediatransmitter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import at.jbiering.mediatransmitter.model.Device;
import at.jbiering.mediatransmitter.websocket.BaseWebSocketActivity;
import at.jbiering.mediatransmitter.websocket.WebsocketBroadcastKeys;

public class NewMessageActivity extends BaseWebSocketActivity {

    private IntentFilter intentFilter;
    private RecyclerView recyclerViewActiveDevices;
    private RecyclerViewActiveDevicesAdapter recyclerViewActiveDevicesAdapter;
    private List<Device> activeDevices;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WebsocketBroadcastKeys
                    .broadcastSubscribersReceivedAction)) {
                //subscriber set has changed
                ArrayList<Device> activeDevicesArray =
                        intent.getParcelableArrayListExtra("active_devices");
                if (activeDevices != null) {
                    activeDevices.clear();
                    for(Device device : activeDevicesArray)
                        activeDevices.add(device);

                    recyclerViewActiveDevicesAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onWebsocketServiceBounded() {
        super.onWebsocketServiceBounded();

        //send retrieve_subscribers event to server and wait for the reply
        //which comes in via a broadcast message from the websocket service
        webSocketService.refreshWebsocketServerSubscribers();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onPause() {
        unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_message);

        this.activeDevices = new ArrayList<>();

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        this.recyclerViewActiveDevicesAdapter =
                new RecyclerViewActiveDevicesAdapter(activeDevices);

        recyclerViewActiveDevices = findViewById(R.id.recyclerViewActiveDevices);
        this.recyclerViewActiveDevices.setLayoutManager(layoutManager);
        this.recyclerViewActiveDevices.setAdapter(recyclerViewActiveDevicesAdapter);
        this.recyclerViewActiveDevices
                .addItemDecoration(new RecyclerViewDividerItemDecoration(this));

        this.intentFilter = new IntentFilter();
        intentFilter.addAction(WebsocketBroadcastKeys.broadcastSubscribersReceivedAction);
    }
}
