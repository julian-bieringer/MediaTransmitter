package at.jbiering.mediatransmitter.preferences;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import at.jbiering.mediatransmitter.R;

public class TransmitterPreferenceFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.fragment_transmitter_preferences);
    }
}
