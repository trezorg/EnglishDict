package by.trezor.android.EnglishDictApp;


import android.os.Bundle;
import android.preference.PreferenceActivity;


public class EnglishDictPreferenceActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        addPreferencesFromResource(R.xml.preference);
    }

}
