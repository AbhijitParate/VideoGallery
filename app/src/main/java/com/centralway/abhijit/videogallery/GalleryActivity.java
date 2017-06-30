package com.centralway.abhijit.videogallery;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GalleryActivity extends AppCompatActivity
        implements
        LoaderManager.LoaderCallbacks<Cursor>,
        ActionMode.Callback,
        RecyclerView.OnItemTouchListener {

    private static final int LOADER_MANAGER_INIT_ID = 390;
    private static final int PERMISSION_REQUEST_CODE_READ_EXTERNAL_STORAGE = 810;

    @BindView(R.id.rvVideoGallery)
    RecyclerView mRecyclerView;

    private GalleryAdapter galleryAdapter;
    ActionMode actionMode;

    GestureDetectorCompat gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        ButterKnife.bind(this);

        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 3);
        mRecyclerView.setLayoutManager(layoutManager);
        galleryAdapter = new GalleryAdapter(this);
        mRecyclerView.setAdapter(galleryAdapter);

        gestureDetector =
                new GestureDetectorCompat(this, new GestureListener());

        checkPermission();
    }

    private void checkPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE_READ_EXTERNAL_STORAGE);
            } else  {
                getSupportLoaderManager().initLoader(LOADER_MANAGER_INIT_ID, null, this);
            }
        } else {
            getSupportLoaderManager().initLoader(LOADER_MANAGER_INIT_ID, null, this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case PERMISSION_REQUEST_CODE_READ_EXTERNAL_STORAGE:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getSupportLoaderManager().initLoader(LOADER_MANAGER_INIT_ID, null, this);
                } else {
                    Snackbar snackBar = Snackbar.make(findViewById(android.R.id.content), "External Storage permission denied", Snackbar.LENGTH_INDEFINITE);
                    snackBar.setAction("Retry", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            checkPermission();
                        }
                    });
                    snackBar.show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.MEDIA_TYPE
        };

        String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

        String orderBy = MediaStore.Files.FileColumns.DATE_ADDED + " DESC";

        return new CursorLoader(
                this,
                MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                null,
                orderBy
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mRecyclerView.addOnItemTouchListener(this);
        galleryAdapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        galleryAdapter.changeCursor(null);
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        MenuInflater inflater = actionMode.getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_play_all:
                Intent intent = VideoActivity.playList(this, galleryAdapter.getSelectedItems());
                startActivity(intent);
                actionMode.finish();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        this.actionMode = null;
        galleryAdapter.clearSelections();
    }

    void onVideoClick(View view){
        int index = mRecyclerView.getChildAdapterPosition(view);
        if (actionMode != null) {
            toggleSelection(index);
            return;
        }
        Intent i = VideoActivity.playSingle(this, galleryAdapter.getItemAt(index));
        startActivity(i);
    }

    private void toggleSelection(int index) {
        galleryAdapter.toggleSelection(index);
        String title = getString(R.string.selected_count, galleryAdapter.getSelectedItemCount());
        actionMode.setTitle(title);
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        gestureDetector.onTouchEvent(e);
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {

    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            View view = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
            onVideoClick(view);
            return super.onSingleTapConfirmed(e);
        }

        public void onLongPress(MotionEvent e) {
            View view = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
            if (actionMode != null) {
                return;
            }
            actionMode = startActionMode(GalleryActivity.this);
            int index = mRecyclerView.getChildAdapterPosition(view);
            toggleSelection(index);
            super.onLongPress(e);
        }
    }
}