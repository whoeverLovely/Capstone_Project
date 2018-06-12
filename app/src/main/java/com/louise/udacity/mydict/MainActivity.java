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

    private boolean isDialogDisplay;
    private AlertDialog.Builder dialogBuilder;
    private ClientVocabulary mClientVocabulary;
    private String currentList;
    private static String LIST_LEARN = "learn";
    private static String LIST_REVIEW = "review";
    private ArrayList<ClientVocabulary> clientVocabularyList;
    private int currentItemIndex;
    private boolean flag;
    public static final String EXTRA_GROUP = "com.louise.udacity.mydict.mainActivity.extra.group";

    private static final String STATE_KEY_DISPLAY_DIALOG = "isDialogDisplay";
    private static final String STATE_KEY_CURRENT_INDEX = "currentIndex";
    private static final String STATE_CLIENT_VOCABULARY_LIST = "vocabularyList";
    private static final String STATE_CURRENT_LIST = "currentList";

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

        buttonArchive.setClickable(false);
        buttonNext.setClickable(false);

        if (savedInstanceState != null) {
            isDialogDisplay = savedInstanceState.getBoolean(STATE_KEY_DISPLAY_DIALOG);
            if (isDialogDisplay)
                dialogBuilder.show();

            currentList = savedInstanceState.getString(STATE_CURRENT_LIST);

            clientVocabularyList = savedInstanceState.getParcelableArrayList(STATE_CLIENT_VOCABULARY_LIST);
            currentItemIndex = savedInstanceState.getInt(STATE_KEY_CURRENT_INDEX);
            mClientVocabulary = clientVocabularyList.get(currentItemIndex);
            if (mClientVocabulary != null)
                setVocab();
        }

        if (clientVocabularyList == null || clientVocabularyList.isEmpty()) {
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

                                Intent intent = new Intent(MainActivity.this, VocabularySettingActivity.class);
                                startActivity(intent);
                            }
                        });
                dialogBuilder.show();
                isDialogDisplay = true;
            }

            // pref_learn_list_status is ready, load learning list
            else if (Constants.LIST_STATUS_READY.equals(learnListStatus)) {
                getSupportLoaderManager().initLoader(LOADER_LEARN_LIST_ID, null, this);
                currentList = LIST_LEARN;
            }

            // pref_learn_list_status is done, the user already complete today's learning list
            else if (Constants.LIST_STATUS_DONE.equals(learnListStatus)) {
                // If review list is ready, start reviewing
                if (Constants.LIST_STATUS_READY.equals(reviewListStatus)) {
                    showIfStartReviewDialog();
                } else {
                    showCompleteDialog();
                }

            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        Menu mMenu = menu;
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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_KEY_DISPLAY_DIALOG, isDialogDisplay);
        outState.putInt(STATE_KEY_CURRENT_INDEX, currentItemIndex);
        outState.putParcelableArrayList(STATE_CLIENT_VOCABULARY_LIST, clientVocabularyList);
        outState.putString(STATE_CURRENT_LIST, currentList);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void startActivity(Intent intent) {
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

            buttonArchive.setClickable(true);
            buttonNext.setClickable(true);
            buttonArchive.setTextColor(getResources().getColor(R.color.colorPrimary));
            buttonNext.setTextColor(getResources().getColor(R.color.colorPrimary));
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
            }

            Timber.d("The number of entries loaded: %s", data.getCount());
            Timber.d("current item index: " + currentItemIndex);

            if (data.getCount() > 0) {
                Timber.d("clientVocabularyList size is " + clientVocabularyList.size());
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
        isDialogDisplay = true;
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
        isDialogDisplay = true;
    }

    private void buttonHandler() {
        if (currentItemIndex == clientVocabularyList.size() - 1) {
            String reviewListStatus = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString(getString(R.string.pref_review_list_status), null);
            if (currentList.equals(LIST_LEARN) && Constants.LIST_STATUS_READY.equals(reviewListStatus)) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
                editor.putString(getString(R.string.pref_learn_list_status), Constants.LIST_STATUS_DONE);
                editor.apply();

                showIfStartReviewDialog();
            } else {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
                editor.putString(getString(R.string.pref_review_list_status), Constants.LIST_STATUS_DONE);
                editor.apply();

                showCompleteDialog();
            }
        } else {
            displayNextVocabulary();
            isDialogDisplay = false;
        }
    }
}
