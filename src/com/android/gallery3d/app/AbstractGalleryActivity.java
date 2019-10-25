/*
 * Copyright (C) 2010 The Android Open Source Project
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
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.view.View;

import com.android.gallery3d.R;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.data.BitmapPool;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLRootView;
import com.android.gallery3d.util.ThreadPool;
import com.android.gallery3d.util.LightCycleHelper.PanoramaViewHelper;
import android.os.Environment;
import com.mediatek.gallery3d.util.MtkUtils;

public class AbstractGalleryActivity extends Activity implements GalleryContext {
    @SuppressWarnings("unused")
    private static final String TAG = "Gallery2/AbstractGalleryActivity";
    private GLRootView mGLRootView;
    private StateManager mStateManager;
    private GalleryActionBar mActionBar;
    private OrientationManager mOrientationManager;
    private TransitionStore mTransitionStore = new TransitionStore();
    private boolean mDisableToggleStatusBar;
    private PanoramaViewHelper mPanoramaViewHelper;

    private AlertDialog mAlertDialog = null;
    /// M: sign gallery status.
    private volatile boolean hasPausedActivity;
    /// M: if shouldHideToast is true, should hide empty_album Toast. for multi delete operation.
    private volatile boolean shouldHideToast;
    private BroadcastReceiver mMountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //if (getExternalCacheDir() != null) onStorageReady();
            // we don't care about SD card content;
            // As long as the card is mounted, dismiss the dialog
            onStorageReady();
        }
    };
    private IntentFilter mMountFilter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mOrientationManager = new OrientationManager(this);
        toggleStatusBarByOrientation();
        getWindow().setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent)); //changed by yuanhuawei from null 20130828
        mPanoramaViewHelper = new PanoramaViewHelper(this);
        mPanoramaViewHelper.onCreate();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mGLRootView.lockRenderThread();
        try {
            super.onSaveInstanceState(outState);
            getStateManager().saveState(outState);
        } finally {
            mGLRootView.unlockRenderThread();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        mStateManager.onConfigurationChange(config);
        getGalleryActionBar().onConfigurationChanged();
        invalidateOptionsMenu();
        toggleStatusBarByOrientation();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return getStateManager().createOptionsMenu(menu);
    }

    @Override
    public Context getAndroidContext() {
        return this;
    }

    @Override
    public DataManager getDataManager() {
        return ((GalleryApp) getApplication()).getDataManager();
    }

    @Override
    public ThreadPool getThreadPool() {
        return ((GalleryApp) getApplication()).getThreadPool();
    }

    public synchronized StateManager getStateManager() {
        if (mStateManager == null) {
            mStateManager = new StateManager(this);
        }
        return mStateManager;
    }

    public GLRoot getGLRoot() {
        return mGLRootView;
    }

    public GLRootView getGLRootView() {//added by scq
        return mGLRootView;
    }
    public OrientationManager getOrientationManager() {
        return mOrientationManager;
    }

    @Override
    public void setContentView(int resId) {
        super.setContentView(resId);
        mGLRootView = (GLRootView) findViewById(R.id.gl_root_view);
    }

    protected void onStorageReady() {
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
            mAlertDialog = null;
            unregisterReceiver(mMountReceiver);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        
        // M: if we're viewing a non-local file/uri, do NOT check storage 
        // or pop up "No storage" dialog
        Log.d(TAG, "onStart: should check storage=" + mShouldCheckStorageState);
        if (!mShouldCheckStorageState) {
            return;
        }
        
        // M: we only care about not mounted condition,
        // SD card full/error state does not affect normal usage of Gallery2
        if ((MtkUtils.getMTKExternalCacheDir(this) == null) && (!isDefaultStorageMounted())) {
            OnCancelListener onCancel = new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            };
            OnClickListener onClick = new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.no_external_storage_title)
                    .setMessage(R.string.no_external_storage)
                    .setNegativeButton(android.R.string.cancel, onClick)
                    .setOnCancelListener(onCancel);
            if (ApiHelper.HAS_SET_ICON_ATTRIBUTE) {
                setAlertDialogIconAttribute(builder);
            } else {
                builder.setIcon(android.R.drawable.ic_dialog_alert);
            }
            mAlertDialog = builder.show();
            if(mMountFilter == null) {
                mMountFilter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
                mMountFilter.addDataScheme("file");
            }
            registerReceiver(mMountReceiver, mMountFilter);
        }
        mPanoramaViewHelper.onStart();
    }

    @TargetApi(ApiHelper.VERSION_CODES.HONEYCOMB)
    private static void setAlertDialogIconAttribute(
            AlertDialog.Builder builder) {
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAlertDialog != null) {
            unregisterReceiver(mMountReceiver);
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
        mPanoramaViewHelper.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLRootView.lockRenderThread();
        try {
            getStateManager().resume();
            getDataManager().resume();
        } finally {
            mGLRootView.unlockRenderThread();
        }
        mGLRootView.onResume();
        mOrientationManager.resume();
        /// M: save activity status.
        hasPausedActivity = false;
        ///M:
        mGLRootView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mOrientationManager.pause();
        mGLRootView.onPause();
        mGLRootView.lockRenderThread();
        try {
            getStateManager().pause();
            getDataManager().pause();
        } finally {
            mGLRootView.unlockRenderThread();
        }
        clearBitmapPool(MediaItem.getMicroThumbPool());
        clearBitmapPool(MediaItem.getThumbPool());

        MediaItem.getBytesBufferPool().clear();
        /// M: save activity status.
        hasPausedActivity = true;
        ///M: the picture show abnormal after rotate device
        ///to landscape mode,lock device, rotate device to portrait
        ///mode, unlock device.
        ///to resolve this problem, let it show dark color
        mGLRootView.setVisibility(View.GONE);
    }

    private static void clearBitmapPool(BitmapPool pool) {
        if (pool != null) pool.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGLRootView.lockRenderThread();
        try {
            getStateManager().destroy();
        } finally {
            mGLRootView.unlockRenderThread();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mGLRootView.lockRenderThread();
        try {
            getStateManager().notifyActivityResult(
                    requestCode, resultCode, data);
        } finally {
            mGLRootView.unlockRenderThread();
        }
    }

    @Override
    public void onBackPressed() {
        // send the back event to the top sub-state
        GLRoot root = getGLRoot();
        root.lockRenderThread();
        try {
            getStateManager().onBackPressed();
        } finally {
            root.unlockRenderThread();
        }
    }

    public GalleryActionBar getGalleryActionBar() {
        if (mActionBar == null) {
            mActionBar = new GalleryActionBar(this);
        }
        return mActionBar;
    }

    public boolean hasPausedActivity () {
        return hasPausedActivity;
    }

    public void setHideToast (boolean hideToast) {
        shouldHideToast = hideToast;
    }

    public boolean isHidedToast () {
        return shouldHideToast ;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        GLRoot root = getGLRoot();
        root.lockRenderThread();
        try {
            return getStateManager().itemSelected(item);
        } finally {
            root.unlockRenderThread();
        }
    }

    protected void disableToggleStatusBar() {
        mDisableToggleStatusBar = true;
    }

    // Shows status bar in portrait view, hide in landscape view
    private void toggleStatusBarByOrientation() {
        if (mDisableToggleStatusBar) return;

        Window win = getWindow();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            win.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            win.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    public TransitionStore getTransitionStore() {
        return mTransitionStore;
    }

    public PanoramaViewHelper getPanoramaViewHelper() {
        return mPanoramaViewHelper;
    }

    protected boolean isFullscreen() {
        return (getWindow().getAttributes().flags
                & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
    }
    
    // M: added for multi-storage support
    private boolean isDefaultStorageMounted() {
        String defaultStorageState = MtkUtils.getMtkDefaultStorageState(this);
        if (defaultStorageState == null) {
            defaultStorageState = Environment.getExternalStorageState();
        }
        return Environment.MEDIA_MOUNTED.equalsIgnoreCase(defaultStorageState);
    }
    
    // M: added for SD hot-plug
    public boolean mShouldCheckStorageState = true;
}
