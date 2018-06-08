package com.louise.udacity.mydict;

import android.support.multidex.MultiDexApplication;

import com.louise.udacity.mydict.service.JobScheduler;

import timber.log.Timber;

public class MyApp extends MultiDexApplication{


    @Override
    public void onCreate() {
        super.onCreate();

        Timber.plant(new Timber.DebugTree());

        JobScheduler.scheduleJobs(this);
    }
}
