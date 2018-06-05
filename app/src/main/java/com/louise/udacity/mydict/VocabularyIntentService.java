package com.louise.udacity.mydict;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.louise.udacity.lib.VocabularyProtos;
import com.louise.udacity.lib.VocabularyReader;
import com.louise.udacity.mydict.data.ClientVocabulary;
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
    public static final String ACTION_UPDATE_STATUS = "com.louise.udacity.mydict.action.update-status";
    public static final String ACTION_UPDATE_REVIEW_LIST = "com.louise.udacity.mydict.action.update-review-list";
    public static final String ACTION_LINK = "com.louise.udacity.mydict.action.link";


    private static final String EXTRA_TAG = "com.louise.udacity.mydict.extra.tag";
    public static final String EXTRA_STATUS_TYPE = "com.louise.udacity.mydict.extra.status-type";
    public static final String EXTRA_STATUS = "com.louise.udacity.mydict.extra.delete-status";
    public static final String EXTRA_VOCABULARY_ID = "com.louise.udacity.mydict.extra.vocab-id";
    public static final String EXTRA_VOCABULARY = "com.louise.udacity.mydict.extra.vocabulary";
    public static final String EXTRA_QUERY = "com.louise.udacity.mydict.extra.query";

    final Intent notifyIntent = new Intent(ACTION_STATUS);

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

    public static void startActionUpdateStatus(Context context, int status, long id) {
        Intent intent = new Intent(context, VocabularyIntentService.class);
        intent.setAction(ACTION_UPDATE_STATUS);
        intent.putExtra(EXTRA_TAG, status);
        intent.putExtra(EXTRA_VOCABULARY_ID, id);
        context.startService(intent);
    }

    public static void startActionUpdateReviewList(Context context) {
        Intent intent = new Intent(context, VocabularyIntentService.class);
        intent.setAction(ACTION_UPDATE_REVIEW_LIST);
        context.startService(intent);
    }

    public static void startActionSearch(Context context, String query) {
        Intent intent = new Intent(context, VocabularyIntentService.class);
        intent.setAction(SearchResultActivity.ACTION_SEARCH);
        intent.putExtra(EXTRA_QUERY, query);
        context.startService(intent);
    }

    public static void startActionLink(Context context, long originalId, @NonNull String originalGroup, ClientVocabulary currentVocabulary) {
        Intent intent = new Intent(context, VocabularyIntentService.class);
        intent.setAction(ACTION_LINK);
        intent.putExtra(SearchResultActivity.EXTRA_ORIGINAL_VOCABULARY_ID, originalId);
        intent.putExtra(SearchResultActivity.EXTRA_ORIGINAL_VOCABULARY_GROUP, originalGroup);
        intent.putExtra(EXTRA_VOCABULARY, currentVocabulary);
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
                        String dailyCountStr = PreferenceManager.getDefaultSharedPreferences(VocabularyIntentService.this)
                                .getString(getString(R.string.pref_daily_count_key), Constants.DEFAULT_DAILY_COUNT);
                        int dailyCount = Integer.parseInt(dailyCountStr);
                        handleActionGenLearnList(tag, dailyCount);
                        break;

                    case ACTION_UPDATE_STATUS:
                        int status = intent.getIntExtra(EXTRA_STATUS, -1);
                        long vocabId = intent.getLongExtra(EXTRA_VOCABULARY_ID, -1);
                        handleActionUpdateStatus(status, vocabId);
                        break;

                    case ACTION_UPDATE_REVIEW_LIST:
                        handleActionUpdateReviewList();
                        break;

                    case SearchResultActivity.ACTION_SEARCH:
                        String query = intent.getStringExtra(EXTRA_QUERY);
                        handleActionSearch(query);
                        break;

                    case ACTION_LINK:
                        long originalId = intent.getLongExtra(SearchResultActivity.EXTRA_ORIGINAL_VOCABULARY_ID, -1);
                        String originalGroup = intent.getStringExtra(SearchResultActivity.EXTRA_ORIGINAL_VOCABULARY_GROUP);
                        ClientVocabulary clientVocabulary = intent.getParcelableExtra(EXTRA_VOCABULARY);
                        handleActionLink(originalId, originalGroup, clientVocabulary);
                        break;

                    default:
                        throw new RuntimeException("The action requested:" + action + "is invalid!");
                }
            }
        }
    }

    private void handleActionLink(long originalId, String originalGroup, ClientVocabulary currentVocabulary) {

        Intent intent = new Intent(SearchResultActivity.ACTION_SEARCH);
        intent.putExtra(EXTRA_STATUS_TYPE, Constants.STATUS_TYPE_LINK);

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
            long existingId = cursor.getLong(1);
            String existingGroup = cursor.getString(2);

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

        if (updatedTotal > 0)
            intent.putExtra(EXTRA_STATUS, Constants.STATUS_SUCCEEDED);
        else
            intent.putExtra(EXTRA_STATUS, Constants.STATUS_FAILED);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        Timber.d(updatedTotal + " vocabularies updated group to " + originalGroup);
    }

    private void handleActionSearch(final String query) {
        Timber.d("Stating search");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("vocabulary").document(query);

        final Intent intent = new Intent(SearchResultActivity.ACTION_SEARCH);
        intent.putExtra(EXTRA_STATUS_TYPE, Constants.STATUS_TYPE_QUERY);


        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    intent.putExtra(EXTRA_STATUS, Constants.STATUS_SUCCEEDED);
                    if (document.exists()) {
                        String phonetic = (String) document.get("phonetic");
                        String translation = (String) document.get("translation");
                        String definition = (String) document.get("definition");
                        ClientVocabulary clientVocabulary = new ClientVocabulary();
                        clientVocabulary.setWord(query);
                        clientVocabulary.setPhonetic(phonetic);
                        clientVocabulary.setTranslation(translation);
                        clientVocabulary.setDefinition(definition);
                        intent.putExtra(EXTRA_VOCABULARY, clientVocabulary);
                        Timber.d("DocumentSnapshot data: " + document.getData());
                    } else {
                        intent.putExtra(EXTRA_VOCABULARY, "Not exists");
                        Timber.d("No such document");
                    }
                } else {
                    intent.putExtra(EXTRA_STATUS, Constants.STATUS_FAILED);
                    Timber.d("get failed with " + task.getException());
                }
                LocalBroadcastManager.getInstance(VocabularyIntentService.this).sendBroadcast(intent);
            }
        });
    }

    private void handleActionUpdateReviewList() {
        LocalDate targetDate = LocalDate.now().minusDays(getResources().getInteger(R.integer.review_interval_days));
        String targetDateStr = targetDate.toString();

        ContentValues cv = new ContentValues();
        cv.put(VocabularyContract.VocabularyEntry.COLUMN_STATUS, VocabularyContract.VocabularyEntry.STATUS_REVIEWING);
        int reviewVocabAdded = getContentResolver().update(VocabularyContract.VocabularyEntry.CONTENT_URI,
                cv,
                VocabularyContract.VocabularyEntry.COLUMN_STATUS + "=? AND "
                        + VocabularyContract.VocabularyEntry.COLUMN_DATE + "<=?",
                new String[]{String.valueOf(VocabularyContract.VocabularyEntry.STATUS_LEARNED), targetDateStr});

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        if (reviewVocabAdded > 0) {
            editor.putString(getString(R.string.pref_review_list_status), Constants.LIST_STATUS_READY);
            Timber.d("------------------------" + reviewVocabAdded + " vocabularies added to review list----------------------");
        } else {
            Timber.d("No new vocabularies added to reviewing list!");

            int reviewNumTotal = VocabularyContentProvider.getVocabularyNumForToday(this, VocabularyContract.VocabularyEntry.STATUS_REVIEWING);
            if (reviewNumTotal > 0)
                editor.putString(getString(R.string.pref_review_list_status), Constants.LIST_STATUS_READY);
            else
                editor.putString(getString(R.string.pref_review_list_status), Constants.LIST_STATUS_EMPTY);
        }

        editor.apply();
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

        notifyIntent.putExtra(EXTRA_STATUS_TYPE, Constants.STATUS_TYPE_DOWNLOAD);
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
                                handleActionGenLearnList(null, dailyCount);

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
        if (numDeleted > 0) {
            Intent localIntent = new Intent(ACTION_STATUS);
            // Send LocalBroadcast to notify the task status
            localIntent.putExtra(EXTRA_STATUS_TYPE, Constants.STATUS_TYPE_DELETE);
            localIntent.putExtra(EXTRA_STATUS, Constants.STATUS_SUCCEEDED);
            // Broadcasts the Intent to receivers in this app.
            LocalBroadcastManager.getInstance(VocabularyIntentService.this).sendBroadcast(localIntent);
        }
    }

    // Update COLUMN_DATE to current date, status to status_learning
    private void handleActionGenLearnList(String tag, int number) {
        LocalDate localDate = LocalDate.now();
        String curentDate = localDate.toString();

        ContentValues cv = new ContentValues();
        cv.put(VocabularyContract.VocabularyEntry.COLUMN_DATE, curentDate);
        cv.put(VocabularyContract.VocabularyEntry.COLUMN_STATUS, VocabularyContract.VocabularyEntry.STATUS_LEARNING);

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

        // Update SharedPreference to initialize MainActivity when it's launched
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        if (itemUpdated > 0) {
            editor.putString(getString(R.string.pref_learn_list_status), Constants.LIST_STATUS_READY);
        } else {
            int learnNumTotal = VocabularyContentProvider.getVocabularyNumForToday(this, VocabularyContract.VocabularyEntry.STATUS_LEARNING);
            if (learnNumTotal > 0)
                editor.putString(getString(R.string.pref_learn_list_status), Constants.LIST_STATUS_READY);
            else
                editor.putString(getString(R.string.pref_learn_list_status), Constants.LIST_STATUS_EMPTY);
        }
        editor.apply();

        Timber.d("Selected vocabularies : " + itemUpdated);
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

