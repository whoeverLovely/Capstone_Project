package com.louise.udacity.mydict;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.WindowDecorActionBar;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class VocabularyIntentService extends IntentService {

    public static final String ACTION_DOWNLOAD_VOCABULARIES = "com.louise.udacity.mydict.action.download-vocabularies";

    private static final String EXTRA_LIST_NAME = "com.louise.udacity.mydict.extra.listName";

    public VocabularyIntentService() {
        super("VocabularyIntentService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionDownloadVocabulary(Context context, String listName) {
        Intent intent = new Intent(context, VocabularyIntentService.class);
        intent.setAction(ACTION_DOWNLOAD_VOCABULARIES);
        intent.putExtra(EXTRA_LIST_NAME, listName);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_DOWNLOAD_VOCABULARIES:
                        final String listName = intent.getStringExtra(EXTRA_LIST_NAME);
                        handleActionDownloadVOcabulary(listName);
                        break;

                    default:
                        throw new RuntimeException("The action requested is invalid!");
                }
            }
        }
    }

    private void handleActionDownloadVOcabulary(final String listName) {
        Timber.d("----------------------------download started-------------------------------");

        // Create an instance of FirebaseStorage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        // Create a storage reference from our app
        StorageReference storageRef = storage.getReference();
        // Create a reference with an initial file path and name
        StorageReference pathReference = storageRef.child(listName + "_list");

        // Download to a local file
        String filePath = VocabularyIntentService.this.getFilesDir().getPath() + listName + "_list";
        final File localFile = new File(filePath);
        localFile.deleteOnExit();

        pathReference.getFile(localFile)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        Timber.d("--------------------download succeeded-----------------");
                        List<VocabularyProtos.Vocabulary> vocabularyList = null;
                        try {
                            vocabularyList = VocabularyReader.parseProto(new FileInputStream(localFile));
                        } catch (FileNotFoundException e) {

                            e.printStackTrace();
                        }

                        if (vocabularyList != null) {
                            Timber.d("The size of downloaded vocabulary list " + localFile.getName() + ": "  + vocabularyList.size());

                        }
                        // Local temp file has been created
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Timber.d("--------------------download failed-----------------");
                exception.printStackTrace();
            }
        });
    }
}

