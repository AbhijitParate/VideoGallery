package com.centralway.abhijit.videogallery;

import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public class Utils {

    public static Uri getUriFromMediaStore(Cursor mMediaStoreCursor, int position) {
        int idIndex = mMediaStoreCursor.getColumnIndex(MediaStore.Files.FileColumns._ID);
        mMediaStoreCursor.moveToPosition(position);
        Integer mediaId = mMediaStoreCursor.getInt(idIndex);
        return Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, Integer.toString(mediaId));
    }

    public static String getFileNameFromMediaStore(Cursor mMediaStoreCursor, int position) {
        int displayNameIndex = mMediaStoreCursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME);
        mMediaStoreCursor.moveToPosition(position);
        return mMediaStoreCursor.getString(displayNameIndex);
    }
}
