package com.louise.udacity.mydict.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.louise.udacity.mydict.R;
import com.louise.udacity.mydict.data.VocabularyContract;


public class MyWidgetRemoteViewFactory implements RemoteViewsService.RemoteViewsFactory {
    Context mContext;
    Cursor mCursor;

    public MyWidgetRemoteViewFactory(Context context, Intent intent) {
        mContext = context;
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {
        final long identityToken = Binder.clearCallingIdentity();
        mCursor = mContext.getContentResolver().query(VocabularyContract.VocabularyEntry.CONTENT_URI,
                new String[]{VocabularyContract.VocabularyEntry.COLUMN_WORD, VocabularyContract.VocabularyEntry.COLUMN_TRANSLATION},
                VocabularyContract.VocabularyEntry.COLUMN_STATUS + "=?",
                new String[]{String.valueOf(VocabularyContract.VocabularyEntry.STATUS_LEARNING)},
                null);
        Binder.restoreCallingIdentity(identityToken);
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        if (mCursor == null)
            return 0;
        return mCursor.getCount();
    }

    @Override
    public RemoteViews getViewAt(int position) {

        mCursor.moveToPosition(position);
        RemoteViews itemRemoteViews = new RemoteViews(mContext.getPackageName(), R.layout.collection_widget_list_item);
        itemRemoteViews.setTextViewText(R.id.widgetItem, mCursor.getString(0));
        return itemRemoteViews;

    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }
}
