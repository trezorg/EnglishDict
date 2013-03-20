package by.trezor.android.EnglishDictApp;


import android.os.Bundle;
import android.preference.PreferenceFragment;


public class EnglishDictPreferenceFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        addPreferencesFromResource(R.xml.preference);
    }

}
