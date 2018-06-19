package com.louise.udacity.mydict;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;

import org.joda.time.LocalDate;

import timber.log.Timber;

public class VocabularySettingActivity extends AppCompatActivity implements VocabularySettingFragment.OnDataPass {

    boolean isDownloaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vocabulary_setting);
        ActionBar actionBar = this.getSupportActionBar();
        // Set the action bar back button to look like an up button
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {

            // If today's learning vocabulary no. is less than daily count, and downloaded new list,
            // create a new instance of main activity
            int todayTotalLearn = PreferenceManager.getDefaultSharedPreferences(this)
                    .getInt(getString(R.string.pref_today_total_learn), 0);
            String dailyCountStr = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString(getString(R.string.pref_daily_count_key),
                            String.valueOf(getResources().getInteger(R.integer.default_daily_count)));
            int dailyCount = Integer.parseInt(dailyCountStr);

            if (isDownloaded && todayTotalLearn < dailyCount ) {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            } else {
                NavUtils.navigateUpFromSameTask(this);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDataPass(boolean isDownload) {
        this.isDownloaded = isDownload;
    }
}
