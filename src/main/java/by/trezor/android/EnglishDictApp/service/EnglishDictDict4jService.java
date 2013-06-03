package by.trezor.android.EnglishDictApp.service;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import by.trezor.android.EnglishDictApp.dict4j.EnglishDictDict4j;

import java.util.*;


import static by.trezor.android.EnglishDictApp.EnglishDictHelper.*;
import static by.trezor.android.EnglishDictApp.provider.EnglishDictDescriptor.EnglishDictBaseColumns.*;
import static by.trezor.android.EnglishDictApp.provider.EnglishDictDescriptor.EnglishDictDetailByNameEnglishWords;
import static by.trezor.android.EnglishDictApp.provider.EnglishDictDescriptor.EnglishDictDetailByNameRussianWords;


public class EnglishDictDict4jService extends IntentService {

    public EnglishDictDict4jService() {
        super("EnglishDictDict4jService");
    }

    public static class EnglishDictDict4jServiceResult {

        private final CharSequence[] cs;
        private final boolean[] checked;
        private final String word;
        private final int lang;

        public EnglishDictDict4jServiceResult(String word, int lang, CharSequence[] cs, boolean[] checked) {
            this.cs = cs;
            this.checked = checked;
            this.word = word;
            this.lang = lang;
        }

        public String getWord() {
            return word;
        }

        public int getLang() {
            return lang;
        }

        public CharSequence[] getCs() {
            return cs;
        }

        public boolean[] getChecked() {
            return checked;
        }

    }

    private void sendMessage(Messenger messenger, int status, EnglishDictDict4jServiceResult st) {
        Message msg = Message.obtain();
        msg.what = status;
        msg.obj = st;
        try {
            messenger.send(msg);
        } catch (android.os.RemoteException e1) {
            Log.e(getClass().getName(), "Exception sending message", e1);
        }
    }

    private Uri getContentUri(String word, int lang) {
        Uri uri;
        switch (lang) {
            case ENGLISH_WORDS:
                uri = EnglishDictDetailByNameRussianWords.CONTENT_URI;
                break;
            case RUSSIAN_WORDS:
                uri = EnglishDictDetailByNameEnglishWords.CONTENT_URI;
                break;
            default:
                throw new IllegalArgumentException("Unknown type: " + lang);
        }
        String queryString = QUERY_RELATION_WORD + "=" + word;
        return Uri.parse(uri + "?" + queryString);
    }

    private List<String> getExistentValues(String word, int lang) {
        Uri uri = getContentUri(word, lang);
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        List<String> list = new ArrayList<String>();
        while (cursor.moveToNext()) list.add(cursor.getString(1));
        cursor.close();
        return list;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final String word = intent.getStringExtra(PARAM_WORD);
        final int lang = intent.getIntExtra(LANG_TYPE, ENGLISH_WORDS);
        final Boolean isNetworkAvailable =
                intent.getBooleanExtra(PARAM_NETWORK_AVAILABLE, false);
        final Bundle extras = intent.getExtras();
        final Messenger messenger = (Messenger) extras.get(PARAM_MESSAGER);
        final List<String> existentValues = getExistentValues(word, lang);
        Set<String> words = new HashSet<String>();
        words.addAll(existentValues);
        if (isNetworkAvailable) {
            EnglishDictDict4j dict = new EnglishDictDict4j();
            List<String> newValues = dict.getWords(word, lang);
            words.addAll(newValues);
        }
        CharSequence[] res = words.toArray(new CharSequence[words.size()]);
        boolean[] checked = new boolean[res.length];
        for (int i = 0; i < res.length; i++) {
            checked[i] = existentValues.contains(res[i].toString());
        }
        sendMessage(messenger, Activity.RESULT_OK,
                new EnglishDictDict4jServiceResult(word, lang, res, checked));
    }
}