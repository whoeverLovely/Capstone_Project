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
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;

import com.louise.udacity.mydict.data.Constants;

import java.util.Set;

import timber.log.Timber;

public class VocabularySettingFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {

    CheckBoxPreference checkBoxPreference;
    Set<String> existingTags;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                new IntentFilter(VocabularyIntentService.ACTION_STATUS));

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        existingTags = sharedPreferences.getStringSet(getString(R.string.pref_list_tag_key), null);

    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());

            String statusType = intent.getStringExtra(VocabularyIntentService.EXTRA_STATUS_TYPE);
            String status = intent.getStringExtra(VocabularyIntentService.EXTRA_STATUS);

            if (Constants.STATUS_TYPE_DOWNLOAD.equals(statusType)) {
                if (Constants.STATUS_SUCCEEDED.equals(status)) {
                    dialogBuilder.setMessage(R.string.dialog_download_complete)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                    /*mProgressBar.setVisibility(View.GONE);*/
                    checkBoxPreference.setChecked(true);
                }
            } else if (Constants.STATUS_TYPE_DELETE.equals(statusType)) {
                if (Constants.STATUS_SUCCEEDED.equals(status)) {
                    dialogBuilder.setMessage(R.string.dialog_deletion_completed)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                    /*mProgressBar.setVisibility(View.GONE);*/
                    checkBoxPreference.setChecked(false);
                }
            }
        }
    };

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);
        String prefListTagKey = getString(R.string.pref_list_tag_key);
        if (prefListTagKey.equals(preference.getKey())) {
            Set<String> tags = ((MultiSelectListPreference) preference).getValues();

            // Download lists which is not in the existingTags
            for (String tag : tags) {
                if (existingTags != null && existingTags.contains(tag)) {
                    return;
                } else {
                    VocabularyIntentService.startActionDownloadVocabulary(getContext(), tag);
                }
            }

            // Deleted lists which is in the exsitingTags but not in tags
            for (String tag : existingTags) {
                if (!tags.contains(tag))
                    VocabularyIntentService.startActionDeleteList(getContext(), tag);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }
}
