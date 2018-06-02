package com.louise.udacity.mydict;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
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

import com.google.android.gms.common.api.Api;
import com.louise.udacity.mydict.data.ClientVocabulary;
import com.louise.udacity.mydict.data.VocabularyContentProvider;
import com.louise.udacity.mydict.data.VocabularyContract;

import org.joda.time.LocalDate;

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
    private static Cursor mCursor = null;
    private static int currentItemIndex;
    private static boolean isDialogDisplay;
    private static AlertDialog.Builder dialogBuilder;
    private static ClientVocabulary mClientVocabulary;
    private static String currentList;

    private static final String STATE_KEY_DISPLAY_DIALOG = "isDialogDisplay";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        getSupportLoaderManager().initLoader(LOADER_LEARN_LIST_ID, null, this);
        mClientVocabulary = new ClientVocabulary();

        if (savedInstanceState != null) {
            isDialogDisplay = savedInstanceState.getBoolean(STATE_KEY_DISPLAY_DIALOG);
            if (isDialogDisplay)
                dialogBuilder.show();

        }

        buttonArchive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set status to STATUS_ARCHIVE which will never appear in review or learn list
                updateStatus(VocabularyContract.VocabularyEntry.STATUS_ARCHIVE, mClientVocabulary.getId());

                Snackbar.make(v, "The word is archived. It'll not appear in review list in the future!", Snackbar.LENGTH_SHORT)
                        .show();

                displayNextVocabulary();

            }
        });

        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCursor == null || mCursor.getCount() < 1) {
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

                } else if (currentItemIndex == mCursor.getCount() - 1) {

                    // If review list is not empty, start reviewing
                    int reviewCount = VocabularyContentProvider.getVocabularyNumForToday(MainActivity.this, VocabularyContract.VocabularyEntry.STATUS_REVIEWING);
                    dialogBuilder = new AlertDialog.Builder(MainActivity.this);
                    if (reviewCount > 0) {
                        dialogBuilder.setMessage("complete_learning_list_and_start_reviewing")
                                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Load reviewing list
                                        getSupportLoaderManager().initLoader(LOADER_REVIEW_LIST_ID, null, MainActivity.this);
                                    }
                                })
                                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });

                    } else {
                        dialogBuilder.setMessage(R.string.complete_learning_list)
                                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                    }

                    dialogBuilder.show();
                    isDialogDisplay = true;

                } else {
                    // Set status to STATUS_LEARNED which will be scheduled in review list days later
                    updateStatus(VocabularyContract.VocabularyEntry.STATUS_LEARNED, mClientVocabulary.getId());
                    displayNextVocabulary();
                }
            }
        });

        buttonArchive.setClickable(false);
        buttonArchive.setTextColor(getResources().getColor(R.color.common_google_signin_btn_text_dark_disabled));

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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_KEY_DISPLAY_DIALOG, isDialogDisplay);
        super.onSaveInstanceState(outState);
    }

    private void setVocab() {
        if (mClientVocabulary != null) {
            textViewWord.setText(mClientVocabulary.getWord());
            textViewPhonetic.setText("[" + mClientVocabulary.getPhonetic() + "]");
            textViewTranslation.setText(mClientVocabulary.getTranslation());

            buttonArchive.setClickable(true);
            buttonArchive.setTextColor(buttonArchive.getTextColors().getDefaultColor());
        }
    }

   /* @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        // TODO read shared preference setting for loading list according

    }*/

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        switch (id) {
            case LOADER_LEARN_LIST_ID:
                return new CursorLoader(this,
                        VocabularyContract.VocabularyEntry.CONTENT_URI,
                        null,
                        VocabularyContract.VocabularyEntry.COLUMN_STATUS + "=" + VocabularyContract.VocabularyEntry.STATUS_LEARNING,
                        null,
                        null);

            case LOADER_REVIEW_LIST_ID:
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
        mCursor = data;

        Timber.d("The number of entries loaded: %s", data.getCount());
        data.moveToPosition(currentItemIndex);

        if (data.getCount() > 0) {
            mClientVocabulary = cursorConvertToClientVocabulary(data);
            setVocab();
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
        return clientVocabulary;
    }

    private void updateStatus(int status, long vocabId) {
        ContentValues cv = new ContentValues();
        cv.put(VocabularyContract.VocabularyEntry.COLUMN_STATUS, status);
        getContentResolver().update(ContentUris.withAppendedId(VocabularyContract.VocabularyEntry.CONTENT_URI, vocabId),
                cv,
                null,
                null);
    }

    private void displayNextVocabulary() {
        currentItemIndex++;
        mCursor.moveToPosition(currentItemIndex);
        mClientVocabulary = cursorConvertToClientVocabulary(mCursor);
        setVocab();
    }
}
