package com.louise.udacity.mydict;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v14.preference.MultiSelectListPreference;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.widget.Button;
import android.widget.TextView;

import com.louise.udacity.mydict.data.Constants;
import com.louise.udacity.mydict.service.VocabularyIntentService;

import java.util.Set;

import timber.log.Timber;

public class VocabularySettingFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {

    Set<String> existingTags;
    AlertDialog alertDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        setSummary(sharedPreferences, getString(R.string.pref_list_tag_key));
        setSummary(sharedPreferences, getString(R.string.pref_daily_count_key));

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(VocabularyIntentService.ACTION_DOWNLOAD);
        intentFilter.addAction(VocabularyIntentService.ACTION_DELETE);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, intentFilter);

        Preference preferenceListTag = findPreference(getString(R.string.pref_list_tag_key));
        preferenceListTag.setOnPreferenceChangeListener(this);

    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = intent.getStringExtra(VocabularyIntentService.EXTRA_STATUS);

            if (alertDialog.isShowing()) {
                alertDialog.setCancelable(true);

                TextView messageView = alertDialog .findViewById(android.R.id.message);
                if (Constants.STATUS_SUCCEEDED.equals(status))
                    messageView.setText(R.string.list_update_succeed);
                else
                    messageView.setText(R.string.list_update_failed);

                Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                positiveButton.setEnabled(true);

                alertDialog.show();
            }
        }
    };

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        alertDialog = new AlertDialog.Builder(getActivity())
                .setMessage("Updating vocabulary lists!")
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                    ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            }
        });
        alertDialog.show();

        Timber.d("list tags changed");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        existingTags = sharedPreferences.getStringSet(getString(R.string.pref_list_tag_key), null);

        Set<String> newTags = (Set<String>) newValue;

        if (existingTags != null && newTags != null) {
            Timber.d("Existing tags: " + existingTags.toString());
            Timber.d("New tags: " + newTags.toString());
        }

        // Download lists which is not in the existingTags
        for (String tag : newTags) {
            if (existingTags == null || existingTags.size() == 0 || !existingTags.contains(tag)) {
                Timber.d("Starting download " + tag);
                VocabularyIntentService.startActionDownloadVocabulary(getContext(), tag);
            }
        }

        // Deleted lists which is in the exsitingTags but not in tags
        if (existingTags != null) {
            for (String tag : existingTags) {
                if (newTags.size() == 0 || !newTags.contains(tag))
                    VocabularyIntentService.startActionDeleteList(getContext(), tag);
            }
        }
        return true;
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        setSummary(sharedPreferences, key);
    }

    private void setSummary(SharedPreferences sharedPreferences, String key) {

        // Set summary for daily count
        if (key.equals(getString(R.string.pref_daily_count_key))) {
            String dailyCount = sharedPreferences.getString(key, null);
            if (dailyCount != null) {
                Preference preferenceDailyCount = findPreference(getString(R.string.pref_daily_count_key));
                preferenceDailyCount.setSummary(dailyCount);
            }
        }

        // Set summary for list tags
        if (key.equals(getString(R.string.pref_list_tag_key))) {
            Set<String> listTags = sharedPreferences.getStringSet(getString(R.string.pref_list_tag_key), null);

            if (listTags != null) {
                Preference preferenceListTags = findPreference(getString(R.string.pref_list_tag_key));
                StringBuilder tagsString = new StringBuilder();
                for (String s : listTags) {
                    MultiSelectListPreference preference = (MultiSelectListPreference) findPreference(key);
                    int index = preference.findIndexOfValue(s);
                    tagsString = tagsString.append(preference.getEntries()[index]).append("; ");
                }
                preferenceListTags.setSummary(tagsString);
            }
        }
    }
}
