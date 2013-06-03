package by.trezor.android.EnglishDictApp.service;

import android.app.Activity;
import android.content.Context;
import android.os.*;
import android.util.Log;
import android.view.View;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static by.trezor.android.EnglishDictApp.EnglishDictHelper.*;


public class EnglishDictGoogleVoiceHandler extends Handler {

    private Map<Integer, ViewPair> queueObj = new ConcurrentHashMap<Integer, ViewPair>();
    private Context context;

    public EnglishDictGoogleVoiceHandler(Context activity) {
        this.context = activity;
    }

    public EnglishDictGoogleVoiceHandler() {
    }

    public void setContent(Context context) {
        this.context = context;
    }

    private static class ViewPair {

        private View visible;
        private View invisible;

        private ViewPair(View visible, View invisible) {
            this.visible = visible;
            this.invisible = invisible;
        }

        private void swap() {
            visible.setVisibility(View.GONE);
            invisible.setVisibility(View.VISIBLE);
        }
    }

    public void addViewPair(int position, View visible, View invisible) {
        queueObj.put(position, new ViewPair(visible, invisible));
    }

    public void addViewPair(View visible, View invisible) {
        queueObj.put(DEFAULT_POSITION, new ViewPair(visible, invisible));
    }

    @Override
    public void handleMessage(Message message) {
        Object err = message.obj;
        int status = message.what;
        int position = message.arg1;
        final ViewPair pair = queueObj.remove(position);
        if (pair != null) pair.swap();
        if (status == Activity.RESULT_CANCELED && err != null) {
            showToast(context, err.toString());
        }
    }
}
