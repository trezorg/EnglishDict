package by.trezor.android.EnglishDictApp.service;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import by.trezor.android.EnglishDictApp.EnglishDictGoogleVoice;

import static by.trezor.android.EnglishDictApp.EnglishDictHelper.*;

public class EnglishDictGoogleVoiceService extends Service {

    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendMessage(Messenger messenger, int status, String message) {
        Message msg = Message.obtain();
        msg.what = status;
        msg.obj = message;
        try {
            messenger.send(msg);
        } catch (android.os.RemoteException e1) {
            Log.w(getClass().getName(), "Exception sending message", e1);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        final String word = intent.getStringExtra(PARAM_WORD);
        final Boolean isNetworkAvailable =
                intent.getBooleanExtra(PARAM_NETWORK_AVAILABLE, false);
        final Bundle extras = intent.getExtras();
        final Messenger messenger = (Messenger) extras.get(PARAM_MESSAGER);
        final EnglishDictGoogleVoice voice = EnglishDictGoogleVoice.getInstance();
        voice.setIsNetworkAvailable(isNetworkAvailable);
        voice.addOnExecuteListener(new EnglishDictGoogleVoice.OnExecuteListener() {
            @Override
            public void onExecute() {
                sendMessage(messenger, Activity.RESULT_OK, null);
                stopSelf(startId);
            }
        });
        voice.setOnErrorListener(new EnglishDictGoogleVoice.OnErrorListener() {
            @Override
            public void onError(final String message) {
                sendMessage(messenger, Activity.RESULT_CANCELED, message);
                stopSelf(startId);
            }
        });
        voice.play(word.split("\\s+"));
        return super.onStartCommand(intent, flags, startId);
    }
}