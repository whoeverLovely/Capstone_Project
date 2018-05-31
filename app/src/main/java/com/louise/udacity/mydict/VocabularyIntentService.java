package com.louise.udacity.mydict;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.louise.udacity.lib.VocabularyProtos;
import com.louise.udacity.lib.VocabularyReader;
import com.louise.udacity.mydict.data.Constants;
import com.louise.udacity.mydict.data.VocabularyContentProvider;
import com.louise.udacity.mydict.data.VocabularyContract;

import org.joda.time.LocalDate;

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
    public static final String ACTION_STATUS = "com.louise.udacity.mydict.action.status";
    public static final String ACTION_DELETE_LIST = "com.louise.udacity.mydict.action.delete-list";
    public static final String ACTION_GENERATE_LEARN_LIST = "com.louise.udacity.mydict.action.list";

    private static final String EXTRA_TAG = "com.louise.udacity.mydict.extra.tag";
    public static final String EXTRA_STATUS_TYPE = "com.louise.udacity.mydict.extra.status-type";
    public static final String EXTRA_STATUS = "com.louise.udacity.mydict.extra.delete-status";
    public static final String EXTRA_LIST_TYPE = "com.louise.udacity.mydict.action.list-type";

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

    public static void startActionDeleteList(Context context, String tag) {
        Intent intent = new Intent(context, VocabularyIntentService.class);
        intent.setAction(ACTION_DELETE_LIST);
        intent.putExtra(EXTRA_TAG, tag);
        context.startService(intent);
    }

    public static void startActionGenLearnList(Context context, @Nullable String tag) {
        Intent intent = new Intent(context, VocabularyIntentService.class);
        intent.setAction(ACTION_GENERATE_LEARN_LIST);
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
                        final String downloadTag = intent.getStringExtra(EXTRA_TAG);
                        handleActionDownload(downloadTag);
                        break;

                    case ACTION_DELETE_LIST:
                        final String deleteTag = intent.getStringExtra(EXTRA_TAG);
                        handleActionDelete(deleteTag);
                        break;

                    case ACTION_GENERATE_LEARN_LIST:
                        final String tag = intent.getStringExtra(EXTRA_TAG);
                        // TODO read number form shared preference
                        handleActionGenLearnList(tag, 20);
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

        final Intent localIntent = new Intent(ACTION_STATUS);
        localIntent.putExtra(EXTRA_STATUS_TYPE, Constants.STATUS_TYPE_DOWNLOAD);
        pathReference.getFile(localFile)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        Timber.d("--------------------download succeeded-----------------");

                        try {
                            importToDB(new FileInputStream(localFile), tag);

                            // Send LocalBroadcast to notify the task status
                            localIntent.putExtra(EXTRA_STATUS, Constants.STATUS_SUCCEEDED);
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
                        localIntent.putExtra(EXTRA_STATUS, Constants.STATUS_FAILED);
                        // Broadcasts the Intent to receivers in this app.
                        LocalBroadcastManager.getInstance(VocabularyIntentService.this).sendBroadcast(localIntent);
                        exception.printStackTrace();
                    }
                });

    }

    private void handleActionDelete(String deleteTag) {
        Uri uri = VocabularyContentProvider.buildVocabularyUriWithTag(deleteTag);
        int numDeleted = getContentResolver().delete(uri, null, null);

        if (numDeleted > 0) {
            Intent localIntent = new Intent(ACTION_STATUS);
            // Send LocalBroadcast to notify the task status
            localIntent.putExtra(EXTRA_STATUS_TYPE, Constants.STATUS_TYPE_DELETE);
            localIntent.putExtra(EXTRA_STATUS, Constants.STATUS_SUCCEEDED);
            // Broadcasts the Intent to receivers in this app.
            LocalBroadcastManager.getInstance(VocabularyIntentService.this).sendBroadcast(localIntent);

        }
    }

    // Update COLUMN_DATE to current date
    private void handleActionGenLearnList(String tag, int number) {
        LocalDate localDate = LocalDate.now();
        String curentDate = localDate.toString();

        ContentValues cv = new ContentValues();
        cv.put(VocabularyContract.VocabularyEntry.COLUMN_DATE, curentDate);

        String sqlWhere;
        if (tag == null)
            // randomly select from the table which status is 0
            sqlWhere = VocabularyContract.VocabularyEntry._ID + " IN (SELECT " + VocabularyContract.VocabularyEntry._ID
                    + " FROM " + VocabularyContract.VocabularyEntry.TABLE_NAME
                    + " WHERE " + VocabularyContract.VocabularyEntry.COLUMN_STATUS + "=" + 0
                    + " AND " + VocabularyContract.VocabularyEntry.COLUMN_DATE + " is null "
                    + " ORDER BY RANDOM() LIMIT "
                    + number + ")";

        else
            sqlWhere = VocabularyContract.VocabularyEntry._ID + " IN (SELECT " + VocabularyContract.VocabularyEntry._ID
                    + " FROM " + VocabularyContract.VocabularyEntry.TABLE_NAME
                    + " WHERE " + VocabularyContract.VocabularyEntry.COLUMN_STATUS + "=" + 0
                    + " AND " + VocabularyContract.VocabularyEntry.COLUMN_TAG + "=" + tag
                    + " AND " + VocabularyContract.VocabularyEntry.COLUMN_DATE + " is null "
                    + " ORDER BY RANDOM() LIMIT "
                    + number + ")";

        int itemUpdated = getContentResolver()
                .update(VocabularyContract.VocabularyEntry.CONTENT_URI,
                        cv,
                        sqlWhere,
                        null);

        Timber.d("Selected vocabularies : " + itemUpdated);
    }

    private void handleActionConvertReview() {

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

