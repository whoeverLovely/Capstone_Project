package com.louise.udacity.mydict;

import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
    private static Cursor mCursor = null;
    private static int currentItemIndex;
    private static boolean isDialogDisplay;
    private static AlertDialog.Builder dialogBuilder;

    private static final String STATE_KEY_DISPLAY_DIALOG = "isDialogDisplay";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        getSupportLoaderManager().initLoader(LOADER_LEARN_LIST_ID, null, this);

        if (savedInstanceState != null) {
            isDialogDisplay = savedInstanceState.getBoolean(STATE_KEY_DISPLAY_DIALOG);
            if (isDialogDisplay)
                dialogBuilder.show();

        }

        buttonArchive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VocabularyIntentService.startActionGenLearnList(MainActivity.this, null);
            }
        });

        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentItemIndex == mCursor.getCount() - 1) {
                    dialogBuilder = new AlertDialog.Builder(MainActivity.this).setMessage(R.string.complete_learning_list)
                            .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    dialogBuilder.show();
                    isDialogDisplay = true;

                } else {
                    currentItemIndex++;
                    mCursor.moveToPosition(currentItemIndex);
                    setVocab(mCursor);
                }
            }
        });

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

    private void setVocab(Cursor cursor) {
        if (cursor != null) {
            textViewWord.setText(cursor.getString(cursor.getColumnIndex(VocabularyContract.VocabularyEntry.COLUMN_WORD)));
            textViewPhonetic.setText("[" + cursor.getString(cursor.getColumnIndex(VocabularyContract.VocabularyEntry.COLUMN_PHONETIC)) + "]");
            textViewTranslation.setText(cursor.getString(cursor.getColumnIndex(VocabularyContract.VocabularyEntry.COLUMN_TRANSLATION)));
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
        return new CursorLoader(this,
                VocabularyContract.VocabularyEntry.CONTENT_URI,
                null,
                VocabularyContract.VocabularyEntry.COLUMN_STATUS + "=" + VocabularyContract.VocabularyEntry.STATUS_NEW
                        + " AND " + VocabularyContract.VocabularyEntry.COLUMN_DATE + "<='" + LocalDate.now().toString() + "'",
                null,
                null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        mCursor = data;

        Timber.d("The number of entries loaded: %s", data.getCount());
        data.moveToPosition(currentItemIndex);
        setVocab(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader loader) {

    }

}
