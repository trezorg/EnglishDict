package by.trezor.android.EnglishDictApp;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


public class EnglishDictUtils {

    private static final String TAG = EnglishDictUtils.class.getSimpleName();
    public static final int ENGLISH_WORDS = 0;
    public static final int RUSSIAN_WORDS = 1;
    public static final String ENGLISH_WORDS_NAME = "en";
    public static final String RUSSIAN_WORDS_NAME = "ru";
    public static final Map<Integer, String> LANG_MAP = new HashMap<Integer, String>() {{
        put(ENGLISH_WORDS, ENGLISH_WORDS_NAME);
        put(RUSSIAN_WORDS, RUSSIAN_WORDS_NAME);
    }};
    static final private String RUSSIAN_LETTERS =
            "[-,абвгдеёжзийклмнопрстуфхцчшщьыъэюяАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЬЫЪЭЮЯ]";
    static final private String ENGLISH_LETTERS =
            "[-,abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ]";
    static final public String RUSSIAN_LETTERS_REGEXP =
            "^(" + RUSSIAN_LETTERS + "+\\p{Z})*" + RUSSIAN_LETTERS + "*$";
    static final public String ENGLISH_LETTERS_REGEXP =
            "^(" + ENGLISH_LETTERS + "+\\p{Z})*" + ENGLISH_LETTERS + "*$";
    static final Pattern RUSSIAN_LETTERS_PATTERN =
            Pattern.compile(RUSSIAN_LETTERS_REGEXP);
    static final Pattern ENGLISH_LETTERS_PATTERN =
            Pattern.compile(ENGLISH_LETTERS_REGEXP);

    private EnglishDictUtils () {}

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

    public static <T> boolean contains( final T[] array, final T v ) {
        for (final T e : array) {
            if (e == v || v != null && v.equals(e)) { return true; }
        }
        return false;
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

    static public String translate(String clientId, String clientKey, String text, int from, int to) {
        EnglishDictBingTranslate trans = new EnglishDictBingTranslate(clientId, clientKey);
        try {
            return trans.translate(text, LANG_MAP.get(from), LANG_MAP.get(to));
        } catch (Exception ex) {
            Log.d(TAG, "Translate error", ex);
            return null;
        }
    }

    static public String translate(Context context, String text, int from) {
        int to = from == ENGLISH_WORDS ? RUSSIAN_WORDS : ENGLISH_WORDS;
        String clientId = context.getResources().getString(R.string.translateClientId);
        String clientKey = context.getResources().getString(R.string.translateClientKey);
        return translate(clientId, clientKey, text, from, to);
    }

    static public String prepareFilePath(String fileName, String dir)
            throws IOException {
        File dictionaryRoot = new File(
                Environment.getExternalStorageDirectory(), dir);
        File dictionaryDirFile = new File(dictionaryRoot, fileName.substring(0,1));
        if (!dictionaryDirFile.exists()) {
            if (!dictionaryDirFile.mkdirs()) {
                throw new IOException("Cannot create directory: " + dictionaryDirFile);
            }
        }
        return new File(dictionaryDirFile, fileName + ".mp3").getAbsolutePath();
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
        FileOutputStream fileOutput = new FileOutputStream(filePath);
        BufferedInputStream inputStream =
                new BufferedInputStream(urlConnection.getInputStream());
        byte[] buffer = new byte[1024];
        int bufferLength;
        while ((bufferLength = inputStream.read(buffer)) > 0) {
            fileOutput.write(buffer, 0, bufferLength);
        }
        fileOutput.close();
        return filePath;
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
}
