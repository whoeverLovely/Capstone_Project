package com.louise.udacity.mydict;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.louise.udacity.mydict.data.ClientVocabulary;
import com.louise.udacity.mydict.data.Constants;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class SearchResultActivity extends AppCompatActivity {

    private ClientVocabulary mClientVocabulary;

    @BindView(R.id.textView_word_result)
    TextView textViewWord;

    @BindView(R.id.textView_phonetic_result)
    TextView textViewPhonetic;

    @BindView(R.id.textView_translation_result)
    TextView textViewTranslation;

    @BindView(R.id.button_link)
    Button buttonLink;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);
        ButterKnife.bind(this);

        buttonLink.setClickable(false);
        // Get the intent, verify the action and get the query

        Intent intent = getIntent();
        String query = intent.getStringExtra(SearchManager.QUERY);
        final long originalVocabularyId = intent.getLongExtra(VocabularyIntentService.EXTRA_ORIGINAL_VOCABULARY_ID, -1);
        final String originalGroup = intent.getStringExtra(VocabularyIntentService.EXTRA_ORIGINAL_VOCABULARY_GROUP);

        Timber.d("get original group in searchResultActivity: " + originalGroup);
        // do query
        VocabularyIntentService.startActionSearch(this, query);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(VocabularyIntentService.ACTION_SEARCH); // Action1 to filter
        intentFilter.addAction(VocabularyIntentService.ACTION_LINK); // Action2 to filter
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                intentFilter);

        buttonLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Link current vocabulary to previous one
                VocabularyIntentService.startActionLink(SearchResultActivity.this, mClientVocabulary, originalVocabularyId, originalGroup);
            }
        });
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            switch (intent.getAction()) {
                case VocabularyIntentService.ACTION_SEARCH:
                    String searchStatus = intent.getStringExtra(VocabularyIntentService.EXTRA_STATUS);
                    ClientVocabulary clientVocabulary = intent.getParcelableExtra(VocabularyIntentService.EXTRA_VOCABULARY);

                    if (clientVocabulary != null)
                        Timber.d("Received vocabulary from IntentService: " + clientVocabulary.toString());
                    else
                        Timber.d("Can't find the vocabulary");

                    if (Constants.STATUS_SUCCEEDED.equals(searchStatus) && clientVocabulary != null) {
                        mClientVocabulary = clientVocabulary;
                        displayVocabulary();
                        buttonLink.setClickable(true);
                    } else {
                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(SearchResultActivity.this);
                        dialogBuilder.setMessage("Sorry that we can't find the vocabulary!")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .show();
                    }
                    break;

                case VocabularyIntentService.ACTION_LINK:
                    String linkStatus = intent.getStringExtra(VocabularyIntentService.EXTRA_STATUS);
                    if (Constants.STATUS_SUCCEEDED.equals(linkStatus)) {
                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(SearchResultActivity.this);
                        dialogBuilder.setMessage("The two vocabularies linked!")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .show();
                    }
            }
        }
    };

    private void displayVocabulary() {
        textViewWord.setText(mClientVocabulary.getWord());
        textViewPhonetic.setText("[" + mClientVocabulary.getPhonetic() + "]");
        textViewTranslation.setText(mClientVocabulary.getTranslation());

    }
}
