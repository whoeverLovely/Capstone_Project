package com.louise.udacity.mydict.service;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.louise.udacity.mydict.R;
import com.louise.udacity.mydict.data.Constants;
import com.louise.udacity.mydict.data.VocabularyContentProvider;
import com.louise.udacity.mydict.data.VocabularyContract;

import org.joda.time.LocalDate;

import timber.log.Timber;

public class MyJobService extends JobService {

    private AsyncTask mBackgrougTask;

    @SuppressLint("StaticFieldLeak")
    @Override
    public boolean onStartJob(JobParameters job) {

        mBackgrougTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                Timber.d("---------------------Starting learnListGen job-----------------------");
                // If current learning list is longer than dailyCount*3, don't schedule new vocabularies
                int numOfLearning = VocabularyContentProvider.getVocabularyNumForToday(getApplicationContext(), VocabularyContract.VocabularyEntry.STATUS_LEARNING);
                String dailyCountStr = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                        .getString(getString(R.string.pref_daily_count_key), Constants.DEFAULT_DAILY_COUNT);
                int dailyCount = Integer.parseInt(dailyCountStr);

                if (numOfLearning < dailyCount * 3) {
                    handleActionGenLearnList(dailyCount, getApplicationContext());
                } else
                    Timber.d("No new learning vocabularies generated because current one is too long!");

                Timber.d("---------------------Starting reviewListGen job-----------------------");
                handleActionUpdateReviewList();
                return null;
            }
        };
        mBackgrougTask.execute();

        return true;
    }

    // Update COLUMN_DATE to current date, status to status_learning
    public static void handleActionGenLearnList(int number, Context context) {
        LocalDate localDate = LocalDate.now();
        String curentDate = localDate.toString();

        ContentValues cv = new ContentValues();
        cv.put(VocabularyContract.VocabularyEntry.COLUMN_DATE, curentDate);
        cv.put(VocabularyContract.VocabularyEntry.COLUMN_STATUS, VocabularyContract.VocabularyEntry.STATUS_LEARNING);

        // randomly select from the table which status is 0
        String sqlWhere = VocabularyContract.VocabularyEntry._ID + " IN (SELECT " + VocabularyContract.VocabularyEntry._ID
                + " FROM " + VocabularyContract.VocabularyEntry.TABLE_NAME
                + " WHERE " + VocabularyContract.VocabularyEntry.COLUMN_STATUS + "=" + 0
                + " AND " + VocabularyContract.VocabularyEntry.COLUMN_DATE + " is null "
                + " ORDER BY RANDOM() LIMIT "
                + number + ")";

        int itemUpdated = context.getContentResolver()
                .update(VocabularyContract.VocabularyEntry.CONTENT_URI,
                        cv,
                        sqlWhere,
                        null);

        // Update SharedPreference to initialize MainActivity when it's launched
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        if (itemUpdated > 0) {
            editor.putString(context.getString(R.string.pref_learn_list_status), Constants.LIST_STATUS_READY);
        } else {
            int learnNumTotal = VocabularyContentProvider.getVocabularyNumForToday(context, VocabularyContract.VocabularyEntry.STATUS_LEARNING);
            if (learnNumTotal > 0)
                editor.putString(context.getString(R.string.pref_learn_list_status), Constants.LIST_STATUS_READY);
            else
                editor.putString(context.getString(R.string.pref_learn_list_status), Constants.LIST_STATUS_EMPTY);
        }
        editor.apply();

        Timber.d("Selected vocabularies : " + itemUpdated);
    }

    private void handleActionUpdateReviewList() {
        LocalDate targetDate = LocalDate.now().minusDays(getResources().getInteger(R.integer.review_interval_days));
        String targetDateStr = targetDate.toString();

        ContentValues cv = new ContentValues();
        cv.put(VocabularyContract.VocabularyEntry.COLUMN_STATUS, VocabularyContract.VocabularyEntry.STATUS_REVIEWING);
        int reviewVocabAdded = getContentResolver().update(VocabularyContract.VocabularyEntry.CONTENT_URI,
                cv,
                VocabularyContract.VocabularyEntry.COLUMN_STATUS + "=? AND "
                        + VocabularyContract.VocabularyEntry.COLUMN_DATE + "<=?",
                new String[]{String.valueOf(VocabularyContract.VocabularyEntry.STATUS_LEARNED), targetDateStr});

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        if (reviewVocabAdded > 0) {
            editor.putString(getString(R.string.pref_review_list_status), Constants.LIST_STATUS_READY);
            Timber.d("------------------------" + reviewVocabAdded + " vocabularies added to review list----------------------");
        } else {
            Timber.d("No new vocabularies added to reviewing list!");

            int reviewNumTotal = VocabularyContentProvider.getVocabularyNumForToday(this, VocabularyContract.VocabularyEntry.STATUS_REVIEWING);
            if (reviewNumTotal > 0)
                editor.putString(getString(R.string.pref_review_list_status), Constants.LIST_STATUS_READY);
            else
                editor.putString(getString(R.string.pref_review_list_status), Constants.LIST_STATUS_EMPTY);
        }

        editor.apply();
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        if (mBackgrougTask != null) mBackgrougTask.cancel(true);
        return true;
    }
}
