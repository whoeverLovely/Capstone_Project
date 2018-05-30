package com.louise.udacity.mydict.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.louise.udacity.lib.VocabularyProtos;


public class VocabularyContentProvider extends ContentProvider {

    public static final int VOCABULARY = 100;
    public static final int VOCABULARY_WITH_WORD = 101;
    public static final int VOCABULARY_WITH_TAG = 102;
    public static final int VOCABULARY_WITH_ID = 103;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    public static UriMatcher buildUriMatcher() {

        // Initialize a UriMatcher with no matches by passing in NO_MATCH to the constructor
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        uriMatcher.addURI(VocabularyContract.AUTHORITY, VocabularyContract.PATH_VOCABULARY, VOCABULARY);
        uriMatcher.addURI(VocabularyContract.AUTHORITY, VocabularyContract.PATH_VOCABULARY + "/word/*", VOCABULARY_WITH_WORD);
        uriMatcher.addURI(VocabularyContract.AUTHORITY, VocabularyContract.PATH_VOCABULARY + "/tag/*", VOCABULARY_WITH_TAG);
        uriMatcher.addURI(VocabularyContract.AUTHORITY, VocabularyContract.PATH_VOCABULARY + "/#", VOCABULARY_WITH_ID);

        return uriMatcher;
    }

    public static Uri buildVocabularyUriWithWord(String word) {
        return VocabularyContract.VocabularyEntry.CONTENT_URI.buildUpon().appendPath("word").appendPath(word).build();
    }

    public static Uri buildVocabularyUriWithTag(String tag) {
        return VocabularyContract.VocabularyEntry.CONTENT_URI.buildUpon().appendPath("tag").appendPath(tag).build();
    }

    private VocabularyDBHelper mVocabularyHelper;

    @Override
    public boolean onCreate() {

        Context context = getContext();
        mVocabularyHelper = new VocabularyDBHelper(context);
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        // Get access to underlying database (read-only for query)
        final SQLiteDatabase db = mVocabularyHelper.getReadableDatabase();

        // Write URI match code and set a variable to return a Cursor
        int match = sUriMatcher.match(uri);
        Cursor retCursor;

        switch (match) {
            case VOCABULARY:
                retCursor = db.query(VocabularyContract.VocabularyEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;

            case VOCABULARY_WITH_WORD:
                retCursor = db.query(VocabularyContract.VocabularyEntry.TABLE_NAME,
                        projection,
                        VocabularyContract.VocabularyEntry.COLUMN_WORD + "=?",
                        new String[]{uri.getLastPathSegment()},
                        null,
                        null,
                        sortOrder);
                break;

            case VOCABULARY_WITH_ID:
                retCursor = db.query(VocabularyContract.VocabularyEntry.TABLE_NAME,
                        projection,
                        VocabularyContract.VocabularyEntry._ID + "=?",
                        new String[]{uri.getLastPathSegment()},
                        null,
                        null,
                        sortOrder);
                break;

            // Default exception
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Set a notification URI on the Cursor and return that Cursor
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the desired Cursor
        return retCursor;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {

        final SQLiteDatabase db = mVocabularyHelper.getWritableDatabase();

        // Write URI matching code to identify the match for the plants directory
        int match = sUriMatcher.match(uri);
        Uri returnUri; // URI to be returned
        switch (match) {
            case VOCABULARY:
                // Insert new values into the database
                long id = db.insert(VocabularyContract.VocabularyEntry.TABLE_NAME, null, values);
                if (id > 0) {
                    returnUri = ContentUris.withAppendedId(VocabularyContract.VocabularyEntry.CONTENT_URI, id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            // Default case throws an UnsupportedOperationException
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Notify the resolver if the uri has been changed, and return the newly inserted URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Return constructed uri (this points to the newly inserted row of data)
        return returnUri;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {

        final SQLiteDatabase db = mVocabularyHelper.getWritableDatabase();

        int numInserted = 0;
        int match = sUriMatcher.match(uri);

        switch (match) {
            case VOCABULARY:
                db.beginTransaction();

                for (ContentValues cv : values) {
                    long newID = db.insertOrThrow(VocabularyContract.VocabularyEntry.TABLE_NAME,
                            null,
                            cv);

                    if (newID <= 0)
                        throw new SQLException("Failed to insert row into " + uri);

                }
                db.setTransactionSuccessful();
                numInserted = values.length;
                db.endTransaction();

                break;

            // Default case throws an UnsupportedOperationException
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);

        return numInserted;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        // Get access to the database and write URI matching code to recognize a single item
        final SQLiteDatabase db = mVocabularyHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);

        // Keep track the number of deleted vocabulary
        int vocabularyDeleted; // starts as 0
        switch (match) {
            case VOCABULARY_WITH_TAG:
                vocabularyDeleted = db.delete(VocabularyContract.VocabularyEntry.TABLE_NAME,
                        VocabularyContract.VocabularyEntry.COLUMN_TAG + "=?",
                        new String[]{uri.getLastPathSegment()});
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Notify the resolver of a change and return the number of items deleted
        if (vocabularyDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        // Return the number of plant deleted
        return vocabularyDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }
}
