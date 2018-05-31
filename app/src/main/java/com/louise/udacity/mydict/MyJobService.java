package com.louise.udacity.mydict;

import android.content.Intent;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

public class MyJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters job) {
        Intent intent = new Intent();

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }
}
