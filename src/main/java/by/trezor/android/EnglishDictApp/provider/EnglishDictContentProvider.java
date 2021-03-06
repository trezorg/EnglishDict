package by.trezor.android.EnglishDictApp.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.util.*;

import static by.trezor.android.EnglishDictApp.EnglishDictHelper.join;

/**
 * Simple content provider that demonstrates the basics of creating a content
 * provider that stores words.
 */
public class EnglishDictContentProvider extends ContentProvider {

    private static final int ENGLISH_WORDS = 1;
    private static final int ENGLISH_WORD_ID = 2;
    private static final int RUSSIAN_WORDS = 3;
    private static final int RUSSIAN_WORD_ID = 4;
    private static final int ENGLISH_DETAIL_WORDS = 5;
    private static final int ENGLISH_DETAIL_WORD_ID = 6;
    private static final int RUSSIAN_DETAIL_WORDS = 7;
    private static final int RUSSIAN_DETAIL_WORD_ID = 8;
    private static final int ENGLISH_RANDOM_WORDS = 9;
    private static final int RUSSIAN_RANDOM_WORDS = 10;
    private static final int ENGLISH_TRAINING_WORDS = 11;
    private static final int RUSSIAN_TRAINING_WORDS = 12;
    private static final int ENGLISH_DETAIL_BY_NAME_WORDS = 13;
    private static final int RUSSIAN_DETAIL_BY_NAME_WORDS = 14;
    public static final String ENGLISH_WORDS_TABLE_NAME = "english";
    public static final String RUSSIAN_WORDS_TABLE_NAME = "russian";
    public static final String RELATION_TABLE_NAME = "english_russian";
    private static final String SQL_QUERY =
            "SELECT %3$s FROM %1$s as T1 JOIN english_russian as T2" +
            " ON T1._id = T2.%1$s_id JOIN %2$s as T3 ON T2.%2$s_id = T3._id WHERE 1";

