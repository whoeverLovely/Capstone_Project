package com.louise.udacity.mydict;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.louise.udacity.mydict.data.ClientVocabulary;
import com.louise.udacity.mydict.data.Constants;
import com.louise.udacity.mydict.data.VocabularyContentProvider;
import com.louise.udacity.mydict.data.VocabularyContract;
import com.louise.udacity.mydict.service.VocabularyIntentService;

import java.util.ArrayList;
import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    @BindView(R.id.button_archive)
    Button buttonArchive;

    @BindView(R.id.button_Next)
    Button buttonNext;

    @BindView(R.id.textView_word)
    TextView textViewWord;

    @BindView(R.id.textView_phonetic)
    TextView textViewPhonetic;

    @BindView(R.id.textView_translation)
    TextView textViewTranslation;

    private static final int LOADER_LEARN_LIST_ID = 100;
    private static final int LOADER_REVIEW_LIST_ID = 200;

    private AlertDialog.Builder dialogBuilder;
    private ClientVocabulary mClientVocabulary;
    private String currentList;
    private static String LIST_LEARN = "learn";
    private static String LIST_REVIEW = "review";
    private ArrayList<ClientVocabulary> clientVocabularyList;
    private int currentItemIndex;
    private boolean flag;
    public static final String EXTRA_GROUP = "com.louise.udacity.mydict.mainActivity.extra.group";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        buttonArchive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set status to STATUS_ARCHIVE which will never appear in review or learn list
                updateStatus(VocabularyContract.VocabularyEntry.STATUS_ARCHIVE, mClientVocabulary.getId());
                Snackbar.make(v, "The word is archived. It'll not appear in review list in the future!", Snackbar.LENGTH_SHORT)
                        .show();
                buttonHandler();
            }
        });

        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set status to STATUS_LEARNED which will be scheduled in review list days later
                updateStatus(VocabularyContract.VocabularyEntry.STATUS_LEARNED, mClientVocabulary.getId());
                buttonHandler();
            }
        });

        buttonArchive.setEnabled(false);
        buttonNext.setEnabled(false);

        String learnListStatus = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(R.string.pref_learn_list_status), null);
        final String reviewListStatus = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(R.string.pref_review_list_status), null);

        // If pref_learn_list_status is null or empty, no vocabulary to learn, ask users to download new lists
        if (learnListStatus == null || Constants.LIST_STATUS_EMPTY.equals(learnListStatus)) {

            dialogBuilder = new AlertDialog.Builder(MainActivity.this).setMessage(R.string.list_is_empty)
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            currentList = LIST_LEARN;
                            Intent intent = new Intent(MainActivity.this, VocabularySettingActivity.class);
                            startActivity(intent);
                        }
                    });
            dialogBuilder.show();
        }

        // pref_learn_list_status is ready, load learning list
        else if (Constants.LIST_STATUS_READY.equals(learnListStatus) || Constants.LIST_STATUS_PROCESSING.equals(learnListStatus)) {
            getSupportLoaderManager().initLoader(LOADER_LEARN_LIST_ID, null, this);
            currentList = LIST_LEARN;
        }

        // pref_learn_list_status is done, but haven't started review list, display a dialog to ask if start reviewing
        else if (Constants.LIST_STATUS_DONE.equals(learnListStatus)) {
            // If review list is ready, start reviewing
            if (Constants.LIST_STATUS_READY.equals(reviewListStatus)) {
                showIfStartReviewDialog();
            } else if (Constants.LIST_STATUS_PROCESSING.equals(reviewListStatus)){
                getSupportLoaderManager().initLoader(LOADER_REVIEW_LIST_ID, null, this);
                currentList = LIST_REVIEW;
            } else {
                showCompleteDialog();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.vocabulary_settings:
                Intent intent = new Intent(this, VocabularySettingActivity.class);
                startActivity(intent);
                return true;

            case R.id.search:
                onSearchRequested();
                return true;

            case R.id.view_group:
                if (mClientVocabulary == null) {
                    Snackbar.make(textViewWord, R.string.currentVocabulaty_is_empty, Snackbar.LENGTH_LONG).show();
                }

                // If the group is empty, display a snackbar
                Cursor cursor = getContentResolver().query(VocabularyContentProvider.buildVocabularyUriWithWord(mClientVocabulary.getWord()),
                        new String[]{VocabularyContract.VocabularyEntry.COLUMN_GROUP_NAME},
                        null,
                        null,
                        null);
                cursor.moveToFirst();
                String group = cursor.getString(0);
                if ("".equals(group) || group == null)
                    Snackbar.make(textViewWord, R.string.no_group, Snackbar.LENGTH_LONG).show();
                else {
                    Intent groupIntent = new Intent(this, GroupActivity.class);
                    groupIntent.putExtra(EXTRA_GROUP, mClientVocabulary.getGroupName());
                    startActivity(groupIntent);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void startActivity(Intent intent) {

        // Set string extra to the intent sent to SearchResultActivity
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            intent.putExtra(VocabularyIntentService.EXTRA_ORIGINAL_VOCABULARY_ID, mClientVocabulary.getId());

            String groupName = mClientVocabulary.getGroupName();
            if (groupName == null || "".equals(groupName))
                groupName = mClientVocabulary.getWord();

            intent.putExtra(VocabularyIntentService.EXTRA_ORIGINAL_VOCABULARY_GROUP, groupName);
        }
        super.startActivity(intent);
    }

    private void setVocab() {
        if (mClientVocabulary != null) {
            textViewWord.setText(mClientVocabulary.getWord());
            textViewPhonetic.setText("[" + mClientVocabulary.getPhonetic() + "]");
            textViewTranslation.setText(mClientVocabulary.getTranslation());

            buttonArchive.setEnabled(true);
            buttonNext.setEnabled(true);
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        switch (id) {
            case LOADER_LEARN_LIST_ID:
                flag = true;
                currentList = LIST_LEARN;
                currentItemIndex = 0;
                CursorLoader learnCursorLoader = new CursorLoader(this,
                        VocabularyContract.VocabularyEntry.CONTENT_URI,
                        null,
                        VocabularyContract.VocabularyEntry.COLUMN_STATUS + "=" + VocabularyContract.VocabularyEntry.STATUS_LEARNING,
                        null,
                        null);

                return learnCursorLoader;

            case LOADER_REVIEW_LIST_ID:
                flag = true;
                currentList = LIST_REVIEW;
                currentItemIndex = 0;
                return new CursorLoader(this,
                        VocabularyContract.VocabularyEntry.CONTENT_URI,
                        null,
                        VocabularyContract.VocabularyEntry.COLUMN_STATUS + "=" + VocabularyContract.VocabularyEntry.STATUS_REVIEWING,
                        null,
                        null);

            default:
                throw new IllegalArgumentException("The loader id is invalid.");

        }

    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (flag) {

            if (clientVocabularyList == null) {
                clientVocabularyList = new ArrayList<>();
                ClientVocabulary cv;
                while (data.moveToNext()) {
                    cv = cursorConvertToClientVocabulary(data);
                    clientVocabularyList.add(cv);
                }
                Collections.shuffle(clientVocabularyList);

                // Update list status to processing
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                if (LIST_LEARN.equals(currentList)) {
                    editor.putString(getString(R.string.pref_learn_list_status), Constants.LIST_STATUS_PROCESSING);
                    Timber.d("pref_learn_list_status updated to processing");
                } else if (LIST_REVIEW.equals(currentList)) {
                    editor.putString(getString(R.string.pref_review_list_status), Constants.LIST_STATUS_PROCESSING);
                    Timber.d("pref_review_list_status updated to processing");

                }
                editor.apply();
            }

            Timber.d("The number of entries loaded: %s", data.getCount());
            Timber.d("current item index: %s", currentItemIndex);

            if (data.getCount() > 0) {
                Timber.d("clientVocabularyList size is %s", clientVocabularyList.size());
                mClientVocabulary = clientVocabularyList.get(currentItemIndex);
                setVocab();
                currentItemIndex++;
            }
        }
    }

    @Override

    public void onLoaderReset(@NonNull Loader loader) {

    }

    private ClientVocabulary cursorConvertToClientVocabulary(Cursor cursor) {
        ClientVocabulary clientVocabulary = new ClientVocabulary();
        clientVocabulary.setId(cursor.getLong(cursor.getColumnIndex(VocabularyContract.VocabularyEntry._ID)));
        clientVocabulary.setWord(cursor.getString(cursor.getColumnIndex(VocabularyContract.VocabularyEntry.COLUMN_WORD)));
        clientVocabulary.setPhonetic(cursor.getString((cursor.getColumnIndex(VocabularyContract.VocabularyEntry.COLUMN_PHONETIC))));
        clientVocabulary.setTranslation(cursor.getString(cursor.getColumnIndex(VocabularyContract.VocabularyEntry.COLUMN_TRANSLATION)));
        clientVocabulary.setGroupName(cursor.getString(cursor.getColumnIndex(VocabularyContract.VocabularyEntry.COLUMN_GROUP_NAME)));
        return clientVocabulary;
    }

    private void updateStatus(int status, long vocabId) {
        flag = false;
        ContentValues cv = new ContentValues();
        cv.put(VocabularyContract.VocabularyEntry.COLUMN_STATUS, status);
        getContentResolver().update(ContentUris.withAppendedId(VocabularyContract.VocabularyEntry.CONTENT_URI, vocabId),
                cv,
                null,
                null);
    }

    private void displayNextVocabulary() {
        currentItemIndex++;
        mClientVocabulary = clientVocabularyList.get(currentItemIndex);
        setVocab();
    }

    private void showIfStartReviewDialog() {
        dialogBuilder = new AlertDialog.Builder(MainActivity.this)
                .setMessage(R.string.complete_learning_list_and_start_reviewing)
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Load reviewing list
                        getSupportLoaderManager().initLoader(LOADER_REVIEW_LIST_ID, null, MainActivity.this);

                        buttonArchive.setClickable(true);
                        buttonArchive.setTextColor(getResources().getColor(R.color.colorPrimary));
                        buttonNext.setClickable(true);
                        buttonNext.setTextColor(getResources().getColor(R.color.colorPrimary));

                        currentList = LIST_REVIEW;
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        dialogBuilder.show();
    }

    private void showCompleteDialog() {
        dialogBuilder = new AlertDialog.Builder(MainActivity.this)
                .setMessage(R.string.complete_learning_list)
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        buttonArchive.setClickable(false);
                        buttonArchive.setClickable(false);
                        dialog.dismiss();
                    }
                });
        dialogBuilder.show();
    }

    private void buttonHandler() {
        if (currentItemIndex == clientVocabularyList.size() - 1) {
            String reviewListStatus = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString(getString(R.string.pref_review_list_status), null);
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
            editor.putString(getString(R.string.pref_learn_list_status), Constants.LIST_STATUS_DONE);
            if (currentList.equals(LIST_LEARN) && Constants.LIST_STATUS_READY.equals(reviewListStatus)) {
                showIfStartReviewDialog();
            } else {
                editor.putString(getString(R.string.pref_review_list_status), Constants.LIST_STATUS_DONE);
                showCompleteDialog();
            }
            editor.apply();
        } else {
            displayNextVocabulary();
        }
    }
}
