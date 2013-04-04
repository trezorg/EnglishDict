package by.trezor.android.EnglishDictApp.training;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import by.trezor.android.EnglishDictApp.R;
import by.trezor.android.EnglishDictApp.provider.EnglishDictDescriptor;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import static by.trezor.android.EnglishDictApp.EnglishDictHelper.*;


public class EnglishDictTrainingChoiceActivity extends SherlockFragmentActivity {

    private ProgressBar mProgressBar;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.english_dict_training_choice);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        prepareActionBar();
        showProgressBar();
        View.OnClickListener listener = getChoiceOnClickListener();
        findViewById(R.id.english_image_button).setOnClickListener(listener);
        findViewById(R.id.russian_image_button).setOnClickListener(listener);
        prepareWordsCount();
        dismissProgressBar();
    }

    private void prepareActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
    }

    private void showTrainingActivity(int lang) {
        Bundle bundle = new Bundle();
        bundle.putInt(LANG_TYPE, lang);
        Intent intent = new Intent(this, EnglishDictTrainingActivity.class);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private View.OnClickListener getChoiceOnClickListener() {
        return new View.OnClickListener() {
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.english_image_button:
                        showTrainingActivity(ENGLISH_WORDS);
                        break;
                    case R.id.russian_image_button:
                        showTrainingActivity(RUSSIAN_WORDS);
                        break;
                }
            }
        };
    }

    private void prepareWordsCount() {
        int russianWords = getWordsCount(
                EnglishDictDescriptor.EnglishDictRussianWords.CONTENT_URI);
        int englishWords = getWordsCount(
                EnglishDictDescriptor.EnglishDictEnglishWords.CONTENT_URI);
        ((TextView) findViewById(R.id.russian_count_text)).setText(
                String.format(getString(R.string.training_words_count), russianWords));
        ((TextView) findViewById(R.id.english_count_text)).setText(
                String.format(getString(R.string.training_words_count), englishWords));
    }

    private int getWordsCount(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    private void showProgressBar() {
        if (mProgressBar == null) {
            mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        }
        findViewById(R.id.training_choice_content).setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void dismissProgressBar() {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.INVISIBLE);
        }
        findViewById(R.id.training_choice_content).setVisibility(View.VISIBLE);
    }

}