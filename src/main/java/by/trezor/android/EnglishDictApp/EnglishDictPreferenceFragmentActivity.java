package by.trezor.android.EnglishDictApp;

import android.preference.PreferenceActivity;

import java.util.List;

public class EnglishDictPreferenceFragmentActivity extends PreferenceActivity {

    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preferenceheaders, target);
    }

}

