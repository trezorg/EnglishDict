package by.trezor.android.EnglishDictApp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Toast;
import by.trezor.android.EnglishDictApp.provider.EnglishDictDescriptor;

import java.io.*;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;


public class EnglishDictHelper {

    private static final String TAG = EnglishDictHelper.class.getSimpleName();
    public static final int ENGLISH_WORDS = 0;
    public static final int RUSSIAN_WORDS = 1;
    protected static final int RESULT_SPEECH = 1013;
    protected static final int SHOW_PREFERENCES = 1014;
    protected static final int VOICE_WORD_TIMEOUT = 700;
    public static final String ENGLISH_WORDS_NAME = "en";
    public static final String RUSSIAN_WORDS_NAME = "ru";
    public static final String PLAY_SOUND_ON_SLIDE = "PLAY_SOUND_ON_SLIDE";
    public static final String TRAINING_WORDS_COUNT = "TRAINING_WORDS_COUNT";
    public static final SparseArray<String> LANG_MAP = new SparseArray<String>() {{
        put(ENGLISH_WORDS, ENGLISH_WORDS_NAME);
        put(RUSSIAN_WORDS, RUSSIAN_WORDS_NAME);
    }};
    public static final String[] PROJECTION = new String[] {
            EnglishDictDescriptor.EnglishDictBaseColumns.WORD,
            EnglishDictDescriptor.EnglishDictBaseColumns._ID,
            EnglishDictDescriptor.EnglishDictBaseColumns.RATING
    };

    static private Toast mToast;
    static public final String WORD = "word";
    static public final String WORD_ID = "word_id";
    static public final String WORD_POSITION = "word_position";
    static public final String ORDERING = "ordering";
    static public final String LANG_TYPE = "lang_type";
    static public final String RATING = "rating";
    static public int TRAINING_VIEW_PAGER_COUNT = 5;
    static final private String RUSSIAN_LETTERS =
            "[-,абвгдеёжзийклмнопрстуфхцчшщьыъэюяАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЬЫЪЭЮЯ]";
    static final private String ENGLISH_LETTERS =
            "[-,abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ]";
    static final public String RUSSIAN_LETTERS_REGEXP =
            "^(" + RUSSIAN_LETTERS + "+\\p{Z})*" + RUSSIAN_LETTERS + "*$";
    static final public String ENGLISH_LETTERS_REGEXP =
            "^(" + ENGLISH_LETTERS + "+\\p{Z})*" + ENGLISH_LETTERS + "*$";
    public static final Pattern RUSSIAN_LETTERS_PATTERN =
            Pattern.compile(RUSSIAN_LETTERS_REGEXP);
    public static final Pattern ENGLISH_LETTERS_PATTERN =
            Pattern.compile(ENGLISH_LETTERS_REGEXP);
    public final static String PARAM_STATUS = "status";
    public final static String PARAM_MESSAGE = "message";
    public final static String PARAM_WORD = "word";
    public final static String PARAM_TYPE = "type";
    public final static String PARAM_MESSAGER = "messager";
    public final static String PARAM_POSITION = "position";
    public final static int DEFAULT_POSITION = -1;
    public final static int VOICE_TYPE = 0;
    public final static String PARAM_NETWORK_AVAILABLE = "isNetworkAvailable";
    public final static String BROADCAST_ACTION = "by.trezor.android.EnglishDictApp.EnglishDictMainActivity";


    private EnglishDictHelper() {}

    public static String readAssetFile(Context context, String filename) throws IOException {
        return readAssetFile(context.getResources().getAssets(), filename);
    }

