package com.louise.udacity.mydict;

import android.app.Application;
import android.support.multidex.MultiDexApplication;

import timber.log.Timber;

public class MyApp extends MultiDexApplication{


    @Override
    public void onCreate() {
        super.onCreate();

        Timber.plant(new Timber.DebugTree());

        JobScheduler.scheduleJobs(this);
    }
}
