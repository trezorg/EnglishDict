package by.trezor.android.EnglishDictApp;

import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;

public class EnglishDictBingTranslate {

    private String clientId;
    private String clientKey;

    EnglishDictBingTranslate(String clientId, String clientKey) {
        this.clientId = clientId;
        this.clientKey = clientKey;
    }

    String[] translate(String text, String from, String to) throws Exception {
        return translate(text, Language.fromString(from), Language.fromString(to));
    }

    String[] translate(String text, Language from, Language to) throws Exception {
        Translate.setClientId(clientId);
        Translate.setClientSecret(clientKey);
        return Translate.executeMany(text, from, to);
    }
}
