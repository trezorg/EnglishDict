package by.trezor.android.EnglishDictApp.training;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.widget.*;
import by.trezor.android.EnglishDictApp.EnglishDictGoogleVoice;
import by.trezor.android.EnglishDictApp.R;
import by.trezor.android.EnglishDictApp.provider.EnglishDictDescriptor;
import by.trezor.android.EnglishDictApp.service.EnglishDictGoogleVoiceService;
import com.actionbarsherlock.app.SherlockFragment;

import java.io.IOException;
import java.util.*;

import static by.trezor.android.EnglishDictApp.EnglishDictHelper.*;


public class EnglishDictTrainingFragment extends SherlockFragment {

    private static final String TAG = EnglishDictTrainingFragment.class.getSimpleName();
    private int mLangType = ENGLISH_WORDS;
    private long mId;
    private int mRating;
    private String mWord;
    private String mRightWord;
    private ProgressBar mProgressBar;
    private Map<String, Integer> mWords;
    private List<String> mAnswerWords;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.english_dict_training, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        registerForContextMenu(getView());
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                showProgressBar();
            }
            @Override
            protected Void doInBackground(Void... args) {
                populateWords();
                prepareAnswerWords();
                return null;
            }
            @Override
            protected void onPostExecute(Void args) {
                prepareView();
                dismissProgressBar();
            }
        }.execute();
        if (getCurrentLangType() == ENGLISH_WORDS) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... args) {
                    prepareVoiceFile();
                    return null;
                }
            }.execute();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        mId = bundle.getLong(WORD_ID);
        mLangType = bundle.getInt(LANG_TYPE, ENGLISH_WORDS);
        mWord = bundle.getString(WORD);
        mRating = bundle.getInt(RATING);
    }

    private Uri getContentUri(int type, long id) {
        Uri uri;
        switch (type) {
            case ENGLISH_WORDS:
                uri = EnglishDictDescriptor.EnglishDictDetailRussianWords.CONTENT_URI;
                break;
            case RUSSIAN_WORDS:
                uri = EnglishDictDescriptor.EnglishDictDetailEnglishWords.CONTENT_URI;
                break;
            default:
                throw new IllegalArgumentException("Unknown language type: " + type);
        }
        return Uri.parse(uri + "?" +
                EnglishDictDescriptor.EnglishDictBaseColumns.QUERY_RELATION_NAME +
                "=" + id);
    }

    private Uri getWordUpdateContentUri(int type, long wordId) {
        Uri uri;
        switch (type) {
            case ENGLISH_WORDS:
                uri = EnglishDictDescriptor.EnglishDictDetailEnglishWords.CONTENT_URI;
                break;
            case RUSSIAN_WORDS:
                uri = EnglishDictDescriptor.EnglishDictDetailRussianWords.CONTENT_URI;
                break;
            default:
                throw new IllegalArgumentException("Unknown language type: " + type);
        }
        return ContentUris.withAppendedId(uri, wordId);
    }

    private Uri getReverseContentUri(int type) {
        Uri uri;
        switch (type) {
            case ENGLISH_WORDS:
                uri = EnglishDictDescriptor.EnglishDictDetailEnglishWords.CONTENT_URI;
                break;
            case RUSSIAN_WORDS:
                uri = EnglishDictDescriptor.EnglishDictDetailRussianWords.CONTENT_URI;
                break;
            default:
                throw new IllegalArgumentException("Unknown language type: " + type);
        }
        return uri;
    }

    private int getMaxRating() {
        Cursor cursor = getSherlockActivity().getContentResolver().query(
                getReverseContentUri(getCurrentLangType()),
                new String[]{
                        EnglishDictDescriptor.EnglishDictBaseColumns.RATING,
                },
                null, null, EnglishDictDescriptor.EnglishDictBaseColumns.RATING
                + " DESC LIMIT 1"
        );
        cursor.moveToNext();
        int rating = cursor.getInt(0);
        cursor.close();
        return rating;
    }

    private int getNewRating(boolean answer) {
        if (!answer) {
            return mRating - 1;
        }
        int newRating = mRating + (getMaxRating() - mRating) / 2;
        if (mRating == newRating) {
            newRating++;
        }
        return newRating;
    }

    private void updateWordRating(boolean answer) {
        ContentValues values = new ContentValues();
        int rating = getNewRating(answer);
        values.put(RATING, rating);
        getSherlockActivity().getContentResolver().update(
                getWordUpdateContentUri(getCurrentLangType(), mId),
                values, null, null
        );
        Log.d(TAG, String.format("Set new rating for word %s. Was %s => set %s",
                mWord, mRating, rating));
    }

    private Uri getRandomUri(int type) {
        switch (type) {
            case ENGLISH_WORDS:
                return EnglishDictDescriptor.EnglishDictRandomRussianWords.CONTENT_URI;
            case RUSSIAN_WORDS:
                return EnglishDictDescriptor.EnglishDictRandomEnglishWords.CONTENT_URI;
            default:
                throw new IllegalArgumentException("Unknown language type: " + type);
        }
    }

    private View getRightAnswerView() {
        int position = mAnswerWords.indexOf(mRightWord);
        if (position == -1)  {
            return null;
        }
        LinearLayout layout =
                (LinearLayout) getView().findViewById(R.id.training_responses);
        return layout.findViewWithTag(position);
    }

    private void transferAnswerView() {
        View view = getRightAnswerView();
        if (view == null) { return; }
        ((ViewGroup) view.getParent()).removeView(view);
        view.setBackgroundResource(
                R.drawable.training_right_answer_background);
        LinearLayout layout =
                (LinearLayout) getView().findViewById(R.id.training_right_answer);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        view.setLayoutParams(params);
        layout.addView(view);
    }

    private void prepareVoiceFile() {
        final EnglishDictGoogleVoice voice = EnglishDictGoogleVoice.getInstance();
        voice.setContext(getSherlockActivity());
        try {
            voice.prepareVoiceFiles(mWord.split("\\s+"));
        } catch (IOException ex) {
            Log.e(TAG, "Cannot download voice file for word " + mWord, ex);
        }
    }

    private void prepareQuestionWordView() {
        ((TextView) getView().findViewById(R.id.training_word)).setText(mWord);
        View soundButton = getView().findViewById(R.id.english_dict_sound);
        if (mLangType == RUSSIAN_WORDS) {
            soundButton.setVisibility(View.GONE);
            return;
        }
        soundButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        playSound(view);
                    }
                });
    }

    private void removeOnClickListeners() {
        final LinearLayout mainLayout =
                (LinearLayout) getView().findViewById(R.id.training_responses);
        int childCount = mainLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ((LinearLayout) mainLayout.getChildAt(i)).getChildAt(0).
                    setOnClickListener(null);
        }
    }

    private View.OnClickListener getChoiceOnClickListener() {
        final Button voiceButton =
                (Button) getView().findViewById(R.id.english_dict_sound);
        return new View.OnClickListener() {
            public void onClick(View view) {
                showProgressBar();
                int tag = (Integer) view.getTag();
                String word = mAnswerWords.get(tag);
                Integer wordId = mWords.get(word);
                final boolean state;
                if (wordId == null) {
                    state = false;
                    view.setBackgroundResource(
                            R.drawable.training_wrong_answer_background);
                } else {
                    state = true;
                    if (mLangType != RUSSIAN_WORDS) {
                        playSound(voiceButton);
                    } else {
                        playSound(word, true);
                    }
                }
                transferAnswerView();
                removeOnClickListeners();
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... args) {
                        updateWordRating(state);
                        return null;
                    }
                }.execute();
                ((EnglishDictTrainingActivity) getSherlockActivity()).
                        setViewPagerEnableState(state);
                dismissProgressBar();
            }
        };
    }

    private void prepareAnswerWordsView() {
        final LinearLayout mainLayout =
                (LinearLayout) getView().findViewById(R.id.training_responses);
        View.OnClickListener listener = getChoiceOnClickListener();
        int i = 0;
        for (String word: mAnswerWords) {
            TextView wordView = (TextView)
                    View.inflate(getSherlockActivity(),
                            R.layout.english_dict_text_view, null);
            LinearLayout layout = new LinearLayout(getActivity());
            layout.setGravity(Gravity.CENTER_HORIZONTAL);
            wordView.setText(word);
            wordView.setTag(i);
            layout.setTag(i);
            i++;
            layout.addView(wordView);
            mainLayout.addView(layout);
            wordView.setOnClickListener(listener);
        }
    }

    private void setRandomAnswerWord() {
        int size = mWords.size();
        if (size == 0) {
            return;
        }
        String[] words = mWords.keySet().toArray(new String[size]);
        mRightWord = words[(new Random()).nextInt(words.length)];
    }

    private void prepareView() {
        prepareQuestionWordView();
        prepareAnswerWordsView();
    }

    private void prepareAnswerWords() {
        setRandomAnswerWord();
        if (mRightWord != null) {
            mAnswerWords.add(mRightWord);
        }
        Collections.shuffle(mAnswerWords);
    }

    private void populateTranslateWords() {
        int langType = getCurrentLangType();
        Uri uri = getContentUri(langType, mId);
        ContentResolver contentResolver =
                getSherlockActivity().getContentResolver();
        Cursor cursor = contentResolver.query(
                uri, getProjection(), null, null, null);
        cursor.moveToFirst();
        mWords = new HashMap<String, Integer>();
        while (!cursor.isAfterLast()) {
            mWords.put(cursor.getString(0), cursor.getInt(1));
            cursor.moveToNext();
        }
        cursor.close();
    }

    private void populateRandomWords() {
        int langType = getCurrentLangType();
        ContentResolver contentResolver =
                getSherlockActivity().getContentResolver();
        String selection = String.format(
                "%s NOT IN (%s)",
                EnglishDictDescriptor.EnglishDictBaseColumns._ID,
                join(mWords.values(), ", ")
        );
        Cursor cursor = contentResolver.query(
                getRandomUri(langType), getProjection(), selection, null, null);
        cursor.moveToFirst();
        mAnswerWords = new ArrayList<String>();
        while (!cursor.isAfterLast()) {
            mAnswerWords.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();
    }

    private void populateWords() {
        populateTranslateWords();
        populateRandomWords();
    }


    public final void playSound(final View view) {
        final ViewGroup parent = (ViewGroup)view.getParent();
        final ProgressBar progressBar =
                (ProgressBar)parent.findViewById(R.id.progress_bar_sound);
        progressBar.setVisibility(View.VISIBLE);
        view.setVisibility(View.GONE);
        final String word = getSoundWord();
        if (word ==  null || word.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            view.setVisibility(View.VISIBLE);
            return;
        }
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                Object err = message.obj;
                int status = message.what;
                progressBar.setVisibility(View.GONE);
                view.setVisibility(View.VISIBLE);
                if (status == Activity.RESULT_CANCELED && err != null) {
                    showToast(getActivity(), err.toString());
                }
                this.removeCallbacksAndMessages(null);
            }
        };
        Intent intent = new Intent(getActivity(), EnglishDictGoogleVoiceService.class);
        Messenger messenger = new Messenger(handler);
        intent.putExtra(PARAM_MESSAGER, messenger);
        intent.putExtra(PARAM_WORD, word);
        intent.putExtra(PARAM_NETWORK_AVAILABLE, isNetworkAvailable(getActivity()));
        getSherlockActivity().startService(intent);
    }

    final void playSound(final String word, final boolean cancel) {
        final EnglishDictGoogleVoice voice = EnglishDictGoogleVoice.getInstance();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... args) {
                if (isCancelled()) {
                    return null;
                }
                final Activity activity = getActivity();
                voice.setContext(activity);
                if (cancel) {
                    voice.finish();
                }
                if (voice.getOnErrorListener() == null) {
                    voice.setOnErrorListener(new EnglishDictGoogleVoice.OnErrorListener() {
                        @Override
                        public void onError(final String message) {
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    showToast(activity, message);
                                }
                            });
                        }
                    });
                }
                if (isCancelled()) {
                    return null;
                }
                voice.play(word.split("\\s+"));
                return null;
            }

            @Override
            protected void onCancelled(Void result) {
                voice.finish();
            }
        }.execute();
    }

    private String getSoundWord() {
        return mWord;
    }

    private void showProgressBar() {
        if (mProgressBar == null) {
            mProgressBar = (ProgressBar) getView().findViewById(R.id.progress_bar);
        }
        getSherlockActivity().findViewById(
                R.id.training_answer_parent).setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void dismissProgressBar() {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.INVISIBLE);
        }
        getSherlockActivity().findViewById(
                R.id.training_answer_parent).setVisibility(View.VISIBLE);
    }

    private int getCurrentLangType() {
        return mLangType;
    }

    private String[] getProjection() {
        return PROJECTION;
    }



}