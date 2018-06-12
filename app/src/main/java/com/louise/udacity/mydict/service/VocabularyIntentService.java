package com.louise.udacity.mydict.service;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.louise.udacity.lib.VocabularyProtos;
import com.louise.udacity.lib.VocabularyReader;
import com.louise.udacity.mydict.R;
import com.louise.udacity.mydict.data.ClientVocabulary;
import com.louise.udacity.mydict.data.Constants;
import com.louise.udacity.mydict.data.VocabularyContentProvider;
import com.louise.udacity.mydict.data.VocabularyContract;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import timber.log.Timber;

public class VocabularyIntentService extends IntentService {

    public static final String ACTION_DOWNLOAD = "com.louise.udacity.mydict.action.download";
    public static final String ACTION_STATUS = "com.louise.udacity.mydict.action.status";
    public static final String ACTION_DELETE = "com.louise.udacity.mydict.action.delete";
    public static final String ACTION_GENERATE_LEARN_LIST = "com.louise.udacity.mydict.action.list";
    public static final String ACTION_UPDATE_STATUS = "com.louise.udacity.mydict.action.update-status";
    public static final String ACTION_UPDATE_REVIEW_LIST = "com.louise.udacity.mydict.action.update-review-list";
    public static final String ACTION_LINK = "com.louise.udacity.mydict.action.link";
    public static final String ACTION_SEARCH = "com.louise.udacity.mydict.action.search";


