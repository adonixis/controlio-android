package ru.adonixis.controlio.util;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.databinding.BindingAdapter;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import ru.adonixis.controlio.BuildConfig;
import ru.adonixis.controlio.R;

public class S3Utils {

    private static final String TAG = "S3Utils";

    public static void loadS3ObjectToImageView(final String s3ObjectKey, final ImageView imageView) {
        if (!TextUtils.isEmpty(s3ObjectKey)) {
            final Context context = imageView.getContext().getApplicationContext();
            final File file = new File(context.getCacheDir(), s3ObjectKey);
            if (file.exists()) {
                Glide.with(context)
                        .load(file)
                        .apply(new RequestOptions().placeholder(R.drawable.progress_animation))
                        .apply(RequestOptions.bitmapTransform(new CropSquareTransformation(context)))
                        .apply(RequestOptions.bitmapTransform(new RoundedCornersTransformation(context, 20, 0)))
                        .into(imageView);
            } else {
                imageView.setImageResource(R.drawable.progress_animation);

                CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                        context,
                        BuildConfig.IDENTITY_POOL_ID,
                        Regions.US_EAST_1
                );

                AmazonS3Client s3Client = new AmazonS3Client(credentialsProvider);

                TransferUtility transferUtility = new TransferUtility(s3Client, context);
                TransferObserver transferObserver = transferUtility.download(
                        BuildConfig.S3_BUCKET_NAME,
                        s3ObjectKey,
                        file
                );
                transferObserver.setTransferListener(new TransferListener() {

                    @Override
                    public void onStateChanged(int id, TransferState state) {
                        Log.d(TAG, "TransferListener.onStateChanged: " + state);
                        if (TransferState.COMPLETED.equals(state)) {
                            Glide.with(context)
                                    .load(file)
                                    .apply(new RequestOptions().placeholder(R.drawable.progress_animation))
                                    .apply(RequestOptions.bitmapTransform(new CropSquareTransformation(context)))
                                    .apply(RequestOptions.bitmapTransform(new RoundedCornersTransformation(context, 20, 0)))
                                    .into(imageView);
                        }
                    }

                    @Override
                    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                        if (bytesTotal != 0) {
                            int percentage = (int) (bytesCurrent / bytesTotal * 100);
                            Log.d(TAG, "TransferListener.onProgressChanged: " + percentage);
                        }
                    }

                    @Override
                    public void onError(int id, Exception ex) {
                        Log.e(TAG, "TransferListener.onError: ", ex);

                    }

                });
            }
        }
    }

    public static String getFileNameFromUri(ContextWrapper contextWrapper, Uri uri) {
        Cursor cursor = contextWrapper.getContentResolver().query(uri, null, null, null, null);
        int nameIndex = 0;
        if (cursor != null) {
            nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            cursor.moveToFirst();
            String name = cursor.getString(nameIndex);
            String extension = name.substring(name.lastIndexOf("."));
            String uniqueString = UUID.randomUUID().toString();
            Long tsLong = System.currentTimeMillis()/1000;
            String timeStamp = tsLong.toString();
            cursor.close();
            return uniqueString + '-' + timeStamp + extension;
        } else {
            return "";
        }
    }

    public static File createFileFromUri(ContextWrapper contextWrapper, Uri uri, String directory, String fileName) throws IOException {
        InputStream is = contextWrapper.getContentResolver().openInputStream(uri);
        File dir = new File(contextWrapper.getCacheDir(), directory + '/');
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File fileUpload = new File(dir, fileName);
        fileUpload.createNewFile();
        FileOutputStream fos = new FileOutputStream(fileUpload);
        byte[] buf = new byte[2046];
        int read = -1;
        while ((read = is.read(buf)) != -1) {
            fos.write(buf, 0, read);
        }
        fos.flush();
        fos.close();
        return fileUpload;
    }

    public static File createTempImageFile(ContextWrapper contextWrapper) throws IOException {
        String uniqueString = UUID.randomUUID().toString();
        Long tsLong = System.currentTimeMillis()/1000;
        String timeStamp = tsLong.toString();
        String imageFileName = "JPEG_" + uniqueString + "_" + timeStamp + "_";
        File storageDir = new File(contextWrapper.getFilesDir(), "photos");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return image;
    }

    public static void upload(Context context, File file, final String objectKey, TransferListener transferListener) {
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                context,
                BuildConfig.IDENTITY_POOL_ID,
                Regions.US_EAST_1
        );
        AmazonS3Client s3Client = new AmazonS3Client(credentialsProvider);
        TransferUtility transferUtility = new TransferUtility(s3Client, context);

        TransferObserver transferObserver = transferUtility.upload(
                BuildConfig.S3_BUCKET_NAME,
                objectKey,
                file
        );
        transferObserver.setTransferListener(transferListener);
    }

    @BindingAdapter({"s3ObjectKey"})
    public static void loadImage(ImageView imageView, String s3ObjectKey) {
        if (!TextUtils.isEmpty(s3ObjectKey)) {
            loadS3ObjectToImageView(s3ObjectKey, imageView);
        }
    }
}
