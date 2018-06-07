package com.louise.udacity.mydict;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    private Context mContenxt;
    private Cursor mCursor;

    public GroupAdapter(Context context) {
        mContenxt = context;
    }

    public void swapData (Cursor cursor) {
        mCursor = cursor;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContenxt).inflate(R.layout.item_group, parent, false);
        return new GroupAdapter.GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        holder.textViewWord.setText(mCursor.getString(1));
        holder.textViewTranslation.setText(mCursor.getString(2));
    }

    @Override
    public int getItemCount() {
        if (mCursor == null)
            return 0;
        return mCursor.getCount();
    }

    class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView textViewWord;
        TextView textViewTranslation;

        public GroupViewHolder(View itemView) {
            super(itemView);

            textViewWord = itemView.findViewById(R.id.textView_word_group_item);
            textViewTranslation = itemView.findViewById(R.id.textView_translation_group_item);
        }
    }
}
