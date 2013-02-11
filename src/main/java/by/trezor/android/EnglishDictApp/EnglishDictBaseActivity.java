package by.trezor.android.EnglishDictApp;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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

import java.util.Locale;
import java.util.regex.Pattern;

import static by.trezor.android.EnglishDictApp.EnglishDictUtils.*;


public abstract class EnglishDictBaseActivity extends FragmentListActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = EnglishDictBaseActivity.class.getSimpleName();
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
                showProgressDialog();
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
        dismissProgressDialog();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // For whatever reason, the Loader's data is now unavailable.
        // Remove any references to the old data by replacing it with
        // a null Cursor.
        mAdapter.swapCursor(null);
        dismissProgressDialog();
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
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }
        Cursor c;
        int wordID;
        Uri uri = getContentUri();
        switch (item.getItemId()) {
            case R.id.popup_menu_delete:
                showProgressDialog();
                c = ((Cursor)(getListAdapter().getItem(info.position)));
                wordID = c.getInt(1);
                uri = ContentUris.withAppendedId(uri, wordID);
                getContentResolver().delete(uri, null, null);
                return true;
            case R.id.popup_menu_edit:
                showProgressDialog();
                c = ((Cursor)(getListAdapter().getItem(info.position)));
                wordID = c.getInt(1);
                String word = c.getString(0);
                showEditWordActivity(wordID, word);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
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

    void showProgressDialog() {
        if (mProgressBar == null) {
            mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        }
        mProgressBar.setVisibility(View.VISIBLE);
    }

    void dismissProgressDialog() {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    String getCurrentLangName() {
        return getCurrentLangType() == RUSSIAN_WORDS ? "russian": "english";
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

    void showEditWordActivity(final int wordId, String word) {
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
                        String text = editText.getText().toString();
                        if (!text.isEmpty()) {
                            //save
                            showProgressDialog();
                            ContentValues values = new ContentValues();
                            values.put("word", text);
                            Uri uri = ContentUris.withAppendedId(getContentUri(), wordId);
                            getContentResolver().update(uri, values, null, null);
                            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                        }
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {}
        });
        builder.show();
    }

    void showAddWordActivity() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(
                R.layout.english_dict_add_popup, null, false);
        final EditText editText = (EditText) dialogLayout.findViewById(R.id.add_popup_input);
        editText.setFilters(new InputFilter[]{getInputFilter()});
        builder.setView(dialogLayout)
                .setTitle("Enter a word")
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String text = editText.getText().toString();
                        if (!text.isEmpty()) {
                            //add
                            showProgressDialog();
                            performAddActions(text);
                        }
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        builder.show();
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

    abstract void performAddActions(String text);

    abstract Uri getContentUri();

    abstract Uri getContentUri(String query);

    abstract int getCurrentLangType();

    abstract int getLoaderId();

    String[] getProjection() {
        return PROJECTION;
    }
}