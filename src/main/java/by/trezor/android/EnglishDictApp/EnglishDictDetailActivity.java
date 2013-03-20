package by.trezor.android.EnglishDictApp;

import android.app.Activity;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewPager;
import android.util.Log;

import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import by.trezor.android.EnglishDictApp.provider.EnglishDictDescriptor;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import static by.trezor.android.EnglishDictApp.EnglishDictUtils.*;
import by.trezor.android.EnglishDictApp.provider.EnglishDictDescriptor.EnglishDictBaseColumns.SORT_ORDER;


public class EnglishDictDetailActivity extends SherlockFragmentActivity {

    private static final String TAG = EnglishDictDetailActivity.class.getSimpleName();
    private int mLangType = ENGLISH_WORDS;
    private SORT_ORDER mOrder = SORT_ORDER.WORD;
    private String mWord;
    private ViewPager mPager;
    private EnglishDictStatePagerAdapter mPagerAdapter;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        Intent intent = getIntent();
        long id = intent.getLongExtra(WORD_ID, -1);
        if (id == -1) {
            finish();
            return;
        }
        int langType = intent.getIntExtra(LANG_TYPE, ENGLISH_WORDS);
        int position = intent.getIntExtra(WORD_POSITION, 0);
        String orderName = intent.getStringExtra(ORDERING);
        if (orderName == null) {
            mOrder = SORT_ORDER.WORD;
        } else {
            mOrder = SORT_ORDER.valueOf(orderName);
        }
        mWord = intent.getStringExtra(WORD);
        setCurrentLangType(langType);
        setContentView(R.layout.english_dict_pager);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new EnglishDictStatePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(position);
        prepareActionBar(mWord, langType);
        Log.d(TAG, "Set detail activity");
        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                invalidateOptionsMenu();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(final com.actionbarsherlock.view.Menu menu) {
        com.actionbarsherlock.view.MenuInflater inflate = getSupportMenuInflater();
        inflate.inflate(getAbsMenuLayout(), menu);
        menu.findItem(R.id.menu_detail_previous).setVisible(mPager.getCurrentItem() > 0);
        menu.findItem(R.id.menu_detail_next).setVisible(mPager.getCurrentItem() != mPagerAdapter.getCount() - 1);
        prepareActionBar(getCurrentWord(), getCurrentLangType());
        simulatePlaySound(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                ((EnglishDictDetailFragment)getCurrentFragment()).getAddAlertDialog();
                return true;
            case R.id.menu_settings:
                showPreferences(this);
                return true;
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

    private void simulatePlaySound(Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.menu_detail_sound);
        boolean isPlaySound = shouldPronounceSound(this);
        if (menuItem != null && isPlaySound) {
            View view = menuItem.getActionView().findViewById(R.id.english_dict_sound);
            EnglishDictGoogleVoice.getInstance().onFinish();
            playSound(view);
        }
    }

    public void playSound(final View view) {
        final LinearLayout parent = (LinearLayout)view.getParent();
        final ProgressBar progressBar =
                (ProgressBar)parent.findViewById(R.id.progress_bar_sound);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                progressBar.setVisibility(View.VISIBLE);
                view.setVisibility(View.GONE);
                setVolume(getActivity(), 70);
            }
            @Override
            protected Void doInBackground(Void... args) {
                EnglishDictGoogleVoice voice = EnglishDictGoogleVoice.getInstance();
                final Activity activity = getActivity();
                voice.addOnExecuteListener(new EnglishDictGoogleVoice.OnExecuteListener() {
                    @Override
                    public void onExecute() {
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                                view.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                });
                if (voice.getOnErrorListener() == null) {
                    voice.setOnErrorListener(new EnglishDictGoogleVoice.OnErrorListener() {
                        @Override
                        public void onError(final String message) {
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    showToast(getActivity(), message);
                                }
                            });
                        }
                    });
                }
                String word = getSoundWord(view);
                String [] words = word.split("\\s+");
                voice.play(words);
                return null;
            }
        }.execute();
    }

    private String getSoundWord(View view) {
        if (getCurrentLangType() == RUSSIAN_WORDS) {
            return mWord;
        }
        ListFragment fragment = getCurrentFragment();
        int position = fragment.getListView().getPositionForView((LinearLayout)view.getParent());
        return ((Cursor)(fragment.getListAdapter().getItem(position))).getString(0);
    }

    private SherlockListFragment getCurrentFragment() {
        return (SherlockListFragment) mPagerAdapter.instantiateItem(mPager, mPager.getCurrentItem());
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

    private void setCurrentLangType(int type) {
        mLangType = type;
    }

    Uri getContentUri() {
        Uri uri;
        int type = getCurrentLangType();
        switch (getCurrentLangType()) {
            case ENGLISH_WORDS:
                uri = EnglishDictDescriptor.EnglishDictRussianWords.CONTENT_URI;
                break;
            case RUSSIAN_WORDS:
                uri = EnglishDictDescriptor.EnglishDictEnglishWords.CONTENT_URI;
                break;
            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
        uri = Uri.parse(uri + "?" + EnglishDictDescriptor.EnglishDictBaseColumns.QUERY_PARAM_ORDER_BY
                + "=" + Uri.encode(mOrder.toString()));
        return uri;
    }

    int getCurrentLangType() {
        return mLangType;
    }

    String[] getProjection() {
        return PROJECTION;
    }

    private Activity getActivity() {
        return this;
    }

    private class EnglishDictStatePagerAdapter extends FragmentStatePagerAdapter {

        private int records;
        private Cursor cursor;
        private SparseArray<ListFragment> registeredFragments = new SparseArray<ListFragment>();

        public EnglishDictStatePagerAdapter(FragmentManager fm) {
            super(fm);
            records = getCursor().getCount();
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = new EnglishDictDetailFragment();
            getCursor().moveToPosition(position);
            long wordId = cursor.getLong(1);
            Bundle args = new Bundle();
            args.putLong(WORD_ID, wordId);
            args.putInt(LANG_TYPE, getCurrentLangType());
            args.putInt(WORD_POSITION, position);
            fragment.setArguments(args);
            closeCursor();
            return fragment;
        }

        private Cursor getCursor() {
            if (cursor == null) {
                cursor = getContentResolver().query(getContentUri(), getProjection() ,null, null, null);
            }
            return cursor;
        }

        private void closeCursor() {
            if (cursor != null) {
                cursor.close();
            }
            cursor = null;
        }

        public String getCurrentWord() {
            getCursor().moveToPosition(mPager.getCurrentItem());
            String word = cursor.getString(0);
            closeCursor();
            return word;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            ListFragment fragment = (ListFragment) super.instantiateItem(container, position);
            registeredFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            registeredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        @Override
        public int getCount() {
            return records;
        }
    }
}