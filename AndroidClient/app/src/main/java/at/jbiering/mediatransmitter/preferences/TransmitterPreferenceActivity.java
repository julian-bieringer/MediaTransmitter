package at.jbiering.mediatransmitter.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

public class TransmitterPreferenceActivity extends PreferenceActivity{

    private static final String LOG_TAG = TransmitterPreferenceActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new TransmitterPreferenceFragment())
                .commit();

        checkValues();
    }

    private void checkValues() {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());
        String serverIp = sharedPreferences
                .getString(SharedPreferenceKeys.SERVER_IP_KEY, "");
        String wsContextPath = sharedPreferences
                .getString(SharedPreferenceKeys.WEBSOCKET_CONTEXT_PATH_KEY, "");
        int serverPort = Integer
                .parseInt(sharedPreferences
                        .getString(SharedPreferenceKeys.SERVER_PORT_KEY, "-1"));

        Log.i(LOG_TAG, String.format("checkValues: {\"server_ip\" : \"%s\"," +
                        " \"server_port\" : \"%d\", \"ws_context_path\" : \"%s\"}",
                        serverIp, serverPort, wsContextPath));
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return TransmitterPreferenceFragment.class.getName().equals(fragmentName);
    }
}
