package org.goodev.cropimage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.util.Log;

public class Lds {
    public static final String[] PROJECTION_IMAGES = new String[] { Images.ImageColumns._ID, Images.ImageColumns.TITLE,
        Images.ImageColumns.MIME_TYPE, Images.ImageColumns.LATITUDE, Images.ImageColumns.LONGITUDE,
        Images.ImageColumns.DATE_TAKEN, Images.ImageColumns.DATE_ADDED, Images.ImageColumns.DATE_MODIFIED,
        Images.ImageColumns.DATA, Images.ImageColumns.ORIENTATION, Images.ImageColumns.BUCKET_ID };

public static final String[] PROJECTION_VIDEOS = new String[] { Video.VideoColumns._ID, Video.VideoColumns.TITLE,
        Video.VideoColumns.MIME_TYPE, Video.VideoColumns.LATITUDE, Video.VideoColumns.LONGITUDE, Video.VideoColumns.DATE_TAKEN,
        Video.VideoColumns.DATE_ADDED, Video.VideoColumns.DATE_MODIFIED, Video.VideoColumns.DATA, Video.VideoColumns.DURATION,
        Video.VideoColumns.BUCKET_ID };

    public static MediaItem createMediaItemFromUri(Context context, Uri target, int mediaType) {
        MediaItem item = null;
        long id = ContentUris.parseId(target);
        ContentResolver cr = context.getContentResolver();
        String whereClause = Images.ImageColumns._ID + "=" + Long.toString(id);
        try {
            final Uri uri = (mediaType == MediaItem.MEDIA_TYPE_IMAGE)
                    ? Images.Media.EXTERNAL_CONTENT_URI
                    : Video.Media.EXTERNAL_CONTENT_URI;
            final String[] projection = (mediaType == MediaItem.MEDIA_TYPE_IMAGE)
                    ? PROJECTION_IMAGES
                    : PROJECTION_VIDEOS;
            Cursor cursor = cr.query(uri, projection, whereClause, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    item = new MediaItem();
                    populateMediaItemFromCursor(item, cr, cursor, uri.toString() + "/");
                }
                cursor.close();
                cursor = null;
            }
        } catch (Exception e) {
            // If the database operation failed for any reason.
            ;
        }
        item.mId = id;
        return item;
    }
    

    // Must preserve order between these indices and the order of the terms in
    // INITIAL_PROJECTION_IMAGES and
    // INITIAL_PROJECTION_VIDEOS.
    public static final int MEDIA_ID_INDEX = 0;
    public static final int MEDIA_CAPTION_INDEX = 1;
    public static final int MEDIA_MIME_TYPE_INDEX = 2;
    public static final int MEDIA_LATITUDE_INDEX = 3;
    public static final int MEDIA_LONGITUDE_INDEX = 4;
    public static final int MEDIA_DATE_TAKEN_INDEX = 5;
    public static final int MEDIA_DATE_ADDED_INDEX = 6;
    public static final int MEDIA_DATE_MODIFIED_INDEX = 7;
    public static final int MEDIA_DATA_INDEX = 8;
    public static final int MEDIA_ORIENTATION_OR_DURATION_INDEX = 9;
    public static final int MEDIA_BUCKET_ID_INDEX = 10;
    public static final void populateMediaItemFromCursor(final MediaItem item, final ContentResolver cr, final Cursor cursor,
            final String baseUri) {
        item.mId = cursor.getLong(MEDIA_ID_INDEX);
        item.mCaption = cursor.getString(MEDIA_CAPTION_INDEX);
        item.mMimeType = cursor.getString(MEDIA_MIME_TYPE_INDEX);
        item.mLatitude = cursor.getDouble(MEDIA_LATITUDE_INDEX);
        item.mLongitude = cursor.getDouble(MEDIA_LONGITUDE_INDEX);
        item.mDateTakenInMs = cursor.getLong(MEDIA_DATE_TAKEN_INDEX);
        item.mDateAddedInSec = cursor.getLong(MEDIA_DATE_ADDED_INDEX);
        item.mDateModifiedInSec = cursor.getLong(MEDIA_DATE_MODIFIED_INDEX);
        if (item.mDateTakenInMs == item.mDateModifiedInSec) {
            item.mDateTakenInMs = item.mDateModifiedInSec * 1000;
        }
        item.mFilePath = cursor.getString(MEDIA_DATA_INDEX);
        if (baseUri != null)
            item.mContentUri = baseUri + item.mId;
        final int itemMediaType = item.getMediaType();
        final int orientationDurationValue = cursor.getInt(MEDIA_ORIENTATION_OR_DURATION_INDEX);
        if (itemMediaType == MediaItem.MEDIA_TYPE_IMAGE) {
            item.mRotation = orientationDurationValue;
        } else {
            item.mDurationInSec = orientationDurationValue;
        }
    }
    
    /**
     * Matches code in MediaProvider.computeBucketValues. Should be a common
     * function.
     */
    public static int getBucketId(String path) {
        return (path.toLowerCase().hashCode());
    }
    
    public static final String getCachePath(final String subFolderName) {
        return Environment.getExternalStorageDirectory() + "/Android/data/com.cooliris.media/cache/" + subFolderName;
    }
    
    public static final Bitmap createFromUri(Context context, String uri, int maxResolutionX, int maxResolutionY, long cacheId,
            ClientConnectionManager connectionManager) throws IOException, URISyntaxException, OutOfMemoryError {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inDither = true;
        long crc64 = 0;
        Bitmap bitmap = null;
        if (uri.startsWith(ContentResolver.SCHEME_CONTENT)) {
            // We need the filepath for the given content uri
            crc64 = cacheId;
        } else {
            crc64 = Utils.Crc64Long(uri);
        }
        bitmap = createFromCache(crc64, maxResolutionX);
        if (bitmap != null) {
            return bitmap;
        }
        final boolean local = uri.startsWith(ContentResolver.SCHEME_CONTENT) || uri.startsWith("file://");

        // Get the input stream for computing the sample size.
        BufferedInputStream bufferedInput = null;
        if (uri.startsWith(ContentResolver.SCHEME_CONTENT) ||
                uri.startsWith(ContentResolver.SCHEME_FILE)) {
            // Get the stream from a local file.
            bufferedInput = new BufferedInputStream(context.getContentResolver()
                    .openInputStream(Uri.parse(uri)), 16384);
        } else {
            // Get the stream from a remote URL.
            bufferedInput = createInputStreamFromRemoteUrl(uri, connectionManager);
        }

        // Compute the sample size, i.e., not decoding real pixels.
        if (bufferedInput != null) {
            options.inSampleSize = computeSampleSize(bufferedInput, maxResolutionX, maxResolutionY);
        } else {
            return null;
        }

        // Get the input stream again for decoding it to a bitmap.
        bufferedInput = null;
        if (uri.startsWith(ContentResolver.SCHEME_CONTENT) ||
                uri.startsWith(ContentResolver.SCHEME_FILE)) {
            // Get the stream from a local file.
            bufferedInput = new BufferedInputStream(context.getContentResolver()
                    .openInputStream(Uri.parse(uri)), 16384);
        } else {
            // Get the stream from a remote URL.
            bufferedInput = createInputStreamFromRemoteUrl(uri, connectionManager);
        }

        // Decode bufferedInput to a bitmap.
        if (bufferedInput != null) {
            options.inDither = false;
            options.inJustDecodeBounds = false;
            Thread timeoutThread = new Thread("BitmapTimeoutThread") {
                public void run() {
                    try {
                        Thread.sleep(6000);
                        options.requestCancelDecode();
                    } catch (InterruptedException e) {
                    }
                }
            };
            timeoutThread.start();

            bitmap = BitmapFactory.decodeStream(bufferedInput, null, options);
        }

        if ((options.inSampleSize > 1 || !local) && bitmap != null) {
            writeToCache(crc64, bitmap, maxResolutionX / options.inSampleSize);
        }
        return bitmap;
    }
    private static int computeSampleSize(InputStream stream, int maxResolutionX,
            int maxResolutionY) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(stream, null, options);
            int maxNumOfPixels = maxResolutionX * maxResolutionY;
            int minSideLength = Math.min(maxResolutionX, maxResolutionY) / 2;
            return Utils.computeSampleSize(options, minSideLength, maxNumOfPixels);
        }
    public static final HttpParams HTTP_PARAMS;
    private static final String USER_AGENT = "Cooliris-ImageDownload";
    private static final int CONNECTION_TIMEOUT = 20000; // ms.
    static {
        // Prepare HTTP parameters.
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setStaleCheckingEnabled(params, false);
        HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, CONNECTION_TIMEOUT);
        HttpClientParams.setRedirecting(params, true);
        HttpProtocolParams.setUserAgent(params, USER_AGENT);
        HTTP_PARAMS = params;

    }
    private static final String TAG = "UriTexture";
    private static final BufferedInputStream createInputStreamFromRemoteUrl(
            String uri, ClientConnectionManager connectionManager) {
        InputStream contentInput = null;
        if (connectionManager == null) {
            try {
                URL url = new URI(uri).toURL();
                URLConnection conn = url.openConnection();
                conn.connect();
                contentInput = conn.getInputStream();
            } catch (Exception e) {
                Log.w(TAG, "Request failed: " + uri);
                e.printStackTrace();
                return null;
            }
        } else {
            // We create a cancelable http request from the client
            final DefaultHttpClient mHttpClient = new DefaultHttpClient(connectionManager, HTTP_PARAMS);
            HttpUriRequest request = new HttpGet(uri);
            // Execute the HTTP request.
            HttpResponse httpResponse = null;
            try {
                httpResponse = mHttpClient.execute(request);
                HttpEntity entity = httpResponse.getEntity();
                if (entity != null) {
                    // Wrap the entity input stream in a GZIP decoder if
                    // necessary.
                    contentInput = entity.getContent();
                }
            } catch (Exception e) {
                Log.w(TAG, "Request failed: " + request.getURI());
                return null;
            }
        }
        if (contentInput != null) {
            return new BufferedInputStream(contentInput, 4096);
        } else {
            return null;
        }
    }

    public static Bitmap createFromCache(long crc64, int maxResolution) {
        try {
            String file = null;
            Bitmap bitmap = null;
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inDither = true;
            if (crc64 != 0) {
                file = createFilePathFromCrc64(crc64, maxResolution);
                bitmap = BitmapFactory.decodeFile(file, options);
            }
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }
    public static final String URI_CACHE = Lds.getCachePath("hires-image-cache");
    public static final String createFilePathFromCrc64(long crc64, int maxResolution) {
        return URI_CACHE + crc64 + "_" + maxResolution + ".cache";
    }

    public static boolean isCached(long crc64, int maxResolution) {
        String file = null;
        if (crc64 != 0) {
            file = createFilePathFromCrc64(crc64, maxResolution);
            try {
                new FileInputStream(file);
                return true;
            } catch (FileNotFoundException e) {
                return false;
            }
        }
        return false;
    }
    public static void writeToCache(long crc64, Bitmap bitmap, int maxResolution) {
        String file = createFilePathFromCrc64(crc64, maxResolution);
        if (bitmap != null && file != null && crc64 != 0) {
            try {
                File fileC = new File(file);
                fileC.createNewFile();
                final FileOutputStream fos = new FileOutputStream(fileC);
                final BufferedOutputStream bos = new BufferedOutputStream(fos, 16384);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
                bos.flush();
                bos.close();
                fos.close();
            } catch (Exception e) {

            }
        }
    }
}
