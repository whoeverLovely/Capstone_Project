package com.louise.udacity.mydict;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.louise.udacity.mydict.data.Constants;

import timber.log.Timber;

public class VocabularySettingFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings_vocabulary);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                new IntentFilter(VocabularyIntentService.ACTION_STATUS));

        Preference prefGre = findPreference(getString(R.string.tag_gre));
        Preference prefToefl = findPreference(getString(R.string.tag_toefl));
        Preference prefIelts = findPreference(getString(R.string.tag_ielts));
        prefGre.setOnPreferenceChangeListener(this);
        prefToefl.setOnPreferenceChangeListener(this);
        prefIelts.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String tag = preference.getKey();
        CheckBoxPreference checkBoxPreference = (CheckBoxPreference)preference;

        if (checkBoxPreference.isChecked()) {
            // Delete the list from local db
            Timber.d("Unchecked preference");
            VocabularyIntentService.startActionDeleteList(getContext(), tag);
            checkBoxPreference.setChecked(false);
        } else {
            // Download the list and import to local db
            Timber.d("Checked preference");
            VocabularyIntentService.startActionDownloadVocabulary(getContext(), tag);
            checkBoxPreference.setChecked(true);
        }
        return false;
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
                }
            }
        }
    };
}
