package by.trezor.android.EnglishDictApp;

import android.app.Activity;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import android.view.WindowManager;
import by.trezor.android.EnglishDictApp.provider.EnglishDictDescriptor;
import com.actionbarsherlock.app.ActionBar;

import static by.trezor.android.EnglishDictApp.EnglishDictUtils.*;


public class EnglishDictDetailActivity extends EnglishDictBaseActivity {

    private static final String TAG = EnglishDictDetailActivity.class.getSimpleName();
    private static final int LOADER_ID = 1;
    private int mLangType = ENGLISH_WORDS;
    private long mId;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        Intent intent = getIntent();
        long id = intent.getLongExtra("id", -1);
        if (id == -1) {
            finish();
            return;
        }
        int langType = intent.getIntExtra("langType", ENGLISH_WORDS);
        String word = intent.getStringExtra("word");
        setCurrentLangType(langType);
        setCurrentWordId(id);
        setInputLanguage();
        setContentView(R.layout.english_dict_detail_list);
        setViewAdapter();
        restartLoader(null);
        prepareActionBar(word, langType);
        registerForContextMenu(getListView());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        com.actionbarsherlock.view.MenuInflater inflate = getSupportMenuInflater();
        inflate.inflate(getAbsMenuLayout(), menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                getAddAlertDialog();
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private int getAbsMenuLayout() {
        return getCurrentLangType() == RUSSIAN_WORDS ?
                R.menu.abs_details_menu_en:
                R.menu.abs_details_menu_ru;
    }

    private void prepareActionBar(String word, int lang) {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(word);
        actionBar.setIcon(
                lang == RUSSIAN_WORDS ? R.drawable.ic_usa: R.drawable.ic_russian);

    }

    AddWordResult<String, Long> performAddAsync(String text) {
        ContentValues values = new ContentValues();
        values.put("word", text);
        Uri uri = getContentResolver().insert(getContentUri(), values);
        Cursor c = getContentResolver().query(uri, null, null, null, null);
        c.moveToFirst();
        long wordId = c.getInt(0);
        String word = c.getString(1);
        Log.i(TAG, String.format("Added word " + word));
        c.close();
        return new AddWordResult<String, Long>(word, wordId);
    }


    void performAddActions(final String text) {
        new AddWordAsyncTask(this, text).setOnQueryTextListener(new OnExecuteListener() {
            @Override
            public void onExecute(AddWordResult<String, Long> result) {
                if (result == null) {
                    showToast("Word " + text + " do not exist.");
                }
            }
            @Override
            public AddWordResult<String, Long> onBackground(String word) {
                if (word != null && !word.isEmpty()) {
                    return performAddAsync(word);
                }
                return null;
            }
        }).execute();
    }

    @Override
    String getSoundWord(int pos) {
        if (getCurrentLangType() == ENGLISH_WORDS) {
            return super.getSoundWord(pos);
        }
        Uri uri = getEnglishContentUri();
        Cursor cursor = getContentResolver().query(
                uri, getProjection(),
                EnglishDictDescriptor.EnglishDictBaseColumns._ID + "=" + getCurrentWordId(),
                null, null);
        if (!cursor.moveToFirst()) {
            return null;
        }
        return cursor.getString(0);
    }

    private void setCurrentLangType(int type) {
        mLangType = type;
    }

    private void setCurrentWordId(long id) {
        mId = id;
    }

    private long getCurrentWordId() {
        return mId;
    }

    private Uri getContentUri(int type, long id) {
        return getSecondaryContentUri(type, id);
    }

    Uri getContentUri() {
        return getContentUri(getCurrentLangType(), getCurrentWordId());
    }

    Uri getContentUri(String search) {
        return getContentUri();
    }

    int getCurrentLangType() {
        return mLangType;
    }

    int getLoaderId() {
        return LOADER_ID;
    }

    Activity getActivity() {
        return this;
    }
}