package by.trezor.android.EnglishDictApp;

import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static by.trezor.android.EnglishDictApp.EnglishDictUtils.checkFile;
import static by.trezor.android.EnglishDictApp.EnglishDictUtils.downloadFile;


public class EnglishDictGoogleVoice {

    private static final String TAG = EnglishDictGoogleVoice.class.getSimpleName();
    private static final String URL_ENGLISH_VOICE =
            "http://ssl.gstatic.com/dictionary/static/sounds/de/0/%s.mp3";
    private static volatile EnglishDictGoogleVoice instance;
    private static final String DICTIONARY_FILES_DIRECTORY = "dictMp3";
    private MediaPlayer mediaPlayer;
    private Queue<String> mediaQueue = new ConcurrentLinkedQueue<String>();
    private Queue<OnExecuteListener> onExecuteListener =
            new ConcurrentLinkedQueue<OnExecuteListener>();
    private OnErrorListener onErrorListener;

    private EnglishDictGoogleVoice() {}

    public interface OnExecuteListener {
        void onExecute();
    }

    public interface OnErrorListener {
        void onError(String message);
    }

    void addOnExecuteListener(OnExecuteListener listener) {
        onExecuteListener.add(listener);
    }

    void setOnErrorListener(OnErrorListener listener) {
        onErrorListener = listener;
    }

    OnErrorListener getOnErrorListener() {
        return onErrorListener;
    }

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

    void prepareError(String message) {
        Log.e(TAG, message);
        if (onErrorListener != null) {
            onErrorListener.onError(message);
        }
    }

    void prepareError(String message, Throwable ex) {
        Log.e(TAG, message, ex);
        if (onErrorListener != null) {
            onErrorListener.onError(message);
        }
    }

    private String getFilePath(String word) throws IOException {
        String localPath = checkFile(word, DICTIONARY_FILES_DIRECTORY);
        if (localPath == null) {
            String url = getUrlEnglishVoiceUrl(word);
            localPath = downloadFile(url, word, DICTIONARY_FILES_DIRECTORY);
        }
        return localPath;
    }

    void play(String[] words) {
        for (String word: words) {
            try {
                mediaQueue.add(getFilePath(word));
            } catch (IOException ex) {
                prepareError("Cannot download file: " + getUrlEnglishVoiceUrl(word), ex);
            }
        }
        playQueue();
    }

    private void onExecute() {
        if (onExecuteListener.isEmpty()) {
            return;
        }
        OnExecuteListener onExecute;
        while ((onExecute = onExecuteListener.poll()) != null) {
            onExecute.onExecute();
        }
    }

    private boolean ensureMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            return true;
        }
        return false;
    }

    private void onFinish(MediaPlayer mp) {
        if (mp != null) {
            mp.stop();
            mp.release();
        }
        mediaQueue.clear();
        onExecute();
    }

    void onFinish() {
        onFinish(mediaPlayer);
        mediaPlayer = null;
    }

    private void onStart(MediaPlayer mp) {
        String path = mediaQueue.poll();
        if (path == null) {
            return;
        }
        try {
            mp.setDataSource(path);
            mp.prepare();
            mp.start();
        } catch (Exception ex) {
            prepareError("Cannot play file: " + path, ex);
            onExecute();
        }
    }

    private void onStart(MediaPlayer mp, boolean isNew) {
        if (!isNew) {
            mp.reset();
        }
        onStart(mp);
    }

    private void onStart() {
        boolean isNew = ensureMediaPlayer();
        onStart(mediaPlayer, isNew);
    }

    void playQueue() {
        if (mediaQueue.isEmpty()) {
            onFinish();
            return;
        }
        ensureMediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (mediaQueue.isEmpty()) {
                    onFinish();
                } else { onStart(); }
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener(){
            @Override
            public boolean onError (MediaPlayer mp, int what, int extra) {
                prepareError("Cannot play file");
                return false;
            }
        });
        onStart();
    }
}
