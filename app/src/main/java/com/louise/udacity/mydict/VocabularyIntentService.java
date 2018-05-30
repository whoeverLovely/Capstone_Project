package com.louise.udacity.mydict;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.louise.udacity.lib.VocabularyProtos;
import com.louise.udacity.lib.VocabularyReader;
import com.louise.udacity.mydict.data.Constants;
import com.louise.udacity.mydict.data.VocabularyContract;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import timber.log.Timber;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class VocabularyIntentService extends IntentService {

    public static final String ACTION_DOWNLOAD = "com.louise.udacity.mydict.action.download";
    public static final String ACTION_DOWNLAOD_STATUS = "com.louise.udacity.mydict.action.download-status";

    private static final String EXTRA_TAG = "com.louise.udacity.mydict.extra.tag";
    public static final String EXTRA_DOWNLOAD_STATUS = "com.louise.udacity.mydict.extra.download-status";

    public VocabularyIntentService() {
        super("VocabularyIntentService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionDownloadVocabulary(Context context, String tag) {
        Intent intent = new Intent(context, VocabularyIntentService.class);
        intent.setAction(ACTION_DOWNLOAD);
        intent.putExtra(EXTRA_TAG, tag);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_DOWNLOAD:
                        final String tag = intent.getStringExtra(EXTRA_TAG);
                        handleActionDownload(tag);
                        break;

                    default:
                        throw new RuntimeException("The action requested is invalid!");
                }
            }
        }
    }

    private void handleActionDownload(final String tag) {
        Timber.d("----------------------------download started-------------------------------");

        // Create an instance of FirebaseStorage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        // Create a storage reference from our app
        StorageReference storageRef = storage.getReference();
        // Create a reference with an initial file path and name
        StorageReference pathReference = storageRef.child(tag + "_list");

        // Download to a local file
        String filePath = VocabularyIntentService.this.getFilesDir().getPath() + tag + "_list";
        final File localFile = new File(filePath);
        localFile.deleteOnExit();

        final Intent localIntent = new Intent(ACTION_DOWNLAOD_STATUS);
        pathReference.getFile(localFile)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        Timber.d("--------------------download succeeded-----------------");

                        try {
                            importToDB(new FileInputStream(localFile), tag);

                            // Send LocalBroadcast to notify the task status
                                    localIntent.putExtra(EXTRA_DOWNLOAD_STATUS, Constants.STATUS_SUCCEEDED);
                            // Broadcasts the Intent to receivers in this app.
                            LocalBroadcastManager.getInstance(VocabularyIntentService.this).sendBroadcast(localIntent);

                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }

                        // Local temp file has been created
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Timber.d("--------------------download failed-----------------");
                        localIntent.putExtra(EXTRA_DOWNLOAD_STATUS, Constants.STATUS_FAILED);
                        exception.printStackTrace();
                    }
                });

    }

    private void importToDB(InputStream inputStream, String tag) {
        List<VocabularyProtos.Vocabulary> vocabularyList = VocabularyReader.parseProto(inputStream);
        if (vocabularyList != null) {
            Timber.d("The size of downloaded vocabulary list " + ": " + vocabularyList.size());
        }

        ContentValues[] cvArr = new ContentValues[vocabularyList.size()];
        ContentValues cv;
        VocabularyProtos.Vocabulary vocabulary;

        for (int i = 0; i < vocabularyList.size(); i++) {
            vocabulary = vocabularyList.get(i);
            cv = new ContentValues();
            cv.put(VocabularyContract.VocabularyEntry.COLUMN_WORD, vocabulary.getWord());
            cv.put(VocabularyContract.VocabularyEntry.COLUMN_PHONETIC, vocabulary.getPhonetic());
            cv.put(VocabularyContract.VocabularyEntry.COLUMN_DEFINITION, vocabulary.getDefinition());
            cv.put(VocabularyContract.VocabularyEntry.COLUMN_TRANSLATION, vocabulary.getTranslation());
            cv.put(VocabularyContract.VocabularyEntry.COLUMN_TAG, tag);

            cvArr[i] = cv;
        }
        int numInserted = getContentResolver().bulkInsert(VocabularyContract.VocabularyEntry.CONTENT_URI, cvArr);

        // Delete local file after importing to database
        if (vocabularyList.size() == numInserted) {
            String filePath = VocabularyIntentService.this.getFilesDir().getPath() + tag + "_list";
            File localFile = new File(filePath);
            localFile.deleteOnExit();
        }

        Timber.d("Here is import to DB.");

    }
}

