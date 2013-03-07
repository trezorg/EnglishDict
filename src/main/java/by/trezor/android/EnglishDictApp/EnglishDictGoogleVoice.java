package by.trezor.android.EnglishDictApp;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import static by.trezor.android.EnglishDictApp.EnglishDictUtils.checkFile;
import static by.trezor.android.EnglishDictApp.EnglishDictUtils.downloadFile;

public class EnglishDictGoogleVoice {

    private static final String TAG = EnglishDictGoogleVoice.class.getSimpleName();
    private static final String URL_ENGLISH_VOICE =
            "http://ssl.gstatic.com/dictionary/static/sounds/de/0/%s.mp3";
    private static volatile EnglishDictGoogleVoice instance;
    private static final String DICTIONARY_FILES_DIRECTORY = "dictMp3";

    private EnglishDictGoogleVoice() {}

    public static EnglishDictGoogleVoice getInstance() {
        EnglishDictGoogleVoice localInstance = instance;
        if (localInstance == null) {
            synchronized (EnglishDictGoogleVoice.class) {
                localInstance = instance;
                if (localInstance == null) {
                    localInstance = new EnglishDictGoogleVoice();
                    instance = localInstance;
                }
            }
        }
        return localInstance;
    }

    private String getUrlEnglishVoiceUrl(String word) {
        return String.format(URL_ENGLISH_VOICE, word);
    }

    void play(String word) {
        String localPath = checkFile(word, DICTIONARY_FILES_DIRECTORY);
        if (localPath == null) {
            String url = getUrlEnglishVoiceUrl(word);
            try {
                localPath = downloadFile(url, word, DICTIONARY_FILES_DIRECTORY);
            } catch (IOException ex) {
                Log.e(TAG, "Cannot download file: " + url, ex);
                return;
            }
        }
        playFile(localPath);
    }

    void playFile(String path) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener(){
            @Override
            public boolean onError (MediaPlayer mp, int what, int extra) {
                mp.release();
                return false;
            }
        });
        try {
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
        } catch (Exception ex) {
            Log.e(TAG, "Cannot play file: " + path, ex);
            mediaPlayer.release();
            return;
        }
        mediaPlayer.start();
    }
}
