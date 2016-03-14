package catchla.yep.fragment;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import catchla.yep.R;
import catchla.yep.adapter.BaseRecyclerViewAdapter;

/**
 * Created by mariotaku on 16/3/14.
 */
public class ChatMediaBottomSheetDialogFragment extends BottomSheetDialogFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final int REQUEST_REQUEST_STORAGE_PERMISSION = 101;
    private GalleryAdapter mGalleryAdapter;
    private RecyclerView mMediaGallery;

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Context context = getContext();
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        dialog.setContentView(R.layout.dialog_bottom_sheet_chat_media);
        mGalleryAdapter = new GalleryAdapter(context);
        mMediaGallery = (RecyclerView) dialog.findViewById(R.id.media_gallery);
        assert mMediaGallery != null;
        final LinearLayoutManager layout = new LinearLayoutManager(context);
        layout.setOrientation(LinearLayoutManager.HORIZONTAL);
        mMediaGallery.setLayoutManager(layout);
        mMediaGallery.setAdapter(mGalleryAdapter);
        final String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
        requestPermissions(permissions, REQUEST_REQUEST_STORAGE_PERMISSION);
        return dialog;
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        switch (requestCode) {
            case REQUEST_REQUEST_STORAGE_PERMISSION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLoaderManager().initLoader(0, null, this);
                } else {
                    // TODO show error
                }
                return;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        final Uri uri = Images.Media.EXTERNAL_CONTENT_URI;
        final String sortOrder = Images.Media.DATE_ADDED + " DESC";
        final String[] projection = {Images.Media._ID};
        return new CursorLoader(getContext(), uri, projection, null, null, sortOrder);
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
        mGalleryAdapter.setCursor(data);
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {
        mGalleryAdapter.setCursor(null);
    }

    static class GalleryAdapter extends BaseRecyclerViewAdapter<RecyclerView.ViewHolder> {

        private static final int VIEW_TYPE_CAMERA_ENTRY = 1;
        private static final int VIEW_TYPE_MEDIA_ITEM = 2;
        private final LayoutInflater mInflater;
        private Cursor mCursor;

        public GalleryAdapter(Context context) {
            super(context);
            mInflater = LayoutInflater.from(context);
        }

        public void setCursor(Cursor cursor) {
            mCursor = cursor;
            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
            switch (viewType) {
                case VIEW_TYPE_CAMERA_ENTRY: {
                    return new CameraEntryViewHolder(this, mInflater.inflate(R.layout.adapter_item_topic_media_item, parent, false));
                }
                case VIEW_TYPE_MEDIA_ITEM: {
                    return new GalleryViewHolder(this, mInflater.inflate(R.layout.adapter_item_topic_media_item, parent, false));
                }
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public int getItemViewType(final int position) {
            if (position == 0) return VIEW_TYPE_CAMERA_ENTRY;
            return VIEW_TYPE_MEDIA_ITEM;
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
            switch (getItemViewType(position)) {
                case VIEW_TYPE_CAMERA_ENTRY: {
                    break;
                }
                case VIEW_TYPE_MEDIA_ITEM: {
                    mCursor.moveToPosition(position - 1);
                    ((GalleryViewHolder) holder).display(mCursor.getLong(0));
                    break;
                }
            }
        }

        @Override
        public int getItemCount() {
            if (mCursor == null) return 1;
            return mCursor.getCount() + 1;
        }
    }

    static class GalleryViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageView;
        private final GalleryAdapter adapter;

        public GalleryViewHolder(GalleryAdapter adapter, final View itemView) {
            super(itemView);
            this.adapter = adapter;
            itemView.findViewById(R.id.media_remove).setVisibility(View.GONE);
            imageView = (ImageView) itemView.findViewById(R.id.media_preview);
        }

        public void display(final long id) {
            adapter.getImageLoader().displayImage("media-thumb://" + id, imageView);
        }
    }

    static class CameraEntryViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageView;
        private final GalleryAdapter adapter;

        public CameraEntryViewHolder(GalleryAdapter adapter, final View itemView) {
            super(itemView);
            this.adapter = adapter;
            itemView.findViewById(R.id.media_remove).setVisibility(View.GONE);
            imageView = (ImageView) itemView.findViewById(R.id.media_preview);
            imageView.setImageResource(R.drawable.ic_pick_source_camera);
        }

        public void display(final long id) {
            adapter.getImageLoader().displayImage("media-thumb://" + id, imageView);
        }
    }
}
