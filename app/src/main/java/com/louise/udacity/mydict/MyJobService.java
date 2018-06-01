package com.louise.udacity.mydict;

import android.content.Intent;
import android.preference.PreferenceManager;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.louise.udacity.mydict.data.Constants;
import com.louise.udacity.mydict.data.VocabularyContentProvider;
import com.louise.udacity.mydict.data.VocabularyContract;

import timber.log.Timber;

public class MyJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters job) {
        Timber.d("---------------------Starting learnListGen job-----------------------");

        // If current learning list is longer than dailyCount*3, don't schedule new vocabularies
        int num = VocabularyContentProvider.getVocabularyNumForToday(this, VocabularyContract.VocabularyEntry.STATUS_LEARNING);
        String dailyCountStr = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(R.string.pref_daily_count_key), Constants.DEFAULT_DAILY_COUNT);
        int dailyCount = Integer.parseInt(dailyCountStr);
        if (num < dailyCount*3) {
            VocabularyIntentService.startActionGenLearnList(this, null);
        } else
            Timber.d("No new learning vocabularies generated because current one is too long!");
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }
}