    private static final String EXTRA_TAG = "com.louise.udacity.mydict.extra.tag";
    public static final String EXTRA_STATUS = "com.louise.udacity.mydict.extra.delete-status";
    public static final String EXTRA_VOCABULARY_ID = "com.louise.udacity.mydict.extra.vocab-id";
    public static final String EXTRA_VOCABULARY = "com.louise.udacity.mydict.extra.vocabulary";
    public static final String EXTRA_QUERY = "com.louise.udacity.mydict.extra.query";
    public static final String EXTRA_GROUP = "com.louise.udacity.mydict.extra.group";
    public static final String EXTRA_ORIGINAL_VOCABULARY_ID = "com.louise.udacity.mydict.extra.original-vocabulary-id";
    public static final String EXTRA_ORIGINAL_VOCABULARY_GROUP = "com.louise.udacity.mydict.extra.original-vocabulary-group";

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
        intent.setAction(ACTION_DELETE);
        intent.putExtra(EXTRA_TAG, tag);
        context.startService(intent);
    }

    public static void startActionUpdateStatus(Context context, int status, long id) {
        Intent intent = new Intent(context, VocabularyIntentService.class);
        intent.setAction(ACTION_UPDATE_STATUS);
        intent.putExtra(EXTRA_TAG, status);
        intent.putExtra(EXTRA_VOCABULARY_ID, id);
        context.startService(intent);
    }

    public static void startActionSearch(Context context, String query) {
        Intent intent = new Intent(context, VocabularyIntentService.class);
        intent.setAction(ACTION_SEARCH);
        intent.putExtra(EXTRA_QUERY, query);
        context.startService(intent);
    }

    public static void startActionLink(Context context, ClientVocabulary currentVocabulary, long originalVocabularyId, String originalGroup) {
        Intent intent = new Intent(context, VocabularyIntentService.class);
        intent.setAction(ACTION_LINK);
        intent.putExtra(EXTRA_VOCABULARY, currentVocabulary);
        intent.putExtra(EXTRA_VOCABULARY_ID, originalVocabularyId);
        intent.putExtra(EXTRA_GROUP, originalGroup);
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

                    case ACTION_DELETE:
                        final String deleteTag = intent.getStringExtra(EXTRA_TAG);
                        handleActionDelete(deleteTag);
                        break;

                    case ACTION_UPDATE_STATUS:
                        int status = intent.getIntExtra(EXTRA_STATUS, -1);
                        long vocabId = intent.getLongExtra(EXTRA_VOCABULARY_ID, -1);
                        handleActionUpdateStatus(status, vocabId);
                        break;

                    case ACTION_SEARCH:
                        String query = intent.getStringExtra(EXTRA_QUERY);
                        handleActionSearch(query);
                        break;

                    case ACTION_LINK:
                        ClientVocabulary clientVocabulary = intent.getParcelableExtra(EXTRA_VOCABULARY);
                        long originalId = intent.getLongExtra(EXTRA_VOCABULARY_ID, -1);
                        String originalGroup = intent.getStringExtra(EXTRA_GROUP);
                        handleActionLink(clientVocabulary, originalId, originalGroup);
                        break;

                    default:
                        throw new RuntimeException("The action requested:" + action + "is invalid!");
                }
            }
        }
    }

    private void handleActionLink(ClientVocabulary currentVocabulary, long originalId, String originalGroup) {

        Timber.d("group name received in intentservice is " + originalGroup);

        // Check if current vocabulary exists in db
        Cursor cursor = getContentResolver().query(VocabularyContentProvider.buildVocabularyUriWithWord(currentVocabulary.getWord()),
                new String[]{VocabularyContract.VocabularyEntry._ID, VocabularyContract.VocabularyEntry.COLUMN_GROUP_NAME},
                null,
                null,
                null);

        ContentValues cvUpdate = new ContentValues();
        cvUpdate.put(VocabularyContract.VocabularyEntry.COLUMN_GROUP_NAME, originalGroup);

        // Update original vocabulary
        int numUpdatedExisting = getContentResolver().update(VocabularyContract.VocabularyEntry.CONTENT_URI,
                cvUpdate,
                VocabularyContract.VocabularyEntry._ID + "=?",
                new String[]{String.valueOf(originalId)});
        int numUpdatedCurrent = 0;

        // Current vocabulary doesn't exist in db
        if (cursor.getCount() < 1) {
            // Insert currentVocabulary to db
            ContentValues cvInsert = new ContentValues();
            cvInsert.put(VocabularyContract.VocabularyEntry.COLUMN_WORD, currentVocabulary.getWord());
            cvInsert.put(VocabularyContract.VocabularyEntry.COLUMN_PHONETIC, currentVocabulary.getPhonetic());
            cvInsert.put(VocabularyContract.VocabularyEntry.COLUMN_TRANSLATION, currentVocabulary.getTranslation());
            cvInsert.put(VocabularyContract.VocabularyEntry.COLUMN_DEFINITION, currentVocabulary.getDefinition());
            cvInsert.put(VocabularyContract.VocabularyEntry.COLUMN_GROUP_NAME, originalGroup);
            cvInsert.put(VocabularyContract.VocabularyEntry.COLUMN_TAG, Constants.TAG_LINK);
            getContentResolver().insert(VocabularyContract.VocabularyEntry.CONTENT_URI,
                    cvInsert);

            numUpdatedCurrent = 1;
        }
        // Current vocabulary does exist in db
        else {
            cursor.moveToFirst();
            long existingId = cursor.getLong(0);
            String existingGroup = cursor.getString(1);

            if (existingGroup != null || existingGroup != "") {
                numUpdatedCurrent = getContentResolver().update(VocabularyContract.VocabularyEntry.CONTENT_URI,
                        cvUpdate,
                        VocabularyContract.VocabularyEntry.COLUMN_GROUP_NAME + "=?",
                        new String[]{existingGroup});
            } else {
                numUpdatedCurrent = getContentResolver().update(ContentUris.withAppendedId(VocabularyContract.VocabularyEntry.CONTENT_URI, existingId),
                        cvUpdate,
                        null,
                        null);
            }
        }

        int updatedTotal = numUpdatedExisting + numUpdatedCurrent;

        Intent intent = new Intent(ACTION_LINK);
        if (updatedTotal > 0)
            intent.putExtra(EXTRA_STATUS, Constants.STATUS_SUCCEEDED);
        else
            intent.putExtra(EXTRA_STATUS, Constants.STATUS_FAILED);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        Timber.d(updatedTotal + " vocabularies updated group to " + originalGroup);
    }

    private void handleActionSearch(final String query) {
        
        final Intent intent = new Intent(ACTION_SEARCH);

        DatabaseReference vocabularyRef = FirebaseDatabase.getInstance().getReference("vocabulary").child(query);
        ValueEventListener vocabularyListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
                ClientVocabulary vocabulary = dataSnapshot.getValue(ClientVocabulary.class);
                intent.putExtra(EXTRA_VOCABULARY, vocabulary);
                intent.putExtra(EXTRA_STATUS, Constants.STATUS_SUCCEEDED);

                if (vocabulary != null)
                    Timber.d("the search result word: " + vocabulary.getWord());
                else
                    Timber.d("the query result is empty.");

                LocalBroadcastManager.getInstance(VocabularyIntentService.this).sendBroadcast(intent);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Timber.d("loadPost:onCancelled" + databaseError.toException());
                // ...

                intent.putExtra(EXTRA_STATUS, Constants.STATUS_FAILED);
                LocalBroadcastManager.getInstance(VocabularyIntentService.this).sendBroadcast(intent);
            }
        };
        vocabularyRef.addListenerForSingleValueEvent(vocabularyListener);
    }


    private void handleActionUpdateStatus(int status, long vocabId) {
        ContentValues cv = new ContentValues();
        cv.put(VocabularyContract.VocabularyEntry.COLUMN_STATUS, status);
        getContentResolver().update(ContentUris.withAppendedId(VocabularyContract.VocabularyEntry.CONTENT_URI, vocabId),
                cv,
                null,
                null);
    }

    private void handleActionDownload(final String tag) {

        // Create an instance of FirebaseStorage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        // Create a storage reference from our app
        StorageReference storageRef = storage.getReference();
        // Create a reference with an initial file path and name
        StorageReference pathReference = storageRef.child(tag + "_list");

        Timber.d("----------------------------download started: " + tag + "_list" + "-------------------------------");

        // Download to a local file
        String filePath = VocabularyIntentService.this.getFilesDir().getPath() + tag + "_list";
        final File localFile = new File(filePath);
        localFile.deleteOnExit();

        final Intent notifyIntent = new Intent(ACTION_DOWNLOAD);
        pathReference.getFile(localFile)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        Timber.d("--------------------download succeeded-----------------");

                        try {
                            importToDB(new FileInputStream(localFile), tag);

                            // Send LocalBroadcast to notify the task status
                            notifyIntent.putExtra(EXTRA_STATUS, Constants.STATUS_SUCCEEDED);
                            // Broadcasts the Intent to receivers in this app.
                            LocalBroadcastManager.getInstance(VocabularyIntentService.this).sendBroadcast(notifyIntent);

                            // Only if vocabularies for today is less than the number user set, generate new learning list
                            int num = VocabularyContentProvider.getVocabularyNumForToday(VocabularyIntentService.this,
                                    VocabularyContract.VocabularyEntry.STATUS_LEARNING);

                            String dailyCountStr = PreferenceManager.getDefaultSharedPreferences(VocabularyIntentService.this)
                                    .getString(getString(R.string.pref_daily_count_key), Constants.DEFAULT_DAILY_COUNT);
                            Timber.d("dailyCount retrieved from sharedPreference is " + dailyCountStr);
                            int dailyCount = Integer.parseInt(dailyCountStr);
                            if (num <= dailyCount)
                                MyJobService.handleActionGenLearnList(dailyCount, getApplicationContext());

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
                        notifyIntent.putExtra(EXTRA_STATUS, Constants.STATUS_FAILED);
                        // Broadcasts the Intent to receivers in this app.
                        LocalBroadcastManager.getInstance(VocabularyIntentService.this).sendBroadcast(notifyIntent);
                        exception.printStackTrace();
                    }
                });

    }

    private void handleActionDelete(String deleteTag) {
        Uri uri = VocabularyContentProvider.buildVocabularyUriWithTag(deleteTag);
        int numDeleted = getContentResolver().delete(uri, null, null);
        Timber.d("--------------------deleted " + numDeleted + " entries from " + deleteTag + "-----------------");

        Intent notifyIntent = new Intent(ACTION_DELETE);
        if (numDeleted > 0)
            // Send LocalBroadcast to notify the task status
            notifyIntent.putExtra(EXTRA_STATUS, Constants.STATUS_SUCCEEDED);
        else
            notifyIntent.putExtra(EXTRA_STATUS, Constants.STATUS_FAILED);

        // Broadcasts the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(VocabularyIntentService.this).sendBroadcast(notifyIntent);
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

    }
}
