package catchla.yep.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.hannesdorfmann.parcelableplease.annotation.ParcelablePlease;
import com.hannesdorfmann.parcelableplease.annotation.ParcelableThisPlease;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import catchla.yep.Constants;
import catchla.yep.util.Utils;

/**
 * Created by mariotaku on 15/10/14.
 */
@ParcelablePlease
@JsonObject
public class FileAttachment extends Attachment implements Parcelable {
    public static final Creator<FileAttachment> CREATOR = new Creator<FileAttachment>() {
        @Override
        public FileAttachment createFromParcel(Parcel in) {
            final FileAttachment attachment = new FileAttachment();
            FileAttachmentParcelablePlease.readFromParcel(attachment, in);
            return attachment;
        }

        @Override
        public FileAttachment[] newArray(int size) {
            return new FileAttachment[size];
        }
    };
    @ParcelableThisPlease
    @JsonField(name = "id")
    String id;
    @ParcelableThisPlease
    @JsonField(name = "metadata")
    String metadata;
    @ParcelableThisPlease
    @JsonField(name = "file")
    AttachmentFile file;

    public FileAttachment() {

    }

    public String getId() {
        return id;
    }

    public AttachmentFile getFile() {
        return file;
    }

    public String getMetadata() {
        return metadata;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        FileAttachmentParcelablePlease.writeToParcel(this, dest, flags);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final FileAttachment that = (FileAttachment) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (metadata != null ? !metadata.equals(that.metadata) : that.metadata != null)
            return false;
        return file != null ? file.equals(that.file) : that.file == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
        result = 31 * result + (file != null ? file.hashCode() : 0);
        return result;
    }

    public interface Metadata {
    }

    @JsonObject
    public static class AudioMetadata implements Metadata {
        @JsonField(name = "audio_samples")
        float[] samples;
        @JsonField(name = "audio_duration")
        float duration;

        public float getDuration() {
            return duration;
        }

        public void setDuration(final float duration) {
            this.duration = duration;
        }

        public float[] getSamples() {
            return samples;
        }

        public void setSamples(final float[] samples) {
            this.samples = samples;
        }
    }

    @JsonObject
    public static class ImageMetadata implements Metadata {
        @JsonField(name = "image_width")
        int width;
        @JsonField(name = "image_height")
        int height;
        @JsonField(name = "blurred_thumbnail_string")
        String blurredThumbnail;
        String mimeType;

        public static ImageMetadata getImageMetadata(final String imagePath) {
            final BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, o);
            final ImageMetadata metadata = new ImageMetadata();
            boolean swapWH = false;
            try {
                ExifInterface exif = new ExifInterface(imagePath);
                final int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                swapWH = orientation == ExifInterface.ORIENTATION_ROTATE_270 || orientation == ExifInterface.ORIENTATION_ROTATE_90;
            } catch (IOException ignore) {

            }
            metadata.setMimeType(o.outMimeType);
            metadata.setWidth(swapWH ? o.outHeight : o.outWidth);
            metadata.setHeight(swapWH ? o.outWidth : o.outHeight);
            o.inJustDecodeBounds = false;
            o.inSampleSize = Math.max(1, Math.max(o.outWidth, o.outHeight) / 100);
            final Bitmap downScaledBitmap = BitmapFactory.decodeFile(imagePath, o);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final Base64OutputStream os = new Base64OutputStream(baos, Base64.URL_SAFE);
            try {
                downScaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, os);
                os.flush();
                metadata.setBlurredThumbnail(baos.toString("ASCII"));
            } catch (IOException e) {
                Log.w(Constants.LOGTAG, e);
            } finally {
                downScaledBitmap.recycle();
                Utils.INSTANCE.closeSilently(os);
            }
            return metadata;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(final String mimeType) {
            this.mimeType = mimeType;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(final int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(final int height) {
            this.height = height;
        }

        public String getBlurredThumbnail() {
            return blurredThumbnail;
        }

        public void setBlurredThumbnail(final String blurredThumbnail) {
            this.blurredThumbnail = blurredThumbnail;
        }
    }

    @Override
    public String toString() {
        return "FileAttachment{" +
                "metadata='" + metadata + '\'' +
                ", file=" + file +
                "} " + super.toString();
    }
}
