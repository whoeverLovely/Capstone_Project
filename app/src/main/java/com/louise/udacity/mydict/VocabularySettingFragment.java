package com.louise.udacity.mydict;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v14.preference.MultiSelectListPreference;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;

import com.louise.udacity.mydict.data.Constants;
import com.louise.udacity.mydict.service.VocabularyIntentService;

import java.util.Set;

import timber.log.Timber;

public class VocabularySettingFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener{

    Set<String> existingTags;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(VocabularyIntentService.ACTION_DOWNLOAD);
        intentFilter.addAction(VocabularyIntentService.ACTION_DELETE);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, intentFilter);

        Preference preference = findPreference(getString(R.string.pref_list_tag_key));
        preference.setOnPreferenceChangeListener(this);

    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String status = intent.getStringExtra(VocabularyIntentService.EXTRA_STATUS);
            switch (action) {
                case VocabularyIntentService.ACTION_DOWNLOAD:
                    if (Constants.STATUS_SUCCEEDED.equals(status)) {
                        Snackbar.make(getActivity().findViewById(android.R.id.content),
                                R.string.dialog_download_complete,
                                Snackbar.LENGTH_LONG).show();
                    }
                    break;
                case VocabularyIntentService.ACTION_DELETE:
                    if (Constants.STATUS_SUCCEEDED.equals(status)) {
                        Snackbar.make(getActivity().findViewById(android.R.id.content),
                                R.string.dialog_deletion_completed,
                                Snackbar.LENGTH_LONG).show();
                    }
                    break;

                default:
                    throw new IllegalArgumentException("The intent action is invalid!");
            }
        }
    };

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Timber.d(preference.getKey() + " updated");

        // Set summary for list preference
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue((String) newValue);
            String entry = (String) listPreference.getEntries()[prefIndex];
            Timber.d("the daily count entry is " + entry);
            listPreference.setSummary(entry);
        }

        // Set summary for multiselect list preference
        if (preference instanceof MultiSelectListPreference) {
            MultiSelectListPreference multiSelectListPreference = (MultiSelectListPreference) preference;
            Set<String> tagsValue = (Set<String>) newValue;
            StringBuilder summary = new StringBuilder();
            for (String s : tagsValue) {
                int index = multiSelectListPreference.findIndexOfValue(s);
                String entry = (String) multiSelectListPreference.getEntries()[index];
                summary = summary.append(entry).append("; ");
            }
            multiSelectListPreference.setSummary(summary);

            // Download and delete related list
            if (getString(R.string.pref_list_tag_key).equals(preference.getKey())) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                existingTags = sharedPreferences.getStringSet(getString(R.string.pref_list_tag_key), null);

                // Download lists which is not in the existingTags
                for (String tag : tagsValue) {
                    int prefIndex = multiSelectListPreference.findIndexOfValue(tag);

                    Timber.d("new tag: " + tag);
                    if (existingTags == null || existingTags.size() == 0 || !existingTags.contains(tag)) {

                        Timber.d("Starting download " + tag);
                        VocabularyIntentService.startActionDownloadVocabulary(getContext(), tag);
                    }
                }

                // Deleted lists which is in the exsitingTags but not in tags
                if (existingTags != null) {
                    for (String tag : existingTags) {
                        if (tagsValue.size() == 0 || !tagsValue.contains(tag))
                            VocabularyIntentService.startActionDeleteList(getContext(), tag);
                    }
                }
            }
        }
        return true;
    }
}
