package com.imagepicker.plus;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

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
    
    private static final int PERMISSION_REQUEST_CODE = 1501;

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
        return Build.VERSION.SDK_INT < 23 || cordova.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    private void requestReadPermission() {
        if (hasReadPermission()) {
            callbackContext.success();
            return;
        }
        cordova.requestPermission(this, PERMISSION_REQUEST_CODE, Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) {
        if (requestCode != PERMISSION_REQUEST_CODE) {
            return;
        }
        if (permissions[0].equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                callbackContext.success();
            } else {
                callbackContext.error("Permission denied");
            }
        }
    }
}