    public static String readAssetFile(AssetManager assetManager, String filename) throws IOException {

        StringBuilder res = new StringBuilder();
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(assetManager.open(filename)));
            String line;
            while ((line = bufferedReader.readLine()) != null) { res.append(line); }
        }
        catch (IOException e1){
            Log.e(TAG + "Cannot read file: " + filename, e1.getMessage());
            throw new IOException(e1);
        } finally {
            try {
                if (bufferedReader != null) { bufferedReader.close(); }
            } catch (Exception e2) {
                Log.e(TAG + "Cannot close reader: ", e2.getMessage());
            }
        }
        return res.toString();
    }

    public static String join(String r[], String d) {
        if (r.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        int i;
        for(i=0; i<r.length-1; i++) {
            sb.append(r[i]);
            sb.append(d);
        }
        return sb.toString()+r[i];
    }

    public static <T> String join(Iterable<T> iterable, String d) {
        Iterator it = iterable.iterator();
        if (!it.hasNext()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String prefix = "";
        while (it.hasNext()) {
            sb.append(prefix);
            prefix = d;
            sb.append(it.next());
        }
        return sb.toString();
    }

    public static <T> boolean contains(final T[] array, final T v) {
        for (final T e : array) {
            if (e == v || (v != null && v.equals(e))) return true;
        }
        return false;
    }

    public static Object[] getArray(Object val){
        int len = Array.getLength(val);
        Object[] outputArray = new Object[len];
        for(int i = 0; i < len; ++i){
            outputArray[i] = Array.get(val, i);
        }
        return outputArray;
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getList(Object... items){
        List<T> list = new ArrayList<T>();
        if (items.length == 1 && items[0].getClass().isArray()) {
            int length = Array.getLength(items[0]);
            for (int i = 0; i < length; i++) {
                Object element = Array.get(items[0], i);
                T item = (T)element;
                list.add(item);
            }
        } else {
            for (Object i : items) {
                T item = (T)i;
                list.add(item);
            }
        }
        return list;
    }

    static public String replaceNotExpectedPattern(String text, Pattern pattern, String repl) {
        if (repl == null || repl.isEmpty()) {
            repl = "";
        }
        while (!text.isEmpty()) {
            if (pattern.matcher(text).matches()) {
                return text;
            }
            // replace last position
            text = text.substring(0, text.length() - 1)  + repl;
        }
        return text;
    }

    static public String replaceNotExpectedPattern(String text, Pattern pattern) {
        return replaceNotExpectedPattern(text, pattern, null);
    }

    static public String prepareFilePath(String fileName, String dir)
            throws IOException {
        File dictionaryRoot = new File(
                Environment.getExternalStorageDirectory(), dir);
        File dictionaryDirFile = new File(dictionaryRoot, fileName.substring(0, 1));
        if (!dictionaryDirFile.exists()) {
            if (!dictionaryDirFile.mkdirs()) {
                throw new IOException("Cannot create directory: " + dictionaryDirFile);
            }
        }
        return new File(dictionaryDirFile, fileName + ".mp3").getCanonicalPath();
    }

    static public String prepareTmpFilePath(String fileName, String dir)
            throws IOException {
        File dictionaryRoot = new File(
                Environment.getExternalStorageDirectory(), dir);
        File dictionaryDirFile = new File(dictionaryRoot, "tmp");
        if (!dictionaryDirFile.exists()) {
            if (!dictionaryDirFile.mkdirs()) {
                throw new IOException("Cannot create directory: " + dictionaryDirFile);
            }
        }
        return new File(dictionaryDirFile, fileName + ".mp3").getCanonicalPath();
    }

    static public String downloadFile(String downloadUrl, String fileName, String dir)
            throws IOException {
        URL url = new URL(downloadUrl);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setDoOutput(true);
        urlConnection.connect();
        int code = urlConnection.getResponseCode();
        if (code > 300 || code == -1) {
            throw new IOException("Cannot read url: " + downloadUrl);
        }
        String filePath = prepareFilePath(fileName, dir);
        String tmpFilePath = prepareTmpFilePath(fileName, dir);
        FileOutputStream fileOutput = new FileOutputStream(tmpFilePath);
        BufferedInputStream inputStream =
                new BufferedInputStream(urlConnection.getInputStream());
        byte[] buffer = new byte[1024];
        int bufferLength;
        while ((bufferLength = inputStream.read(buffer)) > 0) {
            fileOutput.write(buffer, 0, bufferLength);
        }
        fileOutput.close();
        // move tmp to destination
        copyFile(new File(tmpFilePath), new File(filePath));
        return filePath;
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if(!destFile.exists()) {
            if (!destFile.createNewFile()) {
                throw new IOException("Cannot create file: " + destFile.getCanonicalFile());
            }
        }
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            long count = 0;
            long size = source.size();
            while ((count += destination.transferFrom(source, count, size - count)) < size);
        }
        finally {
            if(source != null) {
                source.close();
            }
            if(destination != null) {
                destination.close();
            }
        }
    }

    static public boolean areAllTrue(boolean[] array) {
        for (boolean b : array) if(!b) return false;
        return true;
    }

    static public String checkFile(String fileName, String dir) {
        File dictionaryRoot = new File(
                Environment.getExternalStorageDirectory(), dir);
        File dictionaryFile = new File(dictionaryRoot, fileName.substring(0,1));
        File filePath = new File(dictionaryFile, fileName + ".mp3");
        if (filePath.exists()) {
            return filePath.getAbsolutePath();
        }
        return null;
    }

    static public int getCursorPositionForId(Cursor cursor, long id) {
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            if (cursor.getLong(cursor.getColumnIndexOrThrow(
                    EnglishDictDescriptor.EnglishDictBaseColumns._ID)) == id) {
                return cursor.getPosition();
            }
            cursor.moveToNext();
        }
        return cursor.getPosition();
    }

    static void setInputLanguage(int lang) {
        String langCode = lang == ENGLISH_WORDS ? "EN" : "RU";
        Resources res = Resources.getSystem();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        Locale locale = new Locale(langCode.toLowerCase());
        Locale.setDefault(locale);
        conf.locale = locale;
        res.updateConfiguration(conf, dm);
    }

    static void setVolume(Context context, int percent) {
        AudioManager audioManager =
                (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        //int volume = (maxVolume * percent) / 100;
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    }

    public static void showToast(Context context, String message) {
        if (mToast == null || mToast.getView().getWindowVisibility() != View.VISIBLE) {
            mToast = Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(message);
        }
        mToast.show();
    }

    static boolean isPronouncedSound(Context context) {
        Context applicationContext = context.getApplicationContext();
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(applicationContext);
        return preferences.getBoolean(PLAY_SOUND_ON_SLIDE, true);
    }

    public static int getTrainingWordsSetting(Context context) {
        Context applicationContext = context.getApplicationContext();
        SharedPreferences preferences = PreferenceManager.
                getDefaultSharedPreferences(applicationContext);
        String count = preferences.getString(
                TRAINING_WORDS_COUNT, Integer.toString(TRAINING_VIEW_PAGER_COUNT));
        try {
            return Integer.parseInt(count);
        } catch (NumberFormatException ex) {
            return TRAINING_VIEW_PAGER_COUNT;
        }
    }

    public static void showPreferences(Activity activity) {
        Class c = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB ?
                EnglishDictPreferenceActivity.class :
                EnglishDictPreferenceFragmentActivity.class;
        Intent intent = new Intent(activity, c);
        activity.startActivityForResult(intent, SHOW_PREFERENCES);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    static class AddWordAsyncTask extends AsyncTask<Void, Void, AddWordAsyncTask.AddWordResult<String, Long, Integer>> {

        private String word;
        private OnExecuteListener mOnFinishListener;

        static class AddWordResult<Word, Id, Position> {

            private Word word;
            private Id id;
            private Position position;

            AddWordResult(Word word, Id id, Position position) {
                this.word = word;
                this.id = id;
                this.position = position;
            }

            public Word getWord() {
                return word;
            }

            public Id getId() {
                return id;
            }

            public Position getPosition() {
                return position;
            }

        }

        static interface OnExecuteListener {
            void onExecute(AddWordResult<String, Long, Integer> result);
            void onPreExecute();
            AddWordResult<String, Long, Integer> onBackground(String word);
        }

        AddWordAsyncTask (String word) {
            super();
            this.word = word;
        }

        public AddWordAsyncTask setOnQueryTextListener(OnExecuteListener listener) {
            mOnFinishListener = listener;
            return this;
        }

        @Override
        protected void onPreExecute() {
            if (mOnFinishListener != null) {
                mOnFinishListener.onPreExecute();
            }
        }

        @Override
        protected AddWordResult<String, Long, Integer> doInBackground(Void... args) {
            if (mOnFinishListener != null) {
                return mOnFinishListener.onBackground(word);
            }
            return null;
        }

        @Override
        protected void onPostExecute(AddWordResult<String, Long, Integer> result) {
            if (mOnFinishListener != null) {
                mOnFinishListener.onExecute(result);
            }
        }
    }
}
