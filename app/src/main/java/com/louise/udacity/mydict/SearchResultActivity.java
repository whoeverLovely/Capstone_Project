package com.louise.udacity.mydict;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.louise.udacity.mydict.data.ClientVocabulary;
import com.louise.udacity.mydict.data.Constants;
import com.louise.udacity.mydict.service.VocabularyIntentService;

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

    @BindView(R.id.progressbar_search)
    ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);
        ButterKnife.bind(this);

        progressBar.setVisibility(View.VISIBLE);
        // Get the intent, verify the action and get the query

        Intent intent = getIntent();
        if (intent != null) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            final long originalVocabularyId = intent.getLongExtra(VocabularyIntentService.EXTRA_ORIGINAL_VOCABULARY_ID, -1);
            final String originalGroup = intent.getStringExtra(VocabularyIntentService.EXTRA_ORIGINAL_VOCABULARY_GROUP);

            Timber.d("get original group in searchResultActivity: " + originalGroup);

            if (query != null) {
                // do query
                VocabularyIntentService.startActionSearch(this, query);

                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(VocabularyIntentService.ACTION_SEARCH); // Action1 to filter
                intentFilter.addAction(VocabularyIntentService.ACTION_LINK); // Action2 to filter
                LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                        intentFilter);
            }

            buttonLink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Link current vocabulary to previous one
                    VocabularyIntentService.startActionLink(SearchResultActivity.this, mClientVocabulary, originalVocabularyId, originalGroup);
                }
            });
            buttonLink.setEnabled(false);
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            switch (intent.getAction()) {
                case VocabularyIntentService.ACTION_SEARCH:
                    String searchStatus = intent.getStringExtra(VocabularyIntentService.EXTRA_STATUS);
                    ClientVocabulary clientVocabulary = intent.getParcelableExtra(VocabularyIntentService.EXTRA_VOCABULARY);

                    progressBar.setVisibility(View.GONE);

                    if (Constants.STATUS_SUCCEEDED.equals(searchStatus) && clientVocabulary != null) {
                        mClientVocabulary = clientVocabulary;
                        displayVocabulary();

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
                                        // Go back to MainActivity
                                        NavUtils.navigateUpFromSameTask(SearchResultActivity.this);
                                    }
                                });

                        if (!(SearchResultActivity.this).isFinishing()) {
                            dialogBuilder.show();
                        }
                    }
            }
        }
    };

    private void displayVocabulary() {
        textViewWord.setText(mClientVocabulary.getWord());
        textViewPhonetic.setText("[" + mClientVocabulary.getPhonetic() + "]");
        textViewTranslation.setText(mClientVocabulary.getTranslation());

        buttonLink.setEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);

    }
}
