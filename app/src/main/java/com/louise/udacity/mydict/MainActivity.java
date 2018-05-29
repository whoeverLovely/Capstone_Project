package com.louise.udacity.mydict;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.button_archive)
    Button buttonArchive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        buttonArchive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VocabularyIntentService.startActionDownloadVocabulary(MainActivity.this, "gre");
            }
        });
    }
}
