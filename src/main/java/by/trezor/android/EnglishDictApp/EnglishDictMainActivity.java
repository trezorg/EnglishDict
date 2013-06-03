package by.trezor.android.EnglishDictApp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.speech.RecognizerIntent;
import android.support.v4.app.FragmentTransaction;
import android.view.*;
import android.widget.*;
import by.trezor.android.EnglishDictApp.provider.EnglishDictDescriptor;
import by.trezor.android.EnglishDictApp.service.EnglishDictDict4jService.EnglishDictDict4jServiceResult;
import by.trezor.android.EnglishDictApp.service.EnglishDictDict4jService;
import by.trezor.android.EnglishDictApp.service.EnglishDictGoogleVoiceHandler;
import by.trezor.android.EnglishDictApp.service.EnglishDictGoogleVoiceService;
import by.trezor.android.EnglishDictApp.training.EnglishDictTrainingChoiceActivity;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.widget.SearchView;

import static by.trezor.android.EnglishDictApp.EnglishDictHelper.*;
import by.trezor.android.EnglishDictApp.provider.EnglishDictDescriptor.EnglishDictBaseColumns.*;

import java.util.*;

public class EnglishDictMainActivity extends EnglishDictBaseActivity implements ActionBar.TabListener {

    private static final String LIST_STATE = "listState";
    private int mLangType = ENGLISH_WORDS;
    private SORT_ORDER mOrder = SORT_ORDER.WORD;
    private MenuItem mSearchMenuItem;
    private boolean isSearch;
    private static final int LOADER_ID = 0;
    private Parcelable mListState = null;
    private EnglishDictGoogleVoiceHandler voiceHandler =
            new EnglishDictGoogleVoiceHandler(this);
    private Handler translateHandler = new Handler() {
        @Override
        @SuppressWarnings("unchecked")
        public void handleMessage(Message message) {
            Object obj = message.obj;
            int status = message.what;
            dismissProgressBar();
            if (status == Activity.RESULT_CANCELED && obj != null) {
                showToast(getApplicationContext(), obj.toString());
                return;
            }
            if (obj == null) return;
            final EnglishDictDict4jServiceResult sr = (EnglishDictDict4jServiceResult)obj;
            if (sr.getCs().length == 0) {
                showToast(getApplicationContext(),
                        getString(R.string.translate_no_found_words));
                return;
            }
            createTranslateChoiceDialog(sr.getWord(), sr.getLang(),
                    sr.getCs(), sr.getChecked());
        }
    };

    @Override
    public void onCreate(Bundle state) {
        setCurrentLangType(getLangType(state));
        super.onCreate(state);
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
        state.putInt(LANG_TYPE, getCurrentLangType());
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        // Save instance-specific state
        mListState = state.getParcelable(LIST_STATE);
        setCurrentLangType(state.getInt(LANG_TYPE, ENGLISH_WORDS));
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
        int menuId = langType == ENGLISH_WORDS ? R.id.menu_en: R.id.menu_ru;
        int orderIcon = mOrder.isReverse() ?
                android.R.drawable.arrow_up_float :
                android.R.drawable.arrow_down_float;
        if (mOrder.isWordOrdering()) {
            menu.findItem(R.id.order_word).setIcon(orderIcon);
            menu.findItem(R.id.order_rating).setIcon(null);
        } else {
            menu.findItem(R.id.order_word).setIcon(null);
            menu.findItem(R.id.order_rating).setIcon(orderIcon);
        }
        menu.findItem(menuId).setChecked(true);
        menu.findItem(menuId).setEnabled(false);
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
                isSearch = false;
                invalidateOptionsMenu();
                return true;
            case R.id.menu_add:
                getAddAlertDialog();
                return true;
            case R.id.order_word:
                saveCurrentOrdering(mOrder.isWordOrdering() ?
                        (mOrder.isReverse() ? SORT_ORDER.WORD : SORT_ORDER.WORD_REVERSE) :
                        SORT_ORDER.WORD);
                return true;
            case R.id.order_rating:
                saveCurrentOrdering(mOrder.isRatingOrdering() ?
                        (mOrder.isReverse() ? SORT_ORDER.RATING : SORT_ORDER.RATING_REVERSE) :
                        SORT_ORDER.RATING);
                return true;
            case R.id.menu_settings:
                showPreferences(this);
                return true;
            case R.id.menu_training:
                showTrainingActivity();
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
        menu.findItem(R.id.menu_sort).setVisible(false);
    }

    private void showActionBarIcons(Menu menu) {
        menu.findItem(R.id.menu_reload).setVisible(isSearch);
        menu.findItem(R.id.menu_lang).setVisible(true);
        menu.findItem(R.id.menu_sort).setVisible(true);
    }

