package by.trezor.android.EnglishDictApp.training;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseArray;
import android.view.ViewGroup;
import android.view.WindowManager;
import by.trezor.android.EnglishDictApp.R;
import by.trezor.android.EnglishDictApp.provider.EnglishDictDescriptor;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;


import static by.trezor.android.EnglishDictApp.EnglishDictHelper.*;


public class EnglishDictTrainingActivity extends SherlockFragmentActivity {

    private static final String TAG = EnglishDictTrainingActivity.class.getSimpleName();
    private int mLangType = ENGLISH_WORDS;
    private String mWord;
    private EnglishDictTrainingViewPager mPager;
    private EnglishDictTrainingAdapter mPagerAdapter;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        Intent intent = getIntent();
        int langType = intent.getIntExtra(LANG_TYPE, ENGLISH_WORDS);
        mWord = intent.getStringExtra(WORD);
        setCurrentLangType(langType);
        setContentView(R.layout.english_dict_pager);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mPager = (EnglishDictTrainingViewPager) findViewById(R.id.pager);
        mPagerAdapter = new EnglishDictTrainingAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.setOffscreenPageLimit(TRAINING_VIEW_PAGER_COUNT);
        setViewPagerEnabled(false);
        Log.d(TAG, "Set training activity");
        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                invalidateOptionsMenu();
                if (!mPagerAdapter.isPassedPosition()) {
                    setViewPagerEnabled(false);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        com.actionbarsherlock.view.MenuInflater inflate = getSupportMenuInflater();
        inflate.inflate(getAbsMenuLayout(), menu);
        boolean isPassed = mPagerAdapter.isPassedPosition();
        menu.findItem(R.id.menu_detail_previous).setVisible(
                mPager.getCurrentItem() > 0 && isPassed);
        menu.findItem(R.id.menu_detail_next).setVisible(
                (mPager.getCurrentItem() != mPagerAdapter.getCount() - 1)
                        && isPassed);
        prepareActionBar(getCurrentWord(), getCurrentLangType());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_detail_previous:
                mPager.setCurrentItem(mPager.getCurrentItem() - 1);
                return true;
            case R.id.menu_detail_next:
                mPager.setCurrentItem(mPager.getCurrentItem() + 1);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setCurrentWord() {
        mWord = mPagerAdapter.getCurrentWord();
    }

    private String getCurrentWord() {
        setCurrentWord();
        return mWord;
    }

    private int getAbsMenuLayout() {
        return R.menu.abs_training_menu;
    }

    private void prepareActionBar(String word, int lang) {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(word);
        actionBar.setIcon(
                lang == ENGLISH_WORDS ? R.drawable.ic_usa: R.drawable.ic_russian);

    }

    private void setCurrentLangType(int type) {
        mLangType = type;
    }

    Uri getContentUri() {
        Uri uri;
        int type = getCurrentLangType();
        switch (type) {
            case ENGLISH_WORDS:
                uri = EnglishDictDescriptor.EnglishDictTrainingEnglishWords.CONTENT_URI;
                break;
            case RUSSIAN_WORDS:
                uri = EnglishDictDescriptor.EnglishDictTrainingRussianWords.CONTENT_URI;
                break;
            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
        return uri;
    }

    int getCurrentLangType() {
        return mLangType;
    }

    String[] getProjection() {
        return PROJECTION;
    }

    private void setViewPagerEnabled(boolean value) {
        mPager.setPagingEnabled(value);
    }

    void setViewPagerEnableState(boolean value) {
        setViewPagerEnabled(true);
        mPagerAdapter.addPassedPosition(mPager.getCurrentItem(), value);
        invalidateOptionsMenu();
    }

    private class EnglishDictTrainingAdapter extends FragmentStatePagerAdapter {

        private int records;
        private SparseArray<String> registeredWords = new SparseArray<String>();
        private SparseArray<Boolean> passedPositions = new SparseArray<Boolean>();
        private Cursor cursor;

        public EnglishDictTrainingAdapter(FragmentManager fm) {
            super(fm);
            records = getCursor().getCount();
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = new EnglishDictTrainingFragment();
            Cursor cursor = getCursor();
            cursor.moveToPosition(position);
            long wordId = cursor.getLong(1);
            int rating = cursor.getInt(2);
            String word = cursor.getString(0);
            registeredWords.put(position, word);
            Bundle args = new Bundle();
            args.putString(WORD, word);
            args.putLong(WORD_ID, wordId);
            args.putInt(LANG_TYPE, getCurrentLangType());
            args.putInt(RATING, rating);
            fragment.setArguments(args);
            return fragment;
        }

        private Cursor getCursor() {
            if (cursor == null) {
                int number = getTrainingWordsSetting(EnglishDictTrainingActivity.this);
                cursor = getContentResolver().query(
                        getContentUri(), getProjection(),
                        null, null,
                        EnglishDictDescriptor.EnglishDictBaseColumns.RATING
                                + " LIMIT " + number
                );
            }
            return cursor;
        }

        private void closeCursor() {
            if (cursor != null) {
                cursor.close();
            }
        }

        public String getCurrentWord() {
            return registeredWords.get(mPager.getCurrentItem());
        }

        public boolean isPassedPosition() {
            return passedPositions.get(mPager.getCurrentItem()) != null;
        }

        public void addPassedPosition(int position, boolean value) {
            passedPositions.put(position, value);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            registeredWords.remove(position);
        }

        @Override
        public int getCount() {
            return records;
        }
    }
}