    private static UriMatcher sUriMatcher;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        // english words
        sUriMatcher.addURI(
                EnglishDictDescriptor.AUTHORITY,
                EnglishDictDescriptor.EnglishDictEnglishWords.WORDS_NAME,
                ENGLISH_WORDS);
        sUriMatcher.addURI(
                EnglishDictDescriptor.AUTHORITY,
                EnglishDictDescriptor.EnglishDictEnglishWords.WORDS_NAME + "/#",
                ENGLISH_WORD_ID);
        sUriMatcher.addURI(
                EnglishDictDescriptor.AUTHORITY,
                EnglishDictDescriptor.EnglishDictDetailEnglishWords.WORDS_NAME,
                ENGLISH_DETAIL_WORDS);
        sUriMatcher.addURI(
                EnglishDictDescriptor.AUTHORITY,
                EnglishDictDescriptor.EnglishDictDetailEnglishWords.WORDS_NAME + "/#",
                ENGLISH_DETAIL_WORD_ID);
        sUriMatcher.addURI(
                EnglishDictDescriptor.AUTHORITY,
                EnglishDictDescriptor.EnglishDictRandomEnglishWords.WORDS_NAME,
                ENGLISH_RANDOM_WORDS);
        sUriMatcher.addURI(
                EnglishDictDescriptor.AUTHORITY,
                EnglishDictDescriptor.EnglishDictTrainingEnglishWords.WORDS_NAME,
                ENGLISH_TRAINING_WORDS);
        // russian words
        sUriMatcher.addURI(
                EnglishDictDescriptor.AUTHORITY,
                EnglishDictDescriptor.EnglishDictRussianWords.WORDS_NAME,
                RUSSIAN_WORDS);
        sUriMatcher.addURI(
                EnglishDictDescriptor.AUTHORITY,
                EnglishDictDescriptor.EnglishDictRussianWords.WORDS_NAME + "/#",
                RUSSIAN_WORD_ID);
        sUriMatcher.addURI(
                EnglishDictDescriptor.AUTHORITY,
                EnglishDictDescriptor.EnglishDictDetailRussianWords.WORDS_NAME,
                RUSSIAN_DETAIL_WORDS);
        sUriMatcher.addURI(
                EnglishDictDescriptor.AUTHORITY,
                EnglishDictDescriptor.EnglishDictDetailRussianWords.WORDS_NAME + "/#",
                RUSSIAN_DETAIL_WORD_ID);
        sUriMatcher.addURI(
                EnglishDictDescriptor.AUTHORITY,
                EnglishDictDescriptor.EnglishDictRandomRussianWords.WORDS_NAME,
                RUSSIAN_RANDOM_WORDS);
        sUriMatcher.addURI(
                EnglishDictDescriptor.AUTHORITY,
                EnglishDictDescriptor.EnglishDictTrainingRussianWords.WORDS_NAME,
                RUSSIAN_TRAINING_WORDS);
        sUriMatcher.addURI(
                EnglishDictDescriptor.AUTHORITY,
                EnglishDictDescriptor.EnglishDictDetailByNameEnglishWords.WORDS_NAME,
                ENGLISH_DETAIL_BY_NAME_WORDS);
        sUriMatcher.addURI(
                EnglishDictDescriptor.AUTHORITY,
                EnglishDictDescriptor.EnglishDictDetailByNameRussianWords.WORDS_NAME,
                RUSSIAN_DETAIL_BY_NAME_WORDS);
    }

    private EnglishDictDbHelper englishDictDbHelper;
    private SQLiteDatabase db;

    @Override
    public boolean onCreate() {
        init();
        return true;
    }

    // allows object initialization to be reused.
    private void init() {
        englishDictDbHelper = new EnglishDictDbHelper(getContext());
        db = englishDictDbHelper.getWritableDatabase();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String where,
                        String[] whereArgs, String sortOrder) {
        // If no sort order is specified use the default
        String orderBy;
        int match = sUriMatcher.match(uri);
        // random query
        if (Arrays.asList(ENGLISH_RANDOM_WORDS, RUSSIAN_RANDOM_WORDS).contains(match)) {
            return getRandomCursor(
                    match, uri, projection, where, whereArgs, sortOrder);
        }
        if (Arrays.asList(ENGLISH_TRAINING_WORDS, RUSSIAN_TRAINING_WORDS).contains(match)) {
            return getTrainingCursor(
                    match, uri, projection, where, whereArgs, sortOrder);
        }
        if (Arrays.asList(ENGLISH_DETAIL_BY_NAME_WORDS, RUSSIAN_DETAIL_BY_NAME_WORDS).contains(match)) {
            return getDetailByWordCursor(match, uri, projection, where, whereArgs, sortOrder);
        }
        String queryOrderBy = uri.getQueryParameter(
                EnglishDictDescriptor.EnglishDictBaseColumns.QUERY_PARAM_ORDER_BY);
        if (TextUtils.isEmpty(sortOrder)) {
            if (queryOrderBy == null || queryOrderBy.isEmpty()) {
                orderBy = EnglishDictDescriptor.EnglishDictBaseColumns.
                        SORT_ORDER.WORD.toString();
            } else {
                orderBy = queryOrderBy;
            }
        } else {
            orderBy = sortOrder;
        }
        if (Arrays.asList(ENGLISH_WORDS, RUSSIAN_WORDS,
                ENGLISH_WORD_ID, RUSSIAN_WORD_ID).contains(match)) {
            return getMainCursor(match, uri, projection, where, whereArgs, orderBy);
        } else {
            return getDetailCursor(match, uri, projection, where, whereArgs, orderBy);
        }
    }

    private Cursor getRandomCursor(int match, Uri uri, String[] projection,
                                     String where, String[] whereArgs,
                                     String sortOrder) {
        String table = getTableName(match);
        int limit;
        String limitText = uri.getQueryParameter(
                EnglishDictDescriptor.EnglishDictBaseColumns.LIMIT);
        if (limitText == null) {
            limit = EnglishDictDescriptor.EnglishDictBaseColumns.DEFAULT_RANDOM_LIMIT;
        } else {
            try {
                limit = Integer.parseInt(limitText);
            } catch (NumberFormatException ex) {
                limit = EnglishDictDescriptor.EnglishDictBaseColumns.DEFAULT_RANDOM_LIMIT;
            }
        }
        sortOrder = "RANDOM() LIMIT " + limit;
        return db.query(table, projection, where, whereArgs, null, null, sortOrder);
    }

    private Cursor getTrainingCursor(int match, Uri uri, String[] projection,
                                     String where, String[] whereArgs, String sortOrder) {
        Cursor cursor;
        String sqlQuery = getSqlQuery(match, projection);
        if (where != null && !where.isEmpty()) {
            sqlQuery += " AND " + where;
        }
        sqlQuery += " GROUP BY T1.word ORDER BY T1." + sortOrder;
        cursor = db.rawQuery(sqlQuery, whereArgs);
        return cursor;
    }

    private Cursor getMainCursor(int match, Uri uri, String[] projection,
                                 String where, String[] whereArgs, String sortOrder) {
        Cursor cursor;
        String table = getTableName(match);
        Uri contentUri = getContentUri(match);
        String queryText = uri.getQueryParameter(
                EnglishDictDescriptor.EnglishDictBaseColumns.QUERY_PARAM_NAME);
        if (queryText != null) {
            String search = EnglishDictDescriptor.EnglishDictBaseColumns.QUERY_PARAM_NAME +
                    " LIKE '%" +  queryText + "%'";
            where = search + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
        }
        switch (match) {
            case ENGLISH_WORDS:
            case RUSSIAN_WORDS:
                // query the database for all words
                cursor = db.query(table, projection, where, whereArgs,  null, null, sortOrder);
                break;
            case RUSSIAN_WORD_ID:
            case ENGLISH_WORD_ID:
                // query the database for a specific word
                long wordId = ContentUris.parseId(uri);
                cursor = db.query(
                        table,
                        projection,
                        EnglishDictDescriptor.EnglishDictBaseColumns._ID + " = " +
                                wordId + (!TextUtils.isEmpty(where) ?
                                " AND (" + where + ')' : ""),
                        whereArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("unsupported uri: " + uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(), contentUri);
        return cursor;
    }

    private Cursor getDetailCursor(int match, Uri uri, String[] projection,
                                   String where, String[] whereArgs, String sortOrder) {
        Cursor c;
        String relationId = uri.getQueryParameter(
                EnglishDictDescriptor.EnglishDictBaseColumns.QUERY_RELATION_NAME);
        String sqlQuery = getSqlQuery(match, projection);
        List<String> whereParams = new ArrayList<String>();
        if (relationId != null) {
            sqlQuery += " AND T3._id = ?";
            whereParams.add(relationId);
        }
        if (where != null && !where.isEmpty()) {
            sqlQuery += " AND " + where;
            Collections.addAll(whereParams, whereArgs);
        }
        switch (match) {
            case ENGLISH_DETAIL_WORDS:
            case RUSSIAN_DETAIL_WORDS:
                // query the database for all words
                sqlQuery += " ORDER BY T1." + sortOrder;
                c = db.rawQuery(sqlQuery,
                        whereParams.toArray(new String[whereParams.size()]));
                break;
            case ENGLISH_DETAIL_WORD_ID:
            case RUSSIAN_DETAIL_WORD_ID:
                // query the database for a specific word
                long wordId = ContentUris.parseId(uri);
                sqlQuery += " AND T1._id = ?";
                sqlQuery += " ORDER BY " + sortOrder;
                whereParams.add(Long.toString(wordId));
                c = db.rawQuery(sqlQuery,
                        whereParams.toArray(new String[whereParams.size()]));
                break;
            default:
                throw new IllegalArgumentException("unsupported uri: " + uri);
        }
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    private Cursor getDetailByWordCursor(int match, Uri uri, String[] projection,
                                         String where, String[] whereArgs, String sortOrder) {
        Cursor c;
        String relationId = uri.getQueryParameter(
                EnglishDictDescriptor.EnglishDictBaseColumns.QUERY_RELATION_WORD);
        String sqlQuery = getSqlQuery(match, projection);
        List<String> whereParams = new ArrayList<String>();
        if (relationId != null) {
            sqlQuery += " AND T3.word = ?";
            whereParams.add(relationId);
        }
        if (where != null && !where.isEmpty()) {
            sqlQuery += " AND " + where;
            Collections.addAll(whereParams, whereArgs);
        }
        if (!(sortOrder == null || sortOrder.isEmpty())) {
            sqlQuery += " ORDER BY T1." + sortOrder;
        }
        c = db.rawQuery(sqlQuery,
                whereParams.toArray(new String[whereParams.size()]));
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case ENGLISH_WORDS:
            case RUSSIAN_WORDS:
            case ENGLISH_DETAIL_WORDS:
            case RUSSIAN_DETAIL_WORDS:
            case ENGLISH_DETAIL_BY_NAME_WORDS:
            case RUSSIAN_DETAIL_BY_NAME_WORDS:
            case ENGLISH_RANDOM_WORDS:
            case RUSSIAN_RANDOM_WORDS:
                return EnglishDictDescriptor.EnglishDictBaseColumns.CONTENT_TYPE;
            case ENGLISH_WORD_ID:
            case RUSSIAN_WORD_ID:
            case ENGLISH_DETAIL_WORD_ID:
            case RUSSIAN_DETAIL_WORD_ID:
                return EnglishDictDescriptor.EnglishDictBaseColumns.CONTENT_WORD_TYPE;
            default:
                throw new IllegalArgumentException("Unknown url type: " + uri);
        }
    }

    private String getEnglishSqlQuery(String projection) {
        return String.format(SQL_QUERY, ENGLISH_WORDS_TABLE_NAME,
                RUSSIAN_WORDS_TABLE_NAME, projection);
    }

    private String getRussianSqlQuery(String projection) {
        return String.format(SQL_QUERY, RUSSIAN_WORDS_TABLE_NAME,
                ENGLISH_WORDS_TABLE_NAME, projection);
    }

    private String prepareSqlProjection(String[] projection) {
        if (projection == null || projection.length == 0) { return "T1.*"; }
        String [] args = new String[projection.length];
        for (int i = 0; i< projection.length; i++) {
            args[i] = "T1." + projection[i];
        }
        return join(args, ", ");
    }

    private String getSqlQuery(int match, String[] projection) {
        switch (match) {
            case ENGLISH_DETAIL_WORDS:
            case ENGLISH_DETAIL_BY_NAME_WORDS:
            case ENGLISH_TRAINING_WORDS:
            case ENGLISH_DETAIL_WORD_ID:
                return getEnglishSqlQuery(prepareSqlProjection(projection));
            case RUSSIAN_DETAIL_WORDS:
            case RUSSIAN_DETAIL_BY_NAME_WORDS:
            case RUSSIAN_TRAINING_WORDS:
            case RUSSIAN_DETAIL_WORD_ID:
                return getRussianSqlQuery(prepareSqlProjection(projection));
            default:
                throw new IllegalArgumentException("Unknown url match: " + match);
        }
    }

    public Uri getContentUri(int match) {
        switch (match) {
            case ENGLISH_WORDS:
            case ENGLISH_DETAIL_WORDS:
            case ENGLISH_WORD_ID:
            case ENGLISH_DETAIL_WORD_ID:
                return EnglishDictDescriptor.EnglishDictEnglishWords.CONTENT_URI;
            case RUSSIAN_WORDS:
            case RUSSIAN_DETAIL_WORDS:
            case RUSSIAN_WORD_ID:
            case RUSSIAN_DETAIL_WORD_ID:
                return EnglishDictDescriptor.EnglishDictRussianWords.CONTENT_URI;
            default:
                throw new IllegalArgumentException("Unknown url match: " + match);
        }
    }

    private void verifyValues(ContentValues values) {
        // Make sure that the fields are all set
        if (!values.containsKey(EnglishDictDescriptor.EnglishDictBaseColumns.RATING)) {
            values.put(EnglishDictDescriptor.EnglishDictBaseColumns.RATING, 0);
        }
    }

    private String getTableName(int match) {
        switch (match) {
            case ENGLISH_WORDS:
            case ENGLISH_DETAIL_WORDS:
            case ENGLISH_WORD_ID:
            case ENGLISH_DETAIL_WORD_ID:
            case ENGLISH_RANDOM_WORDS:
            case ENGLISH_TRAINING_WORDS:
                return ENGLISH_WORDS_TABLE_NAME;
            case RUSSIAN_WORDS:
            case RUSSIAN_DETAIL_WORDS:
            case RUSSIAN_WORD_ID:
            case RUSSIAN_DETAIL_WORD_ID:
            case RUSSIAN_RANDOM_WORDS:
            case RUSSIAN_TRAINING_WORDS:
                return RUSSIAN_WORDS_TABLE_NAME;
            default:
                throw new IllegalArgumentException("Unknown identifier: " + match);
        }
    }

    private boolean isRussian(int match) {
        switch (match) {
            case RUSSIAN_WORDS:
            case RUSSIAN_DETAIL_WORDS:
            case RUSSIAN_WORD_ID:
            case RUSSIAN_DETAIL_WORD_ID:
                return true;
            default:
                return false;
        }
    }

    private boolean isEnglish(int match) {
        switch (match) {
            case ENGLISH_WORDS:
            case ENGLISH_DETAIL_WORDS:
            case ENGLISH_WORD_ID:
            case ENGLISH_DETAIL_WORD_ID:
                return true;
            default:
                return false;
        }
    }

    public String contentValuesToSql(ContentValues values) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry: values.valueSet()) {
            String value = entry.getValue().toString();
            if (sb.length() > 0) {
                sb.append(" AND ");
            }
            sb.append(entry.getKey()).append('=').append(value);
        }
        return sb.toString();
    }

    public Uri insertMain(int match, Uri uri, ContentValues initialValues) {
        // Validate the requested uri
        if (match != ENGLISH_WORDS && match != RUSSIAN_WORDS)  {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        Uri contentUri = getContentUri(match);
        String table = getTableName(match);
        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }
        verifyValues(values);
        // insert the initialValues into a new database row
        SQLiteDatabase db = englishDictDbHelper.getWritableDatabase();
        Cursor cursor = db.query(
                table, null, "word = ?",
                new String[]{values.getAsString("word")},  null, null, null);
        long rowId;
        if (cursor.getCount() == 1) {
            // already exists. get Id
            cursor.moveToFirst();
            rowId = cursor.getLong(EnglishDictDescriptor.EnglishDictBaseColumns.__ID);
        } else {
            // insert the initialValues into a new database row
            rowId = db.insert(table, null, values);
        }
        cursor.close();
        if (rowId > 0) {
            Uri wordURi = ContentUris.withAppendedId(contentUri, rowId);
            getContext().getContentResolver().notifyChange(wordURi, null);
            return wordURi;
        }
        throw new SQLException("Failed to insert row into " + uri);
    }

    public Uri insertDetails(int match, Uri uri, ContentValues initialValues) {
        // Validate the requested uri
        if (match != ENGLISH_DETAIL_WORDS && match != RUSSIAN_DETAIL_WORDS)  {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        String relationId = uri.getQueryParameter(
                EnglishDictDescriptor.EnglishDictBaseColumns.QUERY_RELATION_NAME);
        if (relationId == null) {
            throw new IllegalArgumentException(
                    "Failed to insert row into " + uri + ". No relation id.");
        }
        String table = getTableName(match);
        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }
        verifyValues(values);
        SQLiteDatabase db = englishDictDbHelper.getWritableDatabase();
        Cursor cursor = db.query(
                table, null, "word = ?",
                new String[]{values.getAsString("word")},  null, null, null);
        long rId;
        if (cursor.getCount() == 1) {
            // already exists. get Id
            cursor.moveToFirst();
            rId = cursor.getLong(EnglishDictDescriptor.EnglishDictBaseColumns.__ID);
        } else {
            // insert the initialValues into a new database row
            rId = db.insert(table, null, values);
        }
        // make relation
        values.clear();
        boolean isEng = isEnglish(match);
        boolean isRus = isRussian(match);
        values.put("english_id", isEng ? rId : Long.valueOf(relationId));
        values.put("russian_id", isRus ? rId : Long.valueOf(relationId));
        cursor = db.query(
                RELATION_TABLE_NAME, null, contentValuesToSql(values),
                null,  null, null, null);
        if (cursor.getCount() == 1) {
            cursor.moveToFirst();
            cursor.getLong(EnglishDictDescriptor.EnglishDictBaseColumns.__ID);
        } else {
            db.insert(RELATION_TABLE_NAME, null, values);
        }
        cursor.close();
        if (rId > 0) {
            Uri wordURi = ContentUris.withAppendedId(uri, rId);
            getContext().getContentResolver().notifyChange(wordURi, null);
            return wordURi;
        }
        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        // Validate the requested uri
        int match = sUriMatcher.match(uri);
        if (Arrays.asList(ENGLISH_WORDS, RUSSIAN_WORDS).contains(match)) {
            return insertMain(match, uri, initialValues);
        } else {
            return insertDetails(match, uri, initialValues);
        }
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = englishDictDbHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        int affected;
        String table = getTableName(match);
        switch (match) {
            case ENGLISH_WORDS:
            case RUSSIAN_WORDS:
            case RUSSIAN_DETAIL_WORDS:
            case ENGLISH_DETAIL_WORDS:
                affected = db.delete(table, where, whereArgs);
                break;
            case ENGLISH_WORD_ID:
            case RUSSIAN_WORD_ID:
            case RUSSIAN_DETAIL_WORD_ID:
            case ENGLISH_DETAIL_WORD_ID:
                long wordId = ContentUris.parseId(uri);
                affected = db.delete(table, BaseColumns._ID + "=" + wordId
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
                        whereArgs);
                break;
            default:
                throw new IllegalArgumentException("unknown URI: " + uri);
        }
        // the call to notify the uri after deletion is explicit
        getContext().getContentResolver().notifyChange(uri, null);
        return affected;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = englishDictDbHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        int affected;
        String table = getTableName(match);
        switch (match) {
            case ENGLISH_WORDS:
            case RUSSIAN_WORDS:
            case ENGLISH_DETAIL_WORDS:
            case RUSSIAN_DETAIL_WORDS:
                affected = db.update(table, values, where, whereArgs);
                break;
            case ENGLISH_WORD_ID:
            case RUSSIAN_WORD_ID:
            case RUSSIAN_DETAIL_WORD_ID:
            case ENGLISH_DETAIL_WORD_ID:
                long wordId = ContentUris.parseId(uri);
                affected = db.update(table, values,
                        BaseColumns._ID + "=" + wordId
                                + (!TextUtils.isEmpty(where) ?
                                " AND (" + where + ')' : ""),
                        whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return affected;
    }
}
