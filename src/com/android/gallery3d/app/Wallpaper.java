/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;

import com.android.gallery3d.common.ApiHelper;

import com.mediatek.gallery3d.stereo.StereoHelper;
import com.mediatek.gallery3d.util.MediatekFeature;

/**
 * Wallpaper picker for the gallery application. This just redirects to the
 * standard pick action.
 */
public class Wallpaper extends Activity {
    @SuppressWarnings("unused")
    private static final String TAG = "Gallery2/Wallpaper";

    private static final String IMAGE_TYPE = "image/*";
    private static final String KEY_STATE = "activity-state";
    private static final String KEY_PICKED_ITEM = "picked-item";

    private static final int STATE_INIT = 0;
    private static final int STATE_PHOTO_PICKED = 1;

    private int mState = STATE_INIT;
    private Uri mPickedItem;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle != null) {
            mState = bundle.getInt(KEY_STATE);
            mPickedItem = (Uri) bundle.getParcelable(KEY_PICKED_ITEM);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle saveState) {
        saveState.putInt(KEY_STATE, mState);
        if (mPickedItem != null) {
            saveState.putParcelable(KEY_PICKED_ITEM, mPickedItem);
        }
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private Point getDefaultDisplaySize(Point size) {
        Display d = getWindowManager().getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= ApiHelper.VERSION_CODES.HONEYCOMB_MR2) {
            d.getSize(size);
        } else {
            size.set(d.getWidth(), d.getHeight());
        }
        return size;
    }

    @SuppressWarnings("fallthrough")
    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        switch (mState) {
            case STATE_INIT: {
                mPickedItem = intent.getData();
                if (mPickedItem == null) {
                    // M: for MTK-specific pick-and-crop flow
                    if (startMtkCropFlow()) {
                        return;
                    }
                    Intent request = new Intent(Intent.ACTION_GET_CONTENT)
                            .setClass(this, DialogPicker.class)
                            .setType(IMAGE_TYPE);
                    //add Drm Feature support, specify that only FL
                    //kind drm media can be queried out.
                    if (MediatekFeature.isDrmSupported()) {
                        request.putExtra(com.mediatek.drm.OmaDrmStore.DrmExtra.EXTRA_DRM_LEVEL,
                                         com.mediatek.drm.OmaDrmStore.DrmExtra.DRM_LEVEL_FL);
                    }
                    if (MediatekFeature.isStereoDisplaySupported()) {
                        request.putExtra(StereoHelper.ATTACH_WITHOUT_CONVERSION, true);
                    }
                    startActivityForResult(request, STATE_PHOTO_PICKED);
                    return;
                }
                mState = STATE_PHOTO_PICKED;
                // fall-through
            }
            case STATE_PHOTO_PICKED: {
                int width = getWallpaperDesiredMinimumWidth();
                int height = getWallpaperDesiredMinimumHeight();
                Point size = getDefaultDisplaySize(new Point());
                float spotlightX = (float) size.x / width;
                float spotlightY = (float) size.y / height;
                Intent request = new Intent(CropImage.ACTION_CROP)
                        .setDataAndType(mPickedItem, IMAGE_TYPE)
                        .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                        .putExtra(CropImage.KEY_OUTPUT_X, width)
                        .putExtra(CropImage.KEY_OUTPUT_Y, height)
                        .putExtra(CropImage.KEY_ASPECT_X, width)
                        .putExtra(CropImage.KEY_ASPECT_Y, height)
                        .putExtra(CropImage.KEY_SPOTLIGHT_X, spotlightX)
                        .putExtra(CropImage.KEY_SPOTLIGHT_Y, spotlightY)
                        .putExtra(CropImage.KEY_SCALE, true)
                        .putExtra(CropImage.KEY_SCALE_UP_IF_NEEDED, true)
                        .putExtra(CropImage.KEY_NO_FACE_DETECTION, true)
                        .putExtra(CropImage.KEY_SET_AS_WALLPAPER, true);
                startActivity(request);
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            setResult(resultCode);
            finish();
            return;
        }
        mState = requestCode;
        if (mState == STATE_PHOTO_PICKED) {
            mPickedItem = data.getData();
        }

        // onResume() would be called next
    }
    
    // M: added for wallpaper flow change
    public static final String EXTRA_CROP_FOR_WALLPAPER = "crop-for-wallpaper";
    
    private boolean startMtkCropFlow() {
        if (!MediatekFeature.MTK_CHANGE_PICK_CROP_FLOW) {
            return false;
        }
        Intent request = new Intent(Intent.ACTION_GET_CONTENT)
                .setClass(this, DialogPicker.class)
                .setType(IMAGE_TYPE);
        request.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        int width = getWallpaperDesiredMinimumWidth();
        int height = getWallpaperDesiredMinimumHeight();
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        float spotlightX = (float) size.x / width;
        float spotlightY = (float) size.y / height;
        // M: All of these parameters are put together in one bundle
        // and passed along to AlbumPage; CropImage will get these eventually
        request.putExtra(CropImage.KEY_OUTPUT_X, width)
                .putExtra(CropImage.KEY_OUTPUT_Y, height)
                .putExtra(CropImage.KEY_ASPECT_X, width)
                .putExtra(CropImage.KEY_ASPECT_Y, height)
                .putExtra(CropImage.KEY_SPOTLIGHT_X, spotlightX)
                .putExtra(CropImage.KEY_SPOTLIGHT_Y, spotlightY)
                .putExtra(CropImage.KEY_SCALE, true)
                .putExtra(CropImage.KEY_SCALE_UP_IF_NEEDED, true)
                .putExtra(CropImage.KEY_NO_FACE_DETECTION, true)
                .putExtra(CropImage.KEY_SET_AS_WALLPAPER, true)
                .putExtra(Gallery.EXTRA_CROP, EXTRA_CROP_FOR_WALLPAPER);
        
        //add Drm Feature support, specify that only FL
        //kind drm media can be queried out.
        if (MediatekFeature.isDrmSupported()) {
            request.putExtra(com.mediatek.drm.OmaDrmStore.DrmExtra.EXTRA_DRM_LEVEL,
                    com.mediatek.drm.OmaDrmStore.DrmExtra.DRM_LEVEL_FL);
        }
        if (MediatekFeature.isStereoDisplaySupported()) {
            request.putExtra(StereoHelper.ATTACH_WITHOUT_CONVERSION, true);
        }
        // M: for MTK-specific crop flow, we do not handle activity result here;
        // instead, they are all handled in AlbumPage
        startActivity(request);
        finish();
        return true;
    }
}
