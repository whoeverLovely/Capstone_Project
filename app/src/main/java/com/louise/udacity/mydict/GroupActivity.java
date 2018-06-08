package com.louise.udacity.mydict;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;

import com.louise.udacity.mydict.data.VocabularyContract;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GroupActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_GROUP_ID = 100;
    private String group;
    private GroupAdapter mGroupAdapter;

    @BindView(R.id.recyclerView_group)
    RecyclerView recyclerViewGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);
        ButterKnife.bind(this);

        group = getIntent().getStringExtra(MainActivity.EXTRA_GROUP);

        recyclerViewGroup.setLayoutManager(new LinearLayoutManager(this));
        mGroupAdapter = new GroupAdapter(this);
        recyclerViewGroup.setAdapter(mGroupAdapter);

        if (group != null)
            getSupportLoaderManager().initLoader(LOADER_GROUP_ID, null, this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @NonNull
    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return new CursorLoader(this,
                VocabularyContract.VocabularyEntry.CONTENT_URI,
                new String[]{VocabularyContract.VocabularyEntry.COLUMN_WORD, VocabularyContract.VocabularyEntry.COLUMN_TRANSLATION},
                VocabularyContract.VocabularyEntry.COLUMN_GROUP_NAME + "=?",
                new String[]{group},
                null);
    }

    @Override
    public void onLoadFinished(@NonNull android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        mGroupAdapter.swapData(data);
    }

    @Override
    public void onLoaderReset(@NonNull android.support.v4.content.Loader<Cursor> loader) {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);

    }
}
