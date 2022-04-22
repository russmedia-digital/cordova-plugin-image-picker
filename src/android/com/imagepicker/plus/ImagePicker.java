package com.imagepicker.plus;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Collections;

import gun0912.tedimagepicker.builder.TedImagePicker;
import gun0912.tedimagepicker.builder.type.MediaType;

public class ImagePicker extends CordovaPlugin {

    private static final String ACTION_GET_PICTURES = "getPictures";
    private static final String ACTION_HAS_READ_PERMISSION = "hasReadPermission";
    private static final String ACTION_REQUEST_READ_PERMISSION = "requestReadPermission";
    
    private static final int PERMISSION_REQUEST_CODE = 100;

    private CallbackContext callbackContext;
    private Options options;

    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;

        if (ACTION_HAS_READ_PERMISSION.equals(action)) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, hasReadPermission()));
            return true;

        } else if (ACTION_REQUEST_READ_PERMISSION.equals(action)) {            
            requestReadPermission();
            return true;

        } else if (ACTION_GET_PICTURES.equals(action)) {
            options = new Options(args.getJSONObject(0));

            if (options.isSingleSelect()) {
                TedImagePicker.with(cordova.getContext())
                        .mediaType(MediaType.IMAGE)
                        .showCameraTile(false)
                        .dropDownAlbum()
                        .start(uri -> {
                            JSONArray assets = Utils.getResponseAssets(Collections.singletonList(uri),
                                    options,
                                    cordova.getContext());
                            callbackContext.success(assets);
                        });

            } else {
                TedImagePicker.with(cordova.getContext())
                        .mediaType(MediaType.IMAGE)
                        .showCameraTile(false)
                        .max(options.selectionLimit, options.getMaxCountMessage())
                        .dropDownAlbum()
                        .startMultiImage(uris -> {
                            JSONArray assets = Utils.getResponseAssets(uris,
                                    options,
                                    cordova.getContext());
                            callbackContext.success(assets);
                        });
            }
            return true;
        }
        return false;
    }

    private boolean hasReadPermission() {
        return Build.VERSION.SDK_INT < 23 ||
            PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this.cordova.getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    private void requestReadPermission() {
        if (!hasReadPermission()) {    
            ActivityCompat.requestPermissions(
                this.cordova.getActivity(),
                new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_CODE);
        }
        callbackContext.success();
    }
}
