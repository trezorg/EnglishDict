package by.trezor.android.EnglishDictApp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.*;
import android.widget.ListView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.widget.SearchView;

import static by.trezor.android.EnglishDictApp.EnglishDictUtils.*;
import static by.trezor.android.EnglishDictApp.AddWordAsyncTask.*;

public class EnglishDictActivity extends EnglishDictBaseActivity implements ActionBar.TabListener {

    private static final String TAG = EnglishDictActivity.class.getSimpleName();
    private static final String LIST_STATE = "listState";
    private static final String LIST_LANG = "listLang";
    private int mLangType = ENGLISH_WORDS;
    private MenuItem mSearchMenuItem;
    private static final int LOADER_ID = 0;
    private Parcelable mListState = null;

    @Override
    public void onCreate(Bundle state) {
        setCurrentLangType(getLangType(state));
        super.onCreate(state);
        setInputLanguage();
        setContentView(R.layout.english_dict_list);
        setViewAdapter();
        handleIntent(getIntent());
        setTabs();
        registerForContextMenu(getListView());
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        // Save instance-specific state
        mListState = getListView().onSaveInstanceState();
        state.putParcelable(LIST_STATE, mListState);
        state.putInt(LIST_LANG, getCurrentLangType());
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        // Save instance-specific state
        mListState = state.getParcelable(LIST_STATE);
        setCurrentLangType(state.getInt(LIST_LANG, ENGLISH_WORDS));
        super.onRestoreInstanceState(state);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mListState != null) {
            getListView().onRestoreInstanceState(mListState);
        }
        mListState = null;
        dismissProgressBar();
        collapseSearchView();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflate = getSupportMenuInflater();
        inflate.inflate(R.menu.abs_main_menu, menu);
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final MenuItem searchMenuItem = menu.findItem(R.id.menu_search);
        final SearchView searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                // Do something when collapsed
                // Return true to collapse action view
                showActionBarIcons(menu);
                return true;
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                hideActionBarIcons(menu);
                return true;
            }
        });
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean queryTextFocused) {
                if(!queryTextFocused) {
                    searchMenuItem.collapseActionView();
                    searchView.setQuery("", false);
                }
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener () {
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText == null || newText.isEmpty()) {
                    return false;
                }
                String text = checkPatternLanguageText(newText);
                if (!text.equals(newText)) {
                    // change visible text only if different
                    searchView.setQuery(text, false);
                } else {
                    // make new loader cursor
                    restartLoader(text);
                }
                return false;
            }
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
        });
        mSearchMenuItem = searchMenuItem;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        int langType = getCurrentLangType();
        MenuItem enMenuItem = menu.findItem(R.id.menu_en);
        MenuItem ruMenuItem = menu.findItem(R.id.menu_ru);
        enMenuItem.setEnabled(langType != ENGLISH_WORDS);
        ruMenuItem.setEnabled(langType != RUSSIAN_WORDS);
        (langType == RUSSIAN_WORDS ? ruMenuItem : enMenuItem).setChecked(true);
        prepareActionBar();
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_en:
                if (getCurrentLangType() != ENGLISH_WORDS) {
                    saveCurrentLangType(ENGLISH_WORDS);
                    collapseSearchView();
                }
                return true;
            case R.id.menu_ru:
                if (getCurrentLangType() != RUSSIAN_WORDS) {
                    saveCurrentLangType(RUSSIAN_WORDS);
                    collapseSearchView();
                }
                return true;
            case R.id.menu_reload:
                setAdapterCursor();
                collapseSearchView();
                return true;
            case R.id.menu_add:
                getAddAlertDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction transaction) {
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction transaction) {
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction transaction) {
    }

    private void hideActionBarIcons(Menu menu) {
        menu.findItem(R.id.menu_reload).setVisible(false);
        menu.findItem(R.id.menu_lang).setVisible(false);
    }

    private void showActionBarIcons(Menu menu) {
        menu.findItem(R.id.menu_reload).setVisible(true);
        menu.findItem(R.id.menu_lang).setVisible(true);
    }

    private void handleIntent(Intent intent) {
        String query = null;
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = intent.getStringExtra(SearchManager.QUERY);
            collapseSearchView();
        }
        restartLoader(query);
    }

    private void collapseSearchView() {
        if (mSearchMenuItem != null) {
            mSearchMenuItem.collapseActionView();
        }
    }

    void setTabs() {
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        ActionBar.Tab dictTab = getSupportActionBar().newTab();
        dictTab.setText(R.string.dict_text);
        dictTab.setTabListener(this);
        getSupportActionBar().addTab(dictTab);
        ActionBar.Tab trainingTab = getSupportActionBar().newTab();
        trainingTab.setText(R.string.training_text);
        trainingTab.setTabListener(this);
        getSupportActionBar().addTab(trainingTab);
    }

    void performAddActions(final String text) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(text);
        progressDialog.setMessage(getString(R.string.processing_translate_text));
        new AddWordAsyncTask(text).setOnQueryTextListener(new OnExecuteListener() {
            @Override
            public void onExecute(AddWordResult<String, Long, Integer> result) {
                progressDialog.dismiss();
                if (result == null) {
                    showToast(getActivity(), String.format("Word %s do not exist.", text));
                    return;
                }
                showSecondaryActivity(result.getWord(), result.getId(), result.getPosition());
            }
            @Override
            public AddWordResult<String, Long, Integer> onBackground(String word) {
                if (word == null || word.isEmpty()) {
                    return null;
                }
                return performAddAsync(word);
            }
            @Override
            public void onPreExecute() {
                dismissProgressBar();
                progressDialog.show();
            }
        }).execute();
    }

    AddWordResult<String, Long, Integer> performAddAsync(String text) {
        String trans = translate(this, text, getCurrentLangType());
        Log.i(TAG, String.format("Translated: %s -> %s", text, trans));
        if (trans == null) { return null; }
        trans = trans.toLowerCase();
        if (trans == null || trans.isEmpty() || trans.equals(text)) { return null; }
        ContentValues values = new ContentValues();
        values.put("word", text);
        Uri uri = getContentResolver().insert(getContentUri(), values);
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        long wordId = cursor.getLong(0);
        String word = cursor.getString(1);
        cursor.close();
        // get cursor position for ID
        cursor = getContentResolver().query(getContentUri(), null, null, null, null);
        int position = getCursorPositionForId(cursor, wordId);
        cursor.close();
        Log.i(TAG, String.format("Added word " + word));
        Uri secondaryUri = getSecondaryContentUri(
            getCurrentLangType() == ENGLISH_WORDS ?
                    RUSSIAN_WORDS : ENGLISH_WORDS, wordId);
        values = new ContentValues();
        values.put("word", trans);
        getContentResolver().insert(secondaryUri, values);
        Log.i(TAG, String.format("Added word " + trans));
        return new AddWordResult<String, Long, Integer>(word, wordId, position);
    }

    private Uri getContentUri(int type, String search) {
        return getMainContentUri(type, search);
    }

    Uri getContentUri() {
        return getContentUri(getCurrentLangType(), null);
    }

    Uri getContentUri(String search) {
        return getContentUri(getCurrentLangType(), search);
    }

    private void setAdapterCursor() {
        restartLoader(null);
        getListView().setSelection(0);
        invalidateOptionsMenu();
    }

    private void setCurrentLangType(int type) {
        mLangType = type;
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

    private void saveCurrentLangType(int type) {
        type = type == RUSSIAN_WORDS ? RUSSIAN_WORDS: ENGLISH_WORDS;
        setCurrentLangType(type);
        setViewAdapter();
        setAdapterCursor();
    }

    private int getLangIcon() {
        return getCurrentLangType() == RUSSIAN_WORDS ?
                R.drawable.ic_russian: R.drawable.ic_usa;
    }

    private void prepareActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setIcon(getLangIcon());
    }

    private int getLangType(Bundle state) {
        int type = ENGLISH_WORDS;
        if (state != null) {
            type = state.getInt(LIST_LANG, ENGLISH_WORDS);
        }
        return type == RUSSIAN_WORDS ? RUSSIAN_WORDS : ENGLISH_WORDS;
    }
}