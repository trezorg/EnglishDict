package by.trezor.android.EnglishDictApp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.*;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.widget.*;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.inputmethod.InputMethodManager;
import by.trezor.android.EnglishDictApp.provider.EnglishDictDescriptor;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

import static by.trezor.android.EnglishDictApp.EnglishDictUtils.*;


public abstract class EnglishDictBaseActivity extends FragmentListActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = EnglishDictBaseActivity.class.getSimpleName();
    protected static final int RESULT_SPEECH = 1013;
    private AlertDialog mAddAlertDialog;
    private SimpleCursorAdapter mAdapter;
    private Toast mToast;
    private ProgressBar mProgressBar;
    private final String[] PROJECTION = new String[] {
            EnglishDictDescriptor.EnglishDictBaseColumns.WORD,
            EnglishDictDescriptor.EnglishDictBaseColumns._ID
    };

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Create a new CursorLoader with the following query parameters.
        return getLoaderCursor(id, args);
    }

    private Loader<Cursor> getLoaderCursor(int id, Bundle args) {
        String query = args == null ? null : args.getString("query");
        Uri uri = getContentUri(query);
        return new CursorLoader(this, uri, getProjection(), null, null, null) {
            @Override
            protected void onStartLoading() {
                super.onStartLoading();
                showProgressBar();
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // A switch-case is useful when dealing with multiple Loaders/IDs
        if (loader.getId() == getLoaderId()) {
            // The asynchronous load is complete and the data
            // is now available for use. Only now can we associate
            // the queried Cursor with the SimpleCursorAdapter.
            mAdapter.swapCursor(cursor);
        }
        dismissProgressBar();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // For whatever reason, the Loader's data is now unavailable.
        // Remove any references to the old data by replacing it with
        // a null Cursor.
        mAdapter.swapCursor(null);
        dismissProgressBar();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }
        Cursor c = ((Cursor)(getListAdapter().getItem(info.position)));
        String word = c.getString(0);
        MenuInflater inflate = getMenuInflater();
        inflate.inflate(R.menu.popup_menu, menu);
        menu.setHeaderTitle(word);
        menu.setHeaderIcon(android.R.drawable.ic_menu_edit);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }
        Cursor c;
        int wordID;
        final Uri uri = getContentUri();
        switch (item.getItemId()) {
            case R.id.popup_menu_delete:
                showProgressBar();
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... args) {
                        Cursor cursor = ((Cursor)(getListAdapter().getItem(info.position)));
                        int wordID = cursor.getInt(1);
                        getContentResolver().delete(
                                ContentUris.withAppendedId(uri, wordID), null, null);
                        return null;
                    }
                }.execute();
                return true;
            case R.id.popup_menu_edit:
                showProgressBar();
                c = ((Cursor)(getListAdapter().getItem(info.position)));
                wordID = c.getInt(1);
                String word = c.getString(0);
                showEditWordActivity(wordID, word);
                dismissProgressBar();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RESULT_SPEECH: {
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> text =
                            data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    createSpeachChoiceDialog(text);
                }
                break;
            }

        }
    }

    void createSpeachChoiceDialog(final ArrayList<String> text) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        CharSequence[] choice = text.toArray(new CharSequence[text.size()]);
        builder.setTitle("Choice variant").
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

    void setInputLanguage() {
        String langCode = getCurrentLangType() == ENGLISH_WORDS ? "EN" : "RU";
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        Locale locale = new Locale(langCode.toLowerCase());
        Locale.setDefault(locale);
        conf.locale = locale;
        res.updateConfiguration(conf, dm);
    }

    void showProgressBar() {
        if (mProgressBar == null) {
            mProgressBar = (ProgressBar)findViewById(R.id.progress_bar);
        }
        mProgressBar.setVisibility(View.VISIBLE);
    }

    void dismissProgressBar() {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    String getCurrentLangName() {
        return getCurrentLangType() == RUSSIAN_WORDS ? "russian": "english";
    }

    Locale getLocale() {
        return getCurrentLangType() == RUSSIAN_WORDS
                ? new Locale("ru","RU"): new Locale("en","EN");
    }

    InputFilter getInputFilter() {
        return new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start,
                                       int end, Spanned dest, int dstart, int dend) {
                final Pattern pattern = getCurrentLangType() == RUSSIAN_WORDS ?
                        RUSSIAN_LETTERS_PATTERN : ENGLISH_LETTERS_PATTERN;
                String checkedText = dest.subSequence(0, dstart).toString() +
                        source.subSequence(start, end) +
                        dest.subSequence(dend, dest.length()).toString();
                if (!pattern.matcher(checkedText).matches()) {
                    showToast("Please use only " + getCurrentLangName() + " letters");
                    return "";
                }
                return null;
            }
        };
    }

    String checkPatternLanguageText(String text) {
        final Pattern pattern = getCurrentLangType() == RUSSIAN_WORDS ?
                RUSSIAN_LETTERS_PATTERN : ENGLISH_LETTERS_PATTERN;
        if (!pattern.matcher(text).matches()) {
            showToast("Please use only " + getCurrentLangName() + " letters");
            return replaceNotExpectedPattern(text, pattern);
        }
        return text;
    }

    AlertDialog showEditWordActivity(final int wordId, final String word) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(
                R.layout.english_dict_add_popup, null, false);
        final EditText editText = (EditText) dialogLayout.findViewById(R.id.add_popup_input);
        editText.setText(word);
        editText.setFilters(new InputFilter[]{getInputFilter()});
        builder.setView(dialogLayout)
                .setTitle("Edit a word")
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        final String text = editText.getText().toString();
                        if (!text.isEmpty() && !text.equals(word)) {
                            //save
                            showProgressBar();
                            hideVirtualKeyboard(editText);
                            new AsyncTask<Void, Void, Void>() {
                                @Override
                                protected Void doInBackground(Void... args) {
                                    ContentValues values = new ContentValues();
                                    values.put("word", text);
                                    Uri uri = ContentUris.withAppendedId(getContentUri(), wordId);
                                    getContentResolver().update(uri, values, null, null);
                                    return null;
                                }
                            }.execute();
                        }
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {}
                });
        return builder.show();
    }

    AlertDialog showAddWordActivity() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(
                R.layout.english_dict_add_popup, null, false);
        final ImageButton btnSpeak = (ImageButton)
                dialogLayout.findViewById(R.id.btnSpeak);
        final EditText editText = (EditText)
                dialogLayout.findViewById(R.id.add_popup_input);
        editText.setFilters(new InputFilter[]{getInputFilter()});
        btnSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                        getLocale().toString());
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10);
                try {
                    startActivityForResult(intent, RESULT_SPEECH);
                    editText.setText("");
                } catch (ActivityNotFoundException a) {
                    showToast("Opps! Your device doesn't support Speech to Text");
                }
            }
        });
        builder.setView(dialogLayout)
                .setTitle("Enter a word")
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String text = editText.getText().toString();
                        if (!text.isEmpty()) {
                            //add
                            showProgressBar();
                            hideVirtualKeyboard(editText);
                            performAddActions(text);
                        }
                        mAddAlertDialog = null;
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mAddAlertDialog = null;
                    }
                });
        return builder.show();
    }

    private void hideVirtualKeyboard(EditText editText) {
        InputMethodManager imm = (InputMethodManager)getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    Uri getSecondaryContentUri(int type, long id) {
        Uri uri;
        switch (type) {
            case ENGLISH_WORDS:
                uri = EnglishDictDescriptor.EnglishDictDetailEnglishWords.CONTENT_URI;
                break;
            case RUSSIAN_WORDS:
                uri = EnglishDictDescriptor.EnglishDictDetailRussianWords.CONTENT_URI;
                Log.d(TAG, uri.toString());
                break;
            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
        return Uri.parse(uri + "?" +
                EnglishDictDescriptor.EnglishDictBaseColumns.QUERY_RELATION_NAME +
                "=" + id);
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
        if (search == null || search.isEmpty()) { return uri; }
        String queryString = EnglishDictDescriptor.
                EnglishDictBaseColumns.QUERY_PARAM_NAME + "=" + Uri.encode(search);
        return Uri.parse(uri + "?" + queryString);
    }

    private Bundle getSearchBundle(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        Bundle bundle = new Bundle();
        bundle.putString("query", query);
        return bundle;
    }

    void restartLoader(String query) {
        getSupportLoaderManager().restartLoader(
                getLoaderId(), getSearchBundle(query), this);
    }

    void setViewAdapter() {
        int[] viewIds = new int[] { R.id.english_dict_word };
        mAdapter = new SimpleCursorAdapter(
                this,
                R.layout.english_dict_list_item,
                null,
                getProjection(),
                viewIds,
                0
        );
        setListAdapter(mAdapter);
    }

    void showToast(String message) {
        if (mToast == null || mToast.getView().getWindowVisibility() != View.VISIBLE) {
            mToast = Toast.makeText(getApplicationContext(), message , Toast.LENGTH_SHORT);
        } else {
            mToast.setText(message);
        }
        mToast.show();
    }

    class AddWordResult<String, Long> {

        private String word;
        private Long id;

        AddWordResult(String word, Long id) {
            this.word = word;
            this.id = id;
        }

        public String getWord() {
            return word;
        }

        public Long getId() {
            return id;
        }

    }

    public interface OnExecuteListener {
        void onExecute(AddWordResult<String, Long> result);
        AddWordResult<String, Long> onBackground(String word);
    }

    class AddWordAsyncTask extends AsyncTask<Void, Void, AddWordResult<String, Long>> {

        private ProgressDialog progressDialog;
        private Context context;
        private String word;
        private OnExecuteListener mOnFinishListener;

        AddWordAsyncTask (Context context, String word) {
            super();
            this.context = context;
            this.word = word;
        }

        public AddWordAsyncTask setOnQueryTextListener(OnExecuteListener listener) {
            mOnFinishListener = listener;
            return this;
        }

        @Override
        protected void onPreExecute() {
            dismissProgressBar();
            progressDialog = new ProgressDialog(context);
            progressDialog.setTitle(word);
            progressDialog.setMessage("Processing...");
            progressDialog.show();
        }

        @Override
        protected AddWordResult<String, Long> doInBackground(Void... args) {
            if (mOnFinishListener != null) {
                return mOnFinishListener.onBackground(word);
            }
            return null;
        }

        @Override
        protected void onPostExecute(AddWordResult<String, Long> result) {
            progressDialog.dismiss();
            if (mOnFinishListener != null) {
                mOnFinishListener.onExecute(result);
            }
        }
    }

    AlertDialog getAddAlertDialog() {
        if (mAddAlertDialog == null) {
            mAddAlertDialog = showAddWordActivity();
        } else {
            mAddAlertDialog.show();
        }
        return mAddAlertDialog;
    }

    abstract AddWordResult<String, Long> performAddAsync(String text);

    abstract void performAddActions(String text);

    abstract Uri getContentUri();

    abstract Uri getContentUri(String query);

    abstract int getCurrentLangType();

    abstract int getLoaderId();

    String[] getProjection() {
        return PROJECTION;
    }
}