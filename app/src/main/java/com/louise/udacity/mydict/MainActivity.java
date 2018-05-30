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

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
            dialogBuilder.setMessage(R.string.dialog_download_complete)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
            .show();
            // Get extra data included in the Intent
            String downloadStatus = intent.getStringExtra(VocabularyIntentService.EXTRA_DOWNLOAD_STATUS);
            if (downloadStatus.equals(Constants.STATUS_SUCCEEDED))
                setVocab();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(VocabularyIntentService.ACTION_DOWNLAOD_STATUS));

        buttonArchive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VocabularyIntentService.startActionDownloadVocabulary(MainActivity.this, "gre");

            }
        });
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
