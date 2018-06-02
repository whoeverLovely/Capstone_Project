package com.louise.udacity.mydict;

import android.content.Context;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.Trigger;

import java.util.concurrent.TimeUnit;

public class JobScheduler {

    private static boolean sInitialized;

    public static void scheduleJobs(Context context) {

        if (sInitialized) return;

        int intervalMin = context.getResources().getInteger(R.integer.job_interval_min);
        int intervalSec = (int) (TimeUnit.MINUTES.toSeconds(intervalMin));
        int syncFlexTimeSec = intervalSec;

        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        Job myJob = dispatcher.newJobBuilder()
                .setService(MyJobService.class) // the JobService that will be called
                .setTag(VocabularyIntentService.ACTION_GENERATE_LEARN_LIST)        // uniquely identifies the job
                .setRecurring(true)
                .setLifetime(Lifetime.FOREVER)
                .setTrigger(Trigger.executionWindow(
                        intervalSec,
                        intervalSec + syncFlexTimeSec))
                .build();

        dispatcher.mustSchedule(myJob);

        /* The job has been initialized */
        sInitialized = true;
    }
}
