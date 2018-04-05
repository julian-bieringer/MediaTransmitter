package at.jbiering.mediatransmitter;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.List;

public class ServerPreferenceActivity extends PreferenceActivity{

    private static final String LOG_TAG = ServerPreferenceActivity.class.getSimpleName();
    private static final String SERVER_IP_KEY = "SERVER_IP";
    private static final String WEBSOCKET_CONTEXT_PATH_KEY = "WS_CONTEXT_PATH";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new ServerPreferenceFragment())
                .commit();

        checkValues();
    }

    private void checkValues() {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());
        String serverIp = sharedPreferences
                .getString(SERVER_IP_KEY, "");
        String wsContextPath = sharedPreferences
                .getString(WEBSOCKET_CONTEXT_PATH_KEY, "");

        Log.i(LOG_TAG,
                String.format("checkValues: {\"server_ip\" : \"%s\", \"ws_context_path\" : \"%s\"}",
                        serverIp, wsContextPath));
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return ServerPreferenceFragment.class.getName().equals(fragmentName);
    }
}
