package com.centralway.abhijit.videogallery;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ThumbViewHolder> {

    private SparseBooleanArray selectedItems;

    private Cursor mMediaStoreCursor;
    private Context mContext;

    GalleryAdapter(Context context) {
        this.mContext = context;
        selectedItems = new SparseBooleanArray();
    }

    @Override
    public ThumbViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View thumbView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_video_thumbnail, parent, false);
        return new ThumbViewHolder(thumbView);
    }

    @Override
    public void onBindViewHolder(ThumbViewHolder holder, int position) {
        Glide.with(mContext)
                .load(Utils.getUriFromMediaStore(mMediaStoreCursor, position))
                .centerCrop()
                .override(96, 96)
                .into(holder.ivThumb);

        holder.setFileName(Utils.getFileNameFromMediaStore(mMediaStoreCursor, position));
        holder.itemView.setActivated(selectedItems.get(position, false));
    }

    @Override
    public int getItemCount() {
        return (mMediaStoreCursor == null) ? 0 : mMediaStoreCursor.getCount();
    }

    void toggleSelection(int position) {
        if (selectedItems.get(position, false)) {
            selectedItems.delete(position);
        }
        else {
            selectedItems.put(position, true);
        }
        notifyItemChanged(position);
    }

    void clearSelections() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    int getSelectedItemCount() {
        return selectedItems.size();
    }

    private Cursor swapCursor(Cursor cursor) {
        if (mMediaStoreCursor == cursor) {
            return null;
        }
        Cursor oldCursor = mMediaStoreCursor;
        this.mMediaStoreCursor = cursor;
        if (cursor != null) {
            this.notifyDataSetChanged();
        }
        return oldCursor;
    }

    void changeCursor(Cursor cursor) {
        Cursor oldCursor = swapCursor(cursor);
        if (oldCursor != null) {
            oldCursor.close();
        }
    }

    Uri getItemAt(int index) {
        return Utils.getUriFromMediaStore(mMediaStoreCursor, index);
    }

    ArrayList<String> getSelectedItems() {
        ArrayList<String> items = new ArrayList<>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(getItemAt(selectedItems.keyAt(i)).toString());
        }
        return items;
    }

    class ThumbViewHolder extends RecyclerView.ViewHolder{

        @BindView(R.id.tv_filename)
        TextView tvFileName;

        @BindView(R.id.iv_thumb)
        ImageView ivThumb;

        ThumbViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void setFileName(String fileName) {
            this.tvFileName.setText(fileName);
        }
    }
}