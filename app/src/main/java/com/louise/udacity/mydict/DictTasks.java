package com.louise.udacity.mydict;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.louise.udacity.lib.VocabularyProtos;
import com.louise.udacity.lib.VocabularyReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import timber.log.Timber;

public class DictTasks {
    public static final String ACTION_DOWNLOAD_VOCABULARIES = "download-vocabularies";

    public static void executeTask(Context context, Intent intent) {

        String action = intent.getAction();

        switch (action) {
            case ACTION_DOWNLOAD_VOCABULARIES:
                String listName = intent.getStringExtra("listName");
                downloadVocabularies(listName);
                break;

            default:
                throw new RuntimeException("The task action is invalid.");

        }

    }

    private static void downloadVocabularies(String listName) {
        // Create an instance of FirebaseStorage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        // Create a storage reference from our app
        StorageReference storageRef = storage.getReference();
        // Create a reference with an initial file path and name
        StorageReference pathReference = storageRef.child(listName + "_list");

        // Download to a local file
        final File localFile = new File(listName + "_list");

        pathReference.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                List<VocabularyProtos.Vocabulary> vocabularyList = null;
                try {
                    vocabularyList = VocabularyReader.parseProto(new FileInputStream(localFile));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                if (vocabularyList != null) {
                    Timber.d("The size of downloaded vocabulary list is " + vocabularyList.size());

                }
                // Local temp file has been created
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });

    }
}
