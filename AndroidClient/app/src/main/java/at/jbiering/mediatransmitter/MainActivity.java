package at.jbiering.mediatransmitter;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;

import at.jbiering.mediatransmitter.preferences.SharedPreferenceKeys;
import at.jbiering.mediatransmitter.preferences.TransmitterPreferenceActivity;
import at.jbiering.mediatransmitter.websocket.BaseWebSocketActivity;
import at.jbiering.mediatransmitter.websocket.WebSocketService;

public class MainActivity extends BaseWebSocketActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private RecyclerView recyclerViewOpenChats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        String userName = sharedPreferences.getString(SharedPreferenceKeys.USER_NAME_KEY, "");

        Intent intent = new Intent(this, WebSocketService.class);
        startService(intent);

        if(StringUtils.isEmpty(userName)) {
            Toast
                    .makeText(this, "Please specify username in settings!",
                            Toast.LENGTH_LONG)
                    .show();
        }

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        this.recyclerViewOpenChats = findViewById(R.id.recyclerViewOpenChats);
    }

    @Override
    protected void onStart() {
        Log.i(LOG_TAG, "onStart called!");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.i(LOG_TAG, "onStop called!");
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, TransmitterPreferenceActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