    private void handleIntent(Intent intent) {
        String query = null;
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = intent.getStringExtra(SearchManager.QUERY);
            isSearch = true;
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
        Intent intent = new Intent(this, EnglishDictDict4jService.class);
        intent.putExtra(PARAM_MESSAGER, new Messenger(translateHandler));
        intent.putExtra(PARAM_WORD, text);
        intent.putExtra(LANG_TYPE, getCurrentLangType());
        intent.putExtra(PARAM_NETWORK_AVAILABLE, isNetworkAvailable(getActivity()));
        startService(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RESULT_SPEECH: {
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> text =
                            data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    createSpeechChoiceDialog(text);
                }
                break;
            }
        }
    }

    void createTranslateChoiceDialog(final String word,
                                     final int lang,
                                     final CharSequence[] words,
                                     final boolean[] selectedItems) {
        final boolean[] initialSelectedItems = new boolean[selectedItems.length+1];
        System.arraycopy(selectedItems, 0, initialSelectedItems, 1, selectedItems.length);
        final boolean[] changedSelectedItems = new boolean[selectedItems.length+1];
        System.arraycopy(selectedItems, 0, changedSelectedItems, 1, selectedItems.length);
        final CharSequence[] dialogWords = new CharSequence[words.length+1];
        System.arraycopy(words, 0, dialogWords, 1, words.length);
        // add main category
        if (areAllTrue(selectedItems)) {
            initialSelectedItems[0] = true;
            changedSelectedItems[0] = true;
        }
        dialogWords[0] = getString(R.string.translate_chose_all_words);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.translate_select_words);
        builder.setMultiChoiceItems(
                dialogWords,
                changedSelectedItems,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        AlertDialog d = (AlertDialog) dialog;
                        ListView v = d.getListView();
                        changedSelectedItems[which] = isChecked;
                        if (which == 0) {
                            int i = 1;
                            while(i < dialogWords.length) {
                                v.setItemChecked(i, isChecked);
                                changedSelectedItems[i] = isChecked;
                                i++;
                            }
                        } else {
                            if (!isChecked || areAllTrue(Arrays.copyOfRange(
                                    changedSelectedItems, 1, changedSelectedItems.length))) {
                                changedSelectedItems[0] = isChecked;
                                v.setItemChecked(0, isChecked);
                            }
                        }
                    }
                }).setPositiveButton(R.string.save_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {}
                }).setNegativeButton(R.string.cancel_text, null);

        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // get added and deleted
                        final List<String> deleted = new ArrayList<String>();
                        final List<String> added = new ArrayList<String>();
                        for (int i = 1; i < changedSelectedItems.length; i++) {
                            if (initialSelectedItems[i] && !changedSelectedItems[i]) deleted.add(dialogWords[i].toString());
                            else if (!initialSelectedItems[i] && changedSelectedItems[i]) added.add(dialogWords[i].toString());
                        }
                        if (deleted.isEmpty() && added.isEmpty()) {
                            showToast(getApplicationContext(), getString(R.string.translate_no_chosen_words));
                            return;
                        }
                        new AsyncTask<Void, Void, long[]>() {
                            @Override
                            protected void onPreExecute() {
                                alert.dismiss();
                                showProgressBar();
                            }
                            @Override
                            protected long[] doInBackground(Void... args) {
                                long wordId = processTranslatedWords(word, lang, added, deleted);
                                // get cursor position for ID
                                Cursor cursor = getContentResolver().query(getContentUri(), null, null, null, null);
                                long position = getCursorPositionForId(cursor, wordId);
                                cursor.close();
                                return new long[] {wordId, position};
                            }
                            @Override
                            protected void onPostExecute(long[] result) {
                                showSecondaryActivity(word, result[0], (int)result[1]);
                            }
                        }.execute();
                    }
                });
            }
        });
        alert.show();
    }

    private long addMainTranslatedWord(final String word, final int lang) {
        ContentValues values = new ContentValues();
        values.put("word", word);
        Uri uri = getContentResolver().insert(getContentUri(lang, null), values);
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        long wordId = cursor.getLong(0);
        cursor.close();
        return wordId;
    }

    private long processTranslatedWords(final String word, final int lang,
                                        List<String> added, List<String> deleted) {
        long wordId = addMainTranslatedWord(word, lang);
        if (!added.isEmpty()) addTranslateWords(added, wordId, lang);
        if (!deleted.isEmpty()) deleteTranslateWords(deleted, lang);
        return wordId;
    }

    private void deleteTranslateWords(List<String> words, int lang) {
        Uri uri = getContentUri(lang == ENGLISH_WORDS ? RUSSIAN_WORDS: ENGLISH_WORDS, null);
        for (String word: words) {
            getContentResolver().delete(uri, "word=?", new String[] {word});
        }
    }

    private void addTranslateWords(List<String> words, long wordId, int lang) {
        Uri uri = getSecondaryContentUri(
                lang == ENGLISH_WORDS ? RUSSIAN_WORDS : ENGLISH_WORDS, wordId);
        for (String w: words) {
            ContentValues values = new ContentValues();
            values.put("word", w.toLowerCase());
            getContentResolver().insert(uri, values);
        }
    }

    private void createSpeechChoiceDialog(final ArrayList<String> text) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        CharSequence[] choice = text.toArray(new CharSequence[text.size()]);
        builder.setTitle(R.string.choice_variant).
                setSingleChoiceItems(choice, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String word = text.get(which);
                        AlertDialog alertDialog = getAddAlertDialog();
                        EditText editText = (EditText)
                                alertDialog.findViewById(R.id.add_popup_input);
                        editText.setText(word);
                        dialog.dismiss();
                    }
                });
        builder.show();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        String word = ((Cursor)(getListAdapter().getItem(position))).getString(0);
        Cursor cursor = getContentResolver().query(getContentUri(), null, null, null, null);
        int pos = getCursorPositionForId(cursor, id);
        showSecondaryActivity(word, id, pos);
    }

    void showSecondaryActivity(String word, long id, int position) {
        Bundle bundle = new Bundle();
        bundle.putLong(WORD_ID, id);
        bundle.putInt(LANG_TYPE, getCurrentLangType() == ENGLISH_WORDS ? RUSSIAN_WORDS : ENGLISH_WORDS);
        bundle.putString(WORD, word);
        bundle.putInt(WORD_POSITION, position);
        bundle.putString(ORDERING, mOrder.name());
        Intent intent = new Intent(this, EnglishDictDetailActivity.class);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        dismissProgressBar();
        startActivity(intent);
    }

    void showTrainingActivity() {
        Intent intent = new Intent(this, EnglishDictTrainingChoiceActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    Uri getMainContentUri(int type, String search) {
        Uri uri;
        switch (type) {
            case ENGLISH_WORDS:
                uri = EnglishDictDescriptor.EnglishDictEnglishWords.CONTENT_URI;
                break;
            case RUSSIAN_WORDS:
                uri = EnglishDictDescriptor.EnglishDictRussianWords.CONTENT_URI;
                break;
            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
        uri = Uri.parse(uri + "?" + EnglishDictDescriptor.EnglishDictBaseColumns.QUERY_PARAM_ORDER_BY
                + "=" + Uri.encode(mOrder.toString()));
        if (search == null || search.isEmpty()) { return uri; }
        String queryString = EnglishDictDescriptor.
                EnglishDictBaseColumns.QUERY_PARAM_NAME + "=" + Uri.encode(search);
        return Uri.parse(uri + "&" + queryString);
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

    private void saveCurrentOrdering(SORT_ORDER order) {
        mOrder = order;
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
            type = state.getInt(LANG_TYPE, ENGLISH_WORDS);
        }
        return type == RUSSIAN_WORDS ? RUSSIAN_WORDS : ENGLISH_WORDS;
    }

    private int getViewPosition(View view) {
        try {
            return getListView().getPositionForView((LinearLayout)view.getParent());
        } catch (Exception ex) {
            return -1;
        }
    }

    private String getSoundWord(int pos) {
        return ((Cursor)(getListAdapter().getItem(pos))).getString(0);
    }

    public final void playSound(final View view) {
        final LinearLayout parent = (LinearLayout) view.getParent();
        final ProgressBar progressBar =
                (ProgressBar) parent.findViewById(R.id.progress_bar_sound);
        progressBar.setVisibility(View.VISIBLE);
        view.setVisibility(View.GONE);
        final int position = getViewPosition(view);
        final String word = getSoundWord(position);
        if (word ==  null || word.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            view.setVisibility(View.VISIBLE);
            return;
        }
        voiceHandler.addViewPair(position, progressBar, view);
        Intent intent = new Intent(this, EnglishDictGoogleVoiceService.class);
        intent.putExtra(PARAM_MESSAGER, new Messenger(voiceHandler));
        intent.putExtra(PARAM_POSITION, position);
        intent.putExtra(PARAM_WORD, word);
        intent.putExtra(PARAM_NETWORK_AVAILABLE, isNetworkAvailable(getActivity()));
        startService(intent);
    }

}