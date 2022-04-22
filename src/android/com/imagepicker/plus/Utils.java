package com.imagepicker.plus;

import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.webkit.MimeTypeMap;

import androidx.exifinterface.media.ExifInterface;

import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Utils {
    public static String fileNamePrefix = "image_picker_plus_lib_temp_";

    public static File createFile(Context reactContext, String fileType) {
        try {
            String filename = fileNamePrefix  + UUID.randomUUID() + "." + fileType;

            // getCacheDir will auto-clean according to android docs
            File fileDir = reactContext.getCacheDir();

            File file = new File(fileDir, filename);
            file.createNewFile();
            return file;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void copyUri(Uri fromUri, Uri toUri, ContentResolver resolver) {
        try {
            OutputStream os = resolver.openOutputStream(toUri);
            InputStream is = resolver.openInputStream(fromUri);

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Make a copy of shared storage files inside app specific storage so that users can access it later.
    public static Uri getAppSpecificStorageUri(Uri sharedStorageUri, Context context) {
        if (sharedStorageUri == null) {
            return null;
        }
        ContentResolver contentResolver = context.getContentResolver();
        String fileType = getFileTypeFromMime(contentResolver.getType(sharedStorageUri));
        Uri toUri =  Uri.fromFile(createFile(context, fileType));
        copyUri(sharedStorageUri, toUri, contentResolver);
        return toUri;
    }

    public static int[] getImageDimensions(Uri uri, Context reactContext) {
        InputStream inputStream;
        try {
            inputStream = reactContext.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return new int[]{0, 0};
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream,null, options);
        return new int[]{options.outWidth, options.outHeight};
    }

    static String getBase64String(Uri uri, Context reactContext) {
        InputStream inputStream;
        try {
            inputStream = reactContext.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        byte[] bytes;
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        bytes = output.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    // Resize image
    // When decoding a jpg to bitmap all exif meta data will be lost, so make sure to copy orientation exif to new file else image might have wrong orientations
    public static Uri resizeImage(Uri uri, Context context, Options options) {
        try {
            int[] origDimens = getImageDimensions(uri, context);

            if (!shouldResizeImage(origDimens[0], origDimens[1], options)) {
                return uri;
            }

            int[] newDimens = getImageDimensBasedOnConstraints(origDimens[0], origDimens[1], options);

            InputStream imageStream = context.getContentResolver().openInputStream(uri);
            String mimeType =  getMimeTypeFromFileUri(uri);
            Bitmap b = BitmapFactory.decodeStream(imageStream);
            b = Bitmap.createScaledBitmap(b, newDimens[0], newDimens[1], true);
            String originalOrientation = getOrientation(uri, context);

            File file = createFile(context, getFileTypeFromMime(mimeType));
            OutputStream os = context.getContentResolver().openOutputStream(Uri.fromFile(file));
            b.compress(getBitmapCompressFormat(mimeType), options.quality, os);
            setOrientation(file, originalOrientation);
            return Uri.fromFile(file);

        } catch (Exception e) {
            e.printStackTrace();
            return uri; // cannot resize the image, return the original uri
        }
    }

    static String getOrientation(Uri uri, Context context) throws IOException {
        ExifInterface exifInterface = new ExifInterface(context.getContentResolver().openInputStream(uri));
        return exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION);
    }

    // ExifInterface.saveAttributes is costly operation so don't set exif for unnecessary orientations
    static void setOrientation(File file, String orientation) throws IOException {
        if (orientation.equals(String.valueOf(ExifInterface.ORIENTATION_NORMAL)) || orientation.equals(String.valueOf(ExifInterface.ORIENTATION_UNDEFINED))) {
            return;
        }
        ExifInterface exifInterface = new ExifInterface(file);
        exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, orientation);
        exifInterface.saveAttributes();
    }

    static int[] getImageDimensBasedOnConstraints(int origWidth, int origHeight, Options options) {
        int width = origWidth;
        int height = origHeight;

        if (options.maxWidth == 0 || options.maxHeight == 0) {
            return new int[]{width, height};
        }

        if (options.maxWidth < width) {
            height = (int) (((float) options.maxWidth / width) * height);
            width = options.maxWidth;
        }

        if (options.maxHeight < height) {
            width = (int) (((float) options.maxHeight / height) * width);
            height = options.maxHeight;
        }

        return new int[]{width, height};
    }

    static boolean shouldResizeImage(int origWidth, int origHeight, Options options) {
        if ((options.maxWidth == 0 || options.maxHeight == 0) && options.quality == 100) {
            return false;
        }

        return options.maxWidth < origWidth || options.maxHeight < origHeight || options.quality != 100;
    }

    static Bitmap.CompressFormat getBitmapCompressFormat(String mimeType) {
        switch (mimeType) {
            case "image/jpeg": return Bitmap.CompressFormat.JPEG;
            case "image/png": return Bitmap.CompressFormat.PNG;
        }
        return Bitmap.CompressFormat.JPEG;
    }

    static String getFileTypeFromMime(String mimeType) {
        if (mimeType == null) {
            return "jpg";
        }
        switch (mimeType) {
            case "image/jpeg": return "jpg";
            case "image/png": return "png";
            case "image/gif": return "gif";
        }
        return "jpg";
    }

    static String getMimeTypeFromFileUri(Uri uri) {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()));
    }

    static List<Uri> collectUrisFromData(Intent data) {
        // Default Gallery app on older Android versions doesn't support multiple image
        // picking and thus never uses clip data.
        if (data.getClipData() == null) {
            return Collections.singletonList(data.getData());
        }

        ClipData clipData = data.getClipData();
        List<Uri> fileUris = new ArrayList<>(clipData.getItemCount());

        for (int i = 0; i < clipData.getItemCount(); ++i) {
            fileUris.add(clipData.getItemAt(i).getUri());
        }

        return fileUris;
    }

    static JSONArray getResponseAssets(List<? extends Uri> fileUris, Options options, Context context) {
        JSONArray assets = new JSONArray();

        for(int i = 0; i < fileUris.size(); ++i) {
            Uri uri = fileUris.get(i);

            if (uri.getScheme().contains("content")) {
                uri = getAppSpecificStorageUri(uri, context);
            }
            uri = resizeImage(uri, context, options);

            if (options.isBase64Output()) {
                assets.put(getBase64String(uri, context));
            } else {
                assets.put(uri.toString());
            }
        }

        return assets;
    }
}
