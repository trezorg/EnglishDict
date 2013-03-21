package by.trezor.android.EnglishDictApp.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static by.trezor.android.EnglishDictApp.EnglishDictHelper.readAssetFile;

import java.io.*;

public class EnglishDictDbHelper extends SQLiteOpenHelper {

    private static final String TAG = EnglishDictDbHelper.class.getSimpleName();
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "englishDict";
    private static final String DATABASE_SCHEMA_SQL_FILE =
            "database/englishDictSchema.sql";
    private static final String DATABASE_DROP_SQL_FILE =
            "database/englishDictDrop.sql";
    private static final String DATABASE_DATA_SQL_FILE =
            "database/englishDictData.sql";
    private static final String DATABASE_TRIGGERS_SQL_FILE =
            "database/englishDictTriggers.sql";
    private final Context context;

    public EnglishDictDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        makeDatabase(sqLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldv, int newv) {
        dropDatabase(sqLiteDatabase);
        makeDatabase(sqLiteDatabase);
    }

    private void makeDatabase(SQLiteDatabase sqLiteDatabase) {
        Log.i(TAG, "Creating database schema");
        createDatabase(sqLiteDatabase);
        addTriggers(sqLiteDatabase);
        Log.i(TAG, "Filling database");
        fillDatabase(sqLiteDatabase);
    }

    private void execSqlFile(SQLiteDatabase sqLiteDatabase, String filename, String regex) {
        String sql;
        try {
            sql = readAssetFile(context, filename);
        } catch (IOException ex1) {
            return;
        }
        String[] queries = sql.split(regex);
        sqLiteDatabase.beginTransaction();
        try {
            for (String query: queries) {
                query = query.trim();
                sqLiteDatabase.execSQL(query);
            }
            sqLiteDatabase.setTransactionSuccessful();
        } finally {
            sqLiteDatabase.endTransaction();
        }
    }

    private void execSqlFile(SQLiteDatabase sqLiteDatabase, String filename) {
        execSqlFile(sqLiteDatabase, filename, ";");
    }

    private void createDatabase(SQLiteDatabase sqLiteDatabase) {
        execSqlFile(sqLiteDatabase, DATABASE_SCHEMA_SQL_FILE);
    }

    private void fillDatabase(SQLiteDatabase sqLiteDatabase) {
        execSqlFile(sqLiteDatabase, DATABASE_DATA_SQL_FILE);
    }

    private void addTriggers(SQLiteDatabase sqLiteDatabase) {
        execSqlFile(sqLiteDatabase, DATABASE_TRIGGERS_SQL_FILE, "---");
    }

    private void dropDatabase(SQLiteDatabase sqLiteDatabase) {
        execSqlFile(sqLiteDatabase, DATABASE_DROP_SQL_FILE);
    }
}