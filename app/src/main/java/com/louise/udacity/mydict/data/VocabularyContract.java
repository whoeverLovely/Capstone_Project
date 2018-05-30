package com.louise.udacity.mydict.data;

import android.net.Uri;
import android.provider.BaseColumns;

public class VocabularyContract {
    // The authority, which is how your code knows which Content Provider to access
    public static final String AUTHORITY = "com.louise.udacity.mydict";

    // The base content URI = "content://" + <authority>
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    // Define the possible paths for accessing data in this contract
    public static final String PATH_VOCABULARY = "vocabulary";
    public static final String PATH_GROUP = "group";

    public static final long INVALID_VOCABULARY_ID = -1;
    public static final long INVALID_GROUP_ID = -1;

    public static final class VocabularyEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_VOCABULARY).build();

        // Newly imported to database
        public static final int STATUS_NEW = 0;
        // After showing up in learning list
        public static final int STATUS_LEARNED = 1;
        // Need to be reviewed
        public static final int STATUS_REVIEWING = 2;
        // Will never show up in review list
        public static final int STATUS_ARCHIVE = 3;

        public static final String TABLE_NAME = "vocabulary";
        public static final String COLUMN_WORD = "word";
        public static final String COLUMN_PHONETIC = "phonetic";
        public static final String COLUMN_DEFINITION = "definition";
        public static final String COLUMN_TRANSLATION = "translation";
        public static final String COLUMN_TAG = "tag";
        public static final String COLUMN_STATUS = "status";
        public static final String COLUMN_GROUP_ID = "groupId";

    }

    public static final class GroupEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_GROUP).build();

        public static final String TABLE_NAME = "group";
        // Define the first vocabulary as the key and mark all vocabularies which linked to it with the same group id
        public static final String COLUMN_key = "key";

    }
}
