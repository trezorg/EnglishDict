package by.trezor.android.EnglishDictApp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.*;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import by.trezor.android.EnglishDictApp.provider.EnglishDictDescriptor;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockListFragment;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

import static by.trezor.android.EnglishDictApp.EnglishDictUtils.*;
import static by.trezor.android.EnglishDictApp.AddWordAsyncTask.*;


class EnglishDictDetailFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = EnglishDictDetailFragment.class.getSimpleName();
    protected static final int RESULT_SPEECH = 1013;
    static final int RESULT_OK = -1;
    private AlertDialog mAddAlertDialog;
    private Toast mToast;
    private SimpleCursorAdapter mAdapter;
    private int mLangType = ENGLISH_WORDS;
    private int loaderId;
    private long mId;
    private ProgressBar mProgressBar;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.english_dict_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setLoaderAdapter();
        registerForContextMenu(getListView());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mId = getArguments().getLong(WORD_ID);
        mLangType = getArguments().getInt(LANG_TYPE, ENGLISH_WORDS);
        loaderId = getArguments().getInt(WORD_POSITION);
    }

    public void setLoaderAdapter() {
        setViewAdapter();
        initLoader();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Create a new CursorLoader with the following query parameters.
        return getLoaderCursor();
    }

    private Loader<Cursor> getLoaderCursor() {
        return new CursorLoader(getSherlockActivity(), getContentUri(), getProjection(), null, null, null) {
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
            mAdapter.changeCursor(cursor);
            dismissProgressBar();
        }
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
        Cursor cursor = ((Cursor)mAdapter.getItem(info.position));
        String word = cursor.getString(0);
        menu.add(loaderId, R.id.popup_menu_edit, 0, R.string.menu_edit);
        menu.add(loaderId, R.id.popup_menu_delete, 0, R.string.menu_delete);
        menu.setHeaderTitle(word);
        menu.setHeaderIcon(android.R.drawable.ic_menu_edit);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getGroupId() != loaderId) {
            return super.onContextItemSelected(item);
        }
        final AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }
        Cursor cursor;
        int wordID;
        final Uri uri = getContentUri();
        final Activity mActivity = getSherlockActivity();
        switch (item.getItemId()) {
            case R.id.popup_menu_delete:
                showProgressBar();
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... args) {
                        Cursor cursor = (Cursor)mAdapter.getItem(info.position);
                        int wordID = cursor.getInt(1);
                        mActivity.getContentResolver().delete(
                                ContentUris.withAppendedId(uri, wordID), null, null);
                        return null;
                    }
                }.execute();
                return true;
            case R.id.popup_menu_edit:
                showProgressBar();
                cursor = (Cursor)getListAdapter().getItem(info.position);
                wordID = cursor.getInt(1);
                String word = cursor.getString(0);
                showEditWordActivity(wordID, word);
                dismissProgressBar();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        String word = ((Cursor)(mAdapter.getItem(position))).getString(0);
        Cursor cursor = getSherlockActivity().getContentResolver().query(
                getMainContentUri(getCurrentLangType(), null), null, null, null, null);
        int pos = getCursorPositionForId(cursor, id);
        showSecondaryActivity(word, id, pos);
    }

    void showSecondaryActivity(String word, long  id, int position) {
        Bundle bundle = new Bundle();
        bundle.putLong(WORD_ID, id);
        bundle.putInt(LANG_TYPE, getCurrentLangType() == ENGLISH_WORDS ? RUSSIAN_WORDS : ENGLISH_WORDS);
        bundle.putString(WORD, word);
        bundle.putInt(WORD_POSITION, position);
        Intent intent = new Intent(getSherlockActivity(), EnglishDictDetailActivity.class);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    void createSpeechChoiceDialog(final ArrayList<String> text) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getSherlockActivity());
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

    void showProgressBar() {
        if (mProgressBar == null) {
            mProgressBar = (ProgressBar)getView().findViewById(R.id.progress_bar);
        }
        mProgressBar.setVisibility(View.VISIBLE);
    }

    void dismissProgressBar() {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    String getCurrentLangNames() {
        return getCurrentLangType() == RUSSIAN_WORDS ?
                getString(R.string.russian_name_multiple):
                getString(R.string.english_name_multiple);
    }

    String getIETFLocale() {
        return (getCurrentLangType() == RUSSIAN_WORDS
                ? new Locale("ru","RU"): new Locale("en","US")
        ).toString().replace("_", "-");
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
                    showToast(String.format(getString(
                            R.string.wrong_letters_language_text), getCurrentLangNames()));
                    return "";
                }
                return null;
            }
        };
    }

    AlertDialog showEditWordActivity(final int wordId, final String word) {
        final SherlockFragmentActivity activity = getSherlockActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogLayout = inflater.inflate(
                R.layout.english_dict_add_popup, null, false);
        final EditText editText = (EditText) dialogLayout.findViewById(R.id.add_popup_input);
        editText.setText(word);
        editText.setFilters(new InputFilter[]{getInputFilter()});
        builder.setView(dialogLayout)
                .setTitle(R.string.edit_word_text)
                .setPositiveButton(R.string.save_text, new DialogInterface.OnClickListener() {
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
                                    activity.getContentResolver().update(uri, values, null, null);
                                    return null;
                                }
                            }.execute();
                        }
                    }
                }).setNegativeButton(R.string.cancel_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {}
                });
        return builder.show();
    }

    AlertDialog showAddWordActivity() {
        final SherlockFragmentActivity activity = getSherlockActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
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
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, getIETFLocale());
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10);
                try {
                    startActivityForResult(intent, RESULT_SPEECH);
                    editText.setText("");
                } catch (ActivityNotFoundException a) {
                    showToast(getString(R.string.speech_support));
                }
            }
        });
        builder.setView(dialogLayout)
                .setTitle(R.string.enter_word_text)
                .setPositiveButton(R.string.add_text, new DialogInterface.OnClickListener() {
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
                }).setNegativeButton(R.string.cancel_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mAddAlertDialog = null;
                    }
                });
        return builder.show();
    }

    private void hideVirtualKeyboard(EditText editText) {
        InputMethodManager imm = (InputMethodManager)getSherlockActivity().getSystemService(
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

    void initLoader() {
        getSherlockActivity().getSupportLoaderManager().initLoader(getLoaderId(), null, this);
    }

    private int getListItem() {
        return getCurrentLangType() == RUSSIAN_WORDS ?
                R.layout.english_dict_list_item_ru :
                R.layout.english_dict_list_item_en;
    }

    void setViewAdapter() {
        int[] viewIds = new int[] { R.id.english_dict_word };
        mAdapter = new SimpleCursorAdapter(
                getSherlockActivity(),
                getListItem(),
                null,
                getProjection(),
                viewIds,
                0
        );
        setListAdapter(mAdapter);
    }

    void showToast(String message) {
        if (mToast == null || mToast.getView().getWindowVisibility() != View.VISIBLE) {
            mToast = Toast.makeText(getSherlockActivity(), message , Toast.LENGTH_SHORT);
        } else {
            mToast.setText(message);
        }
        mToast.show();
    }

    AddWordResult<String, Long, Integer> performAddAsync(String text) {
        ContentValues values = new ContentValues();
        SherlockFragmentActivity activity = getSherlockActivity();
        values.put("word", text);
        Uri uri = activity.getContentResolver().insert(getContentUri(), values);
        Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        long wordId = cursor.getInt(0);
        String word = cursor.getString(1);
        cursor = activity.getContentResolver().query(getContentUri(), null, null, null, null);
        int position = getCursorPositionForId(cursor, wordId);
        cursor.close();
        Log.i(TAG, String.format("Added word " + word));
        return new AddWordResult<String, Long, Integer>(word, wordId, position);
    }


    void performAddActions(final String text) {
        final ProgressDialog progressDialog = new ProgressDialog(getSherlockActivity());
        progressDialog.setTitle(text);
        progressDialog.setMessage(getString(R.string.processing_translate_text));
        new AddWordAsyncTask(text).setOnQueryTextListener(new AddWordAsyncTask.OnExecuteListener() {
            @Override
            public void onExecute(AddWordAsyncTask.AddWordResult<String, Long, Integer> result) {
                progressDialog.dismiss();
                if (result == null) {
                    showToast(String.format("Word %s do not exist.", text));
                }
            }
            @Override
            public AddWordAsyncTask.AddWordResult<String, Long, Integer> onBackground(String word) {
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

    AlertDialog getAddAlertDialog() {
        if (mAddAlertDialog == null) {
            mAddAlertDialog = showAddWordActivity();
        } else {
            mAddAlertDialog.show();
        }
        return mAddAlertDialog;
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

    int getCurrentLangType() {
        return mLangType;
    }

    int getLoaderId() {
        return loaderId;
    }

    String[] getProjection() {
        return PROJECTION;
    }
}