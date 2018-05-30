package com.louise.udacity.mydict;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.louise.udacity.mydict.data.Constants;
import com.louise.udacity.mydict.data.VocabularyContract;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.button_archive)
    Button buttonArchive;

    @BindView(R.id.textView_word)
    TextView textViewWord;

    @BindView(R.id.textView_phonetic)
    TextView textViewPhonetic;

    @BindView(R.id.textView_translation)
    TextView textViewTranslation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);


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

    private void setVocab() {
        Cursor cursor = getContentResolver().query(ContentUris.withAppendedId(VocabularyContract.VocabularyEntry.CONTENT_URI, 10),
                null,
                null,
                null,
                null);

        if (cursor.moveToFirst()) {
            textViewWord.setText(cursor.getString(cursor.getColumnIndex(VocabularyContract.VocabularyEntry.COLUMN_WORD)));
            textViewPhonetic.setText(cursor.getString(cursor.getColumnIndex(VocabularyContract.VocabularyEntry.COLUMN_PHONETIC)));
            textViewTranslation.setText(cursor.getString(cursor.getColumnIndex(VocabularyContract.VocabularyEntry.COLUMN_TRANSLATION)));
        }

    }
}
