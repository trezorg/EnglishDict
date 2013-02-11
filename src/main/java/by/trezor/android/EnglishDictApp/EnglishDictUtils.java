package by.trezor.android.EnglishDictApp;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;


public class EnglishDictUtils {

    private static final String TAG = EnglishDictUtils.class.getSimpleName();
    public static final int ENGLISH_WORDS = 0;
    public static final int RUSSIAN_WORDS = 1;
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
}
