package by.trezor.android.EnglishDictApp;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static by.trezor.android.EnglishDictApp.EnglishDictHelper.*;


public class EnglishDictGoogleVoice {

    private static final String TAG = EnglishDictGoogleVoice.class.getSimpleName();
    private static final String URL_ENGLISH_VOICE =
            "http://ssl.gstatic.com/dictionary/static/sounds/de/0/%s.mp3";
    private static volatile EnglishDictGoogleVoice instance;
    private static final String DICTIONARY_FILES_DIRECTORY = "dictMp3";
    private MediaPlayer mediaPlayer;
    private Queue<String> mediaQueue = new ConcurrentLinkedQueue<String>();
    private Queue<String> wordsQueue = new ConcurrentLinkedQueue<String>();
    private Queue<OnExecuteListener> onExecuteListener =
            new ConcurrentLinkedQueue<OnExecuteListener>();
    private OnErrorListener onErrorListener;
    private Context context;
    private boolean isNetworkAvailable;

    private EnglishDictGoogleVoice() {}

    public interface OnExecuteListener {
        void onExecute();
    }

    public interface OnErrorListener {
        void onError(String message);
    }

    public void addOnExecuteListener(OnExecuteListener listener) {
        onExecuteListener.add(listener);
    }

    public void setOnErrorListener(OnErrorListener listener) {
        onErrorListener = listener;
    }

    public OnErrorListener getOnErrorListener() {
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
        if (localPath == null && isNetworkAvailable) {
            String url = getUrlEnglishVoiceUrl(word);
            localPath = downloadFile(url, word, DICTIONARY_FILES_DIRECTORY);
        }
        return localPath;
    }

    public void prepareVoiceFile(String word) throws IOException {
        String localPath = checkFile(word, DICTIONARY_FILES_DIRECTORY);
        if (localPath == null && context != null && isNetworkAvailable(context)) {
            String url = getUrlEnglishVoiceUrl(word);
            downloadFile(url, word, DICTIONARY_FILES_DIRECTORY);
        }
    }

    public void prepareVoiceFiles(String[] words) throws IOException {
        for (String word: words) {
            prepareVoiceFile(word);
        }
    }

    public void setContext(Context context) {
        if (this.context == null) {
            this.context = context;
        }
    }

    void prepareFile(String word) {
        if (word == null) {
            return;
        }
        try {
            String filePath = getFilePath(word);
            if (filePath != null) {
                mediaQueue.add(filePath);
            }
        } catch (IOException ex) {
            prepareError("Cannot download file: " + getUrlEnglishVoiceUrl(word), ex);
        }
    }

    public void play(String[] words) {
        isNetworkAvailable = isNetworkAvailable(context);
        // add words to wordsQueue
        wordsQueue.addAll(Arrays.asList(words));
        if (!isPlaying()) {
            playQueue();
        }
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

    synchronized private void ensureMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            return;
        }
        try {
            mediaPlayer.reset();
        } catch (IllegalStateException ignored) {}
    }

    public synchronized void finish() {
        if (mediaPlayer != null) {
            try {
                if (isPlaying()) {
                    mediaPlayer.stop();
                }
                if (mediaPlayer != null) {
                    mediaPlayer.release();
                }
            } catch (IllegalAccessError ex) {
                Log.d(TAG, "The error has been occurred during mediaPlayer finishing", ex);
            } finally {
                mediaPlayer = null;
            }
        }
        mediaQueue.clear();
        wordsQueue.clear();
        onExecute();
    }

    private void start() {
        ensureMediaPlayer();
        String path = mediaQueue.poll();
        if (path == null) {
            return;
        }
        try {
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception ex) {
            prepareError("Cannot play file: " + path, ex);
            onExecute();
        }
    }

    private boolean isPlaying() {
        try {
            return mediaPlayer != null && mediaPlayer.isPlaying();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private void playNextWord() {
        prepareFile(wordsQueue.poll());
        if (!mediaQueue.isEmpty()) {
            start();
        } else {
            while (!wordsQueue.isEmpty()) {
                prepareFile(wordsQueue.poll());
                if (!mediaQueue.isEmpty()) {
                    break;
                }
            }
            if (!mediaQueue.isEmpty()) {
                start();
            } else {
                finish();
            }
        }
    }

    void playQueue() {
        if (wordsQueue.isEmpty()) {
            finish();
            return;
        }
        ensureMediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (wordsQueue.isEmpty()) {
                    finish();
                } else {
                    playNextWord();
                }
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener(){
            @Override
            public boolean onError (MediaPlayer mp, int what, int extra) {
                prepareError("Cannot play file");
                mediaPlayer.reset();
                return false;
            }
        });
        playNextWord();
    }
}
