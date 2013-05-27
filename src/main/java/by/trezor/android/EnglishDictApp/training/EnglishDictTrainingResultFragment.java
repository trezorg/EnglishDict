package by.trezor.android.EnglishDictApp.training;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import by.trezor.android.EnglishDictApp.EnglishDictMainActivity;
import by.trezor.android.EnglishDictApp.R;
import com.actionbarsherlock.app.SherlockFragment;

import static by.trezor.android.EnglishDictApp.EnglishDictHelper.*;


public class EnglishDictTrainingResultFragment extends SherlockFragment {

    private static final String TAG = EnglishDictTrainingResultFragment.class.getSimpleName();
    private int mLangType = ENGLISH_WORDS;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.english_dict_training_result, container, false);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        View.OnClickListener listener = getTrainingResultOnClickListener();
        getView().findViewById(R.id.training_once_more).setOnClickListener(listener);
        getView().findViewById(R.id.training_stop).setOnClickListener(listener);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        mLangType = bundle.getInt(LANG_TYPE, ENGLISH_WORDS);
    }

    private View.OnClickListener getTrainingResultOnClickListener() {
        return new View.OnClickListener() {
            public void onClick(View view) {
            switch (view.getId()) {
                case R.id.training_once_more:
                    showTrainingActivity();
                    break;
                case R.id.training_stop:
                    showMainActivity();
                    break;
                }
            }
        };
    }

    void showTrainingActivity() {
        Intent intent = new Intent(getActivity(), EnglishDictTrainingChoiceActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    void showMainActivity() {
        Intent intent = new Intent(getActivity(), EnglishDictMainActivity.class);
        Bundle args = new Bundle();
        args.putInt(LANG_TYPE, mLangType);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    public void invalidate(int rightWords, int allWords) {
        ((TextView) getView().findViewById(R.id.training_result)).setText(
                String.format(getString(R.string.training_result_text),
                        rightWords, allWords));
    }
}