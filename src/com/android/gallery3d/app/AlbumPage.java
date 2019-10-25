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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.gallery3d.R;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.MtpDevice;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.ui.ActionModeHandler;
import com.android.gallery3d.ui.ActionModeHandler.ActionModeListener;
import com.android.gallery3d.ui.AlbumSlotRenderer;
import com.android.gallery3d.ui.DetailsHelper;
import com.android.gallery3d.ui.DetailsHelper.CloseListener;
import com.android.gallery3d.ui.FadeTexture;
import com.android.gallery3d.ui.GLCanvas;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.PhotoFallbackEffect;
import com.android.gallery3d.ui.RelativePosition;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SlotView;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.MediaSetUtils;

import java.util.ArrayList;
import java.util.Random;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Handler;

import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.ThreadPool.Job;
import com.android.gallery3d.util.ThreadPool.JobContext;
/* Begin: added by yuanhuawei 20130809*/
import com.android.gallery3d.app.IphoneAlbumBottomNavigationBar;
import com.android.gallery3d.app.IphoneBottomSharePopWindow;
import com.android.gallery3d.app.IphoneBottomDeletePopWindow;
import com.android.gallery3d.app.IphoneAlbumTopBar;
import android.widget.RelativeLayout;
import android.os.Parcelable;
import android.content.pm.PackageManager;
import java.util.List;
import android.content.pm.ResolveInfo;
import android.content.pm.ActivityInfo;
import android.content.ComponentName;
/* End: added by yuanhuawei 20130809*/

import com.mediatek.gallery3d.drm.DrmHelper;
import com.mediatek.gallery3d.stereo.StereoConvertor;
import com.mediatek.gallery3d.stereo.StereoHelper;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MtkLog;

public class AlbumPage extends ActivityState implements GalleryActionBar.ClusterRunner,
        SelectionManager.SelectionListener, MediaSet.SyncListener, GalleryActionBar.OnAlbumModeSelectedListener,
        IphoneAlbumBottomNavigationBar.Delegate,
        IphoneBottomSharePopWindow.Delegate,
        IphoneBottomDeletePopWindow.Delegate,
        IphoneAlbumTopBar.Delegate{ //added by yuanhuawei 20130809
    @SuppressWarnings("unused")
    private static final String TAG = "Gallery2/AlbumPage";

    public static final String KEY_MEDIA_PATH = "media-path";
    public static final String KEY_PARENT_MEDIA_PATH = "parent-media-path";
    public static final String KEY_SET_CENTER = "set-center";
    public static final String KEY_AUTO_SELECT_ALL = "auto-select-all";
    public static final String KEY_SHOW_CLUSTER_MENU = "cluster-menu";
    public static final String KEY_EMPTY_ALBUM = "empty-album";
    public static final String KEY_RESUME_ANIMATION = "resume_animation";
    public static final int REQUEST_CODE_ATTACH_IMAGE     = 100;//added by xiashuaishuai on 20150318
    private static final int REQUEST_SLIDESHOW = 1;
    public static final int REQUEST_PHOTO = 2;
    private static final int REQUEST_DO_ANIMATION = 3;

    // M: added for get content feature change
    private static final int REQUEST_CROP = 100;
    private static final int REQUEST_CROP_WALLPAPER = 101;
    private static final int BIT_LOADING_RELOAD = 1;
    private static final int BIT_LOADING_SYNC = 2;

    private static final float USER_DISTANCE_METER = 0.3f;

    /* Begin: added by yuanhuawei 20130928 */
    private static final int BOTTOM_BAR_HEIGHT = 80;
    /* End: added by yuanhuawei 20130928 */


    private static final boolean mIsDrmSupported = 
                                          MediatekFeature.isDrmSupported();
    private static final boolean mIsStereoDisplaySupported = 
                                          MediatekFeature.isStereoDisplaySupported();
    private int mMtkInclusion = 0;

    private ProgressDialog mProgressDialog;
    private Future<?> mConvertUriTask;
    private boolean mIsActive = false;
    private AlbumSlotRenderer mAlbumView;
    private Path mMediaSetPath;
    private String mParentMediaSetString;
    private SlotView mSlotView;

    private AlbumDataLoader mAlbumDataAdapter;

    protected SelectionManager mSelectionManager;
    private Vibrator mVibrator;

    private boolean mGetContent;
    private boolean mShowClusterMenu;

    private ActionModeHandler mActionModeHandler;
    private int mFocusIndex = 0;
    private DetailsHelper mDetailsHelper;
    private MyDetailsSource mDetailsSource;
    private MediaSet mMediaSet;
    private boolean mShowDetails;
    private float mUserDistance; // in pixel
    private Future<Integer> mSyncTask = null;
    private boolean mLaunchedFromPhotoPage;
    private boolean mInCameraApp;
    private boolean mInCameraAndWantQuitOnPause;

    private int mLoadingBits = 0;
    private boolean mInitialSynced = false;
    private int mSyncResult;
    private boolean mLoadingFailed;
    private RelativePosition mOpenCenter = new RelativePosition();

    /* Begin: added by yuanhuawei 20130809*/
    private IphoneAlbumBottomNavigationBar mBottomNavigationBarControls;
    private int mParentSelectedAction;

    private IphoneAlbumTopBar mAlbumTopBar;

    private IphoneBottomSharePopWindow mBottomSharePopWindow;

    private IphoneBottomDeletePopWindow mBottomDeletePopWindow;

    private final String mShareContactsName = "com.android.contacts.activities.AttachPhotoActivity";
    private final String mShareWallpaperName = "com.android.gallery3d.app.Wallpaper";
    /* End: added by yuanhuawei 20130809*/

    private Handler mHandler;
    private static final int MSG_PICK_PHOTO = 0;

    private PhotoFallbackEffect mResumeEffect;
    // save selection for onPause/onResume
    private boolean mNeedUpdateSelection = false;
    private PhotoFallbackEffect.PositionProvider mPositionProvider =
            new PhotoFallbackEffect.PositionProvider() {
        @Override
        public Rect getPosition(int index) {
            Rect rect = mSlotView.getSlotRect(index);
            Rect bounds = mSlotView.bounds();
            rect.offset(bounds.left - mSlotView.getScrollX(),
                    bounds.top - mSlotView.getScrollY());
            return rect;
        }

        @Override
        public int getItemIndex(Path path) {
            int start = mSlotView.getVisibleStart();
            int end = mSlotView.getVisibleEnd();
            for (int i = start; i < end; ++i) {
                MediaItem item = mAlbumDataAdapter.get(i);
                if (item != null && item.getPath() == path) return i;
            }
            return -1;
        }
    };

    @Override
    protected int getBackgroundColorId() {
        return R.color.album_background;
    }

    private final GLView mRootPane = new GLView() {
        private final float mMatrix[] = new float[16];

        @Override
        protected void onLayout(
                boolean changed, int left, int top, int right, int bottom) {

            int slotViewTop = mActivity.getGalleryActionBar().getHeight();
            int slotViewBottom = bottom - top - BOTTOM_BAR_HEIGHT; //changed by yuanhuawei 20130928 
            int slotViewRight = right - left;

            if (mShowDetails) {
                mDetailsHelper.layout(left, slotViewTop, right, bottom);
            } else {
                mAlbumView.setHighlightItemPath(null);
            }

            // Set the mSlotView as a reference point to the open animation
            mOpenCenter.setReferencePosition(0, slotViewTop);
            mSlotView.layout(0, slotViewTop, slotViewRight, slotViewBottom);
            GalleryUtils.setViewPointMatrix(mMatrix,
                    (right - left) / 2, (bottom - top) / 2, -mUserDistance);
        }

        @Override
        protected void render(GLCanvas canvas) {
            canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
            canvas.multiplyMatrix(mMatrix, 0);
            super.render(canvas);

            if (mResumeEffect != null) {
                boolean more = mResumeEffect.draw(canvas);
                if (!more) {
                    mResumeEffect = null;
                    mAlbumView.setSlotFilter(null);
                }
                // We want to render one more time even when no more effect
                // required. So that the animated thumbnails could be draw
                // with declarations in super.render().
                invalidate();
            }
            canvas.restore();
        }
    };

    // This are the transitions we want:
    //
    // +--------+           +------------+    +-------+    +----------+
    // | Camera |---------->| Fullscreen |--->| Album |--->| AlbumSet |
    // |  View  | thumbnail |   Photo    | up | Page  | up |   Page   |
    // +--------+           +------------+    +-------+    +----------+
    //     ^                      |               |            ^  |
    //     |                      |               |            |  |         close
    //     +----------back--------+               +----back----+  +--back->  app
    //
    @Override
    protected void onBackPressed() {
        if (mShowDetails) {
            hideDetails();
        } else if (mSelectionManager.inSelectionMode()) {
            mSelectionManager.leaveSelectionMode();
        } else {
            if(mLaunchedFromPhotoPage) {
                mActivity.getTransitionStore().putIfNotPresent(
                        PhotoPage.KEY_ALBUMPAGE_TRANSITION,
                        PhotoPage.MSG_ALBUMPAGE_RESUMED);
            }
            // TODO: fix this regression
            // mAlbumView.savePositions(PositionRepository.getInstance(mActivity));
            if (mInCameraApp) {
                super.onBackPressed();
            } else {
                onUpPressed();
            }
        }
    }

    private void onUpPressed() {
        if (mInCameraApp) {
            GalleryUtils.startGalleryActivity(mActivity);
        } else if (mActivity.getStateManager().getStateCount() > 1) {
            super.onBackPressed();
        } else if (mParentMediaSetString != null) {
            Bundle data = new Bundle(getData());
            data.putString(AlbumSetPage.KEY_MEDIA_PATH, mParentMediaSetString);
            mActivity.getStateManager().switchState(
                    this, AlbumSetPage.class, data);
        }
    }

    private void onDown(int index) {
        mAlbumView.setPressedIndex(index);
    }

    private void onUp(boolean followedByLongPress) {
        if (followedByLongPress) {
            // Avoid showing press-up animations for long-press.
            mAlbumView.setPressedIndex(-1);
        } else {
            mAlbumView.setPressedUp();
        }
    }

    private void onSingleTapUp(int slotIndex) {
        if (!mIsActive) return;

        if (mSelectionManager.inSelectionMode()) {
            MediaItem item = mAlbumDataAdapter.get(slotIndex);
            if (item == null) return; // Item not ready yet, ignore the click
            mSelectionManager.toggle(item.getPath());
            mSlotView.invalidate();
        } else {
            // Render transition in pressed state
            mAlbumView.setPressedIndex(slotIndex);
            mAlbumView.setPressedUp();
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_PICK_PHOTO, slotIndex, 0),
                    FadeTexture.DURATION);
        }
    }

    private void pickPhoto(int slotIndex) {
        pickPhoto(slotIndex, false);
    }

    private void pickPhoto(int slotIndex, boolean startInFilmstrip) {
        if (!mIsActive) return;

        if (!startInFilmstrip) {
            // Launch photos in lights out mode
            mActivity.getGLRoot().setLightsOutMode(true);
        }

        MediaItem item = mAlbumDataAdapter.get(slotIndex);
        if (item == null) return; // Item not ready yet, ignore the click
        if (mGetContent) {
            onGetContent(item);
        } else if (mLaunchedFromPhotoPage) {
            TransitionStore transitions = mActivity.getTransitionStore();
            transitions.put(
                    PhotoPage.KEY_ALBUMPAGE_TRANSITION,
                    PhotoPage.MSG_ALBUMPAGE_PICKED);
            transitions.put(PhotoPage.KEY_INDEX_HINT, slotIndex);
            onBackPressed();
        } else {
            // Get into the PhotoPage.
            // mAlbumView.savePositions(PositionRepository.getInstance(mActivity));
            Bundle data = new Bundle();
            data.putInt(PhotoPage.KEY_INDEX_HINT, slotIndex);
            data.putParcelable(PhotoPage.KEY_OPEN_ANIMATION_RECT,
                    mSlotView.getSlotRect(slotIndex, mRootPane));
            data.putString(PhotoPage.KEY_MEDIA_SET_PATH,
                    mMediaSetPath.toString());
            data.putString(PhotoPage.KEY_MEDIA_ITEM_PATH,
                    item.getPath().toString());
                //add for DRM feature: pass drm inclusio info to next ActivityState
                if (mIsDrmSupported || mIsStereoDisplaySupported) {
                    data.putInt(DrmHelper.DRM_INCLUSION, mMtkInclusion);
                }
            data.putInt(PhotoPage.KEY_ALBUMPAGE_TRANSITION,
                    PhotoPage.MSG_ALBUMPAGE_STARTED);
            data.putBoolean(PhotoPage.KEY_START_IN_FILMSTRIP,
                    startInFilmstrip);
            data.putBoolean(PhotoPage.KEY_IN_CAMERA_ROLL, mMediaSet.isCameraRoll());
            if (startInFilmstrip) {
                mActivity.getStateManager().switchState(this, PhotoPage.class, data);
            } else {
                mActivity.getStateManager().startStateForResult(
                            PhotoPage.class, REQUEST_PHOTO, data);
            }
        }
    }

    private void onGetContent(final MediaItem item) {
        DataManager dm = mActivity.getDataManager();
        Activity activity = mActivity;
        if (mData.getString(Gallery.EXTRA_CROP) != null) {
            // M: try handling MTK-specific pick-and-crop flow first
            if (!startMtkCropFlow(item)) {
                // TODO: Handle MtpImagew
                Uri uri = dm.getContentUri(item.getPath());
                Intent intent = new Intent(CropImage.ACTION_CROP, uri)
                        .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                        .putExtras(getData());
                if (mData.getParcelable(MediaStore.EXTRA_OUTPUT) == null) {
                    intent.putExtra(CropImage.KEY_RETURN_DATA, true);
                }
                activity.startActivity(intent);
                activity.finish();
            }
        } else {
            if (mIsStereoDisplaySupported) {
                boolean attachWithoutConversion = mData.getBoolean(
                    StereoHelper.ATTACH_WITHOUT_CONVERSION, false);
                Log.i(TAG,"onGetContent:attachWithoutConversion=" + 
                                               attachWithoutConversion);
                int subtype = item.getSubType();
                if (!attachWithoutConversion &&
                    (0 != (MediaObject.SUBTYPE_MPO_3D & subtype) ||
                     0 != (MediaObject.SUBTYPE_MPO_3D_PAN & subtype) ||
                     0 != (MediaObject.SUBTYPE_STEREO_JPS & subtype))) {
                    boolean pickAs2D = mData.getBoolean(
                        StereoHelper.KEY_GET_NO_STEREO_IMAGE, false);
                    Log.i(TAG,"onGetContent:pickAs2D="+pickAs2D);
                    showStereoPickDialog(item,pickAs2D);
                    return;
                }
            }
            Intent intent = new Intent(null, item.getContentUri())
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.setResult(REQUEST_CODE_ATTACH_IMAGE, intent);//mody by xiashuaishuai on 20150318
            activity.finish();
        }
    }

    private void showStereoPickDialog(MediaItem item, boolean pickAs2D) {
        int positiveCap = 0;
        int negativeCap = 0;
        int title = 0;
        int message = 0;
        if (pickAs2D) {
            positiveCap = android.R.string.ok;
            negativeCap = android.R.string.cancel;
            title = R.string.stereo3d_convert2d_dialog_title;
            message = R.string.stereo3d_share_convert_text_single;
        } else {
            positiveCap = R.string.stereo3d_attach_dialog_button_2d;
            negativeCap = R.string.stereo3d_attach_dialog_button_3d;
            title = R.string.stereo3d_attach_dialog_title;
            message = R.string.stereo3d_share_dialog_text_single;
        }
        final MediaItem fItem = item;
        final boolean onlyPickAs2D = pickAs2D;
        final AlertDialog.Builder builder =
                        new AlertDialog.Builder((Context)mActivity);

        Log.i(TAG,"showStereoPickDialog:fItem.getContentUri()=" +
                                              fItem.getContentUri());
        DialogInterface.OnClickListener clickListener =
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (DialogInterface.BUTTON_POSITIVE == which) {
                        convertAndPick(fItem);
                    } else {
                        if (!onlyPickAs2D) {
                            Activity activity = (Activity) mActivity;
                            activity.setResult(Activity.RESULT_OK,
                                    new Intent(null, fItem.getContentUri()));
                            activity.finish();
                        }
                    }
                    dialog.dismiss();
                }
            };
        builder.setPositiveButton(positiveCap, clickListener);
        builder.setNegativeButton(negativeCap, clickListener);
        builder.setTitle(title)
               .setMessage(message);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void convertAndPick(final MediaItem item) {
        Log.i(TAG,"convertAndPick(item="+item+")");
        if (mConvertUriTask != null) {
            mConvertUriTask.cancel();
        }
        //show converting dialog
        int messageId = R.string.stereo3d_convert2d_progress_text;
        mProgressDialog = ProgressDialog.show(
                ((Activity)mActivity), null, 
                ((Activity)mActivity).getString(messageId), true, false);
        //create a job that convert intents and start sharing intent.
        mConvertUriTask = mActivity.getThreadPool().submit(new Job<Void>() {
            public Void run(JobContext jc) {
                //the majer process!
                final JobContext fJc = jc;
                final Uri convertedUri = StereoConvertor.convertSingle(jc, 
                                            (Context)mActivity, item);
                //dismis progressive dialog when we done
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mConvertUriTask = null;
                        if (null != mProgressDialog) {
                            Log.v(TAG,"mConvertUriTask:dismis ProgressDialog");
                            mProgressDialog.dismiss();
                        }
                        //start new intent
                        if (!fJc.isCancelled() && null != convertedUri) {
                            Log.i(TAG,"convertAndPick:convertedUri="+convertedUri);
                            Activity activity = (Activity) mActivity;
                            activity.setResult(Activity.RESULT_OK, 
                                               new Intent(null, convertedUri));
                            activity.finish();
                        }
                    }
                });
                return null;
            }
        });
    }

    public void onLongTap(int slotIndex) {
        if (mGetContent) return;
        MediaItem item = mAlbumDataAdapter.get(slotIndex);
        if (item == null) return;
        mSelectionManager.setAutoLeaveSelectionMode(true);
        mSelectionManager.toggle(item.getPath());
        mSlotView.invalidate();
    }

    @Override
    public void doCluster(int clusterType) {
        String basePath = mMediaSet.getPath().toString();
        String newPath = FilterUtils.newClusterPath(basePath, clusterType);
        Bundle data = new Bundle(getData());
        data.putString(AlbumSetPage.KEY_MEDIA_PATH, newPath);
        if (mShowClusterMenu) {
            Context context = mActivity.getAndroidContext();
            data.putString(AlbumSetPage.KEY_SET_TITLE, mMediaSet.getName());
            data.putString(AlbumSetPage.KEY_SET_SUBTITLE,
                    GalleryActionBar.getClusterByTypeString(context, clusterType));
        }
        //add for DRM feature: pass drm inclusio info to next ActivityState
        if (mIsDrmSupported || mIsStereoDisplaySupported) {
            data.putInt(DrmHelper.DRM_INCLUSION, mMtkInclusion);
        }

        // mAlbumView.savePositions(PositionRepository.getInstance(mActivity));
        mActivity.getStateManager().startStateForResult(
                AlbumSetPage.class, REQUEST_DO_ANIMATION, data);
    }

    @Override
    protected void onCreate(Bundle data, Bundle restoreState) {
        super.onCreate(data, restoreState);
        mUserDistance = GalleryUtils.meterToPixel(USER_DISTANCE_METER);
        initializeViews();
        initializeData(data);
        mGetContent = data.getBoolean(Gallery.KEY_GET_CONTENT, false);
        mShowClusterMenu = data.getBoolean(KEY_SHOW_CLUSTER_MENU, false);
        /* Begin: added by yuanhuawei 20130809*/
	mParentSelectedAction = data.getInt(AlbumSetPage.KEY_SELECTED_CLUSTER_TYPE,
                FilterUtils.CLUSTER_BY_ALBUM);
        /* End: added by yuanhuawei 20130809*/
        mDetailsSource = new MyDetailsSource();
        Context context = mActivity.getAndroidContext();
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        // Enable auto-select-all for mtp album
        if (data.getBoolean(KEY_AUTO_SELECT_ALL)) {
            mSelectionManager.selectAll();
        }

        mLaunchedFromPhotoPage =
                mActivity.getStateManager().hasStateClass(PhotoPage.class);
        mInCameraApp = data.getBoolean(PhotoPage.KEY_APP_BRIDGE, false);

        mHandler = new SynchronizedHandler(mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_PICK_PHOTO: {
                        pickPhoto(message.arg1);
                        break;
                    }
                    default:
                        throw new AssertionError(message.what);
                }
            }
        };

      /* Begin: added by yuanhuawei 20130809*/
       RelativeLayout galleryRoot = (RelativeLayout) ((Activity) mActivity)
                .findViewById(R.id.gallery_root);
        if (galleryRoot != null) {
               mBottomNavigationBarControls = new IphoneAlbumBottomNavigationBar(this, mActivity, galleryRoot);
	      mAlbumTopBar = new IphoneAlbumTopBar(this, mActivity, galleryRoot);
        	}
       /* End: added by yuanhuawei 20130809*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsActive = true;

        mResumeEffect = mActivity.getTransitionStore().get(KEY_RESUME_ANIMATION);
        if (mResumeEffect != null) {
            mAlbumView.setSlotFilter(mResumeEffect);
            mResumeEffect.setPositionProvider(mPositionProvider);
            mResumeEffect.start();
        }

        setContentPane(mRootPane);

        /// M: put these code in onCreateActionBar @{
        //boolean enableHomeButton = (mActivity.getStateManager().getStateCount() > 1) |
                //mParentMediaSetString != null;
        //GalleryActionBar actionBar = mActivity.getGalleryActionBar();
        //actionBar.setDisplayOptions(enableHomeButton, false);
        //if (!mGetContent) {
        //    actionBar.enableAlbumModeMenu(GalleryActionBar.ALBUM_GRID_MODE_SELECTED, this);
        //}
        /// @}

        // Set the reload bit here to prevent it exit this page in clearLoadingBit().
        setLoadingBit(BIT_LOADING_RELOAD);
        mLoadingFailed = false;
        if (mSelectionManager != null && mSelectionManager.inSelectionMode()) {
            mNeedUpdateSelection = true;
        }
        mAlbumDataAdapter.resume();

        mAlbumView.resume();
        mAlbumView.setPressedIndex(-1);
        //mActionModeHandler.resume();

        /* Begin: added by yuanhuawei 2013089*/
        refreshBottomControlsWhenReady();
        refreshAlbumTopBarControlsWhenReady();
        updateBottomPopWindowActionVisibility();
        /* End: added by yuanhuaewi 20130809*/
	
        if (!mInitialSynced) {
            setLoadingBit(BIT_LOADING_SYNC);
            mSyncTask = mMediaSet.requestSync(this);
        }
        mInCameraAndWantQuitOnPause = mInCameraApp;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsActive = false;

        mAlbumView.setSlotFilter(null);

        mAlbumDataAdapter.pause();
        mAlbumView.pause();
        DetailsHelper.pause();
        if (!mGetContent) {
            mActivity.getGalleryActionBar().disableAlbumModeMenu(true);
        }

        /* Begin: added by yuanhuawei 20130809*/
        refreshBottomControlsWhenReady();
        refreshAlbumTopBarControlsWhenReady();
        if(mBottomSharePopWindow!= null){
		mBottomSharePopWindow.cleanup();
        		mBottomSharePopWindow = null;
        	}
        if(mBottomDeletePopWindow != null){
		mBottomDeletePopWindow.cleanup();
		mBottomDeletePopWindow = null;
	}
        /* End: added by yuanhuawei 20130809*/

        if (mSyncTask != null) {
            mSyncTask.cancel();
            mSyncTask = null;
            clearLoadingBit(BIT_LOADING_SYNC);
        }
        mActionModeHandler.pause();
        if (mSelectionManager != null && mSelectionManager.inSelectionMode()) {
            mSelectionManager.saveSelection();
            mNeedUpdateSelection = false;
        }
    }

    @Override
    protected void onDestroy() {
        /* Begin: added by yuanhuawei 20130809*/
        if (mBottomNavigationBarControls != null) mBottomNavigationBarControls.cleanup();
        if(mAlbumTopBar != null) mAlbumTopBar.cleanup();
        if(mBottomNavigationBarControls != null){
		mBottomNavigationBarControls.cleanup();
        		mBottomNavigationBarControls = null;
        	}
        /* End: added by yuanhuawei 20130809*/
        super.onDestroy();
        if (mAlbumDataAdapter != null) {
            mAlbumDataAdapter.setLoadingListener(null);
        }
    }

    /* Begin: added by yuanhuawei 20130809*/
        @Override
    public boolean canDisplayBottomControls() {
        return mIsActive && !mGetContent;
    }

    @Override
    public boolean canDisplayBottomControl(int control) {
	return mIsActive && !mGetContent;
    }

    @Override
    public void onBottomControlClicked(int control) {
        switch(control) {
            case R.id.iphone_album_bottom_navigation_bar_photos:
	       if(mParentSelectedAction != FilterUtils.CLUSTER_BY_TIME){
		       android.util.Log.d(TAG, "onBottomControlClicked ** photos ** mParentSelectedAction = " + mParentSelectedAction);
		       Intent result = new Intent();
	                result.putExtra(AlbumSetPage.KEY_SELECTED_CLUSTER_TYPE , FilterUtils.CLUSTER_BY_TIME);
				
	                setStateResult(Activity.RESULT_OK, result);
		       mActivity.getGalleryActionBar().disableAlbumModeMenu(true);
	                mActivity.getStateManager().finishState(this, false);
	       }
	       if(mBottomNavigationBarControls != null){
	       	mBottomNavigationBarControls.setSelectedId(control);
	       }
                return;
            case R.id.iphone_album_bottom_navigation_bar_albums:
	       if(mParentSelectedAction != FilterUtils.CLUSTER_BY_ALBUM){
		       android.util.Log.d(TAG, "onBottomControlClicked ** albums ** mParentSelectedAction = " + mParentSelectedAction);
		      Intent result = new Intent();
	               result.putExtra(AlbumSetPage.KEY_SELECTED_CLUSTER_TYPE , FilterUtils.CLUSTER_BY_ALBUM);
	               setStateResult(Activity.RESULT_OK, result);
		      mActivity.getGalleryActionBar().disableAlbumModeMenu(true);
	               mActivity.getStateManager().finishState(this, false);
	       }
	      if(mBottomNavigationBarControls != null){
	      	mBottomNavigationBarControls.setSelectedId(control);
	      }
                return;
	   case R.id.iphone_album_bottom_share:
	       RelativeLayout galleryRoot = (RelativeLayout) ((Activity) mActivity)
	                .findViewById(R.id.gallery_root);
	        if (galleryRoot != null) {
		      mBottomSharePopWindow = new IphoneBottomSharePopWindow(this, mActivity, galleryRoot);
		      if(mActionModeHandler != null){
			  	mBottomSharePopWindow.setShareIntent(mActionModeHandler.getSharedIntent());
		      }
	        	}
	       refreshSharePopBottomControlsWhenReady();
	       updateBottomPopWindowActionVisibility();
	       return;
	   case R.id.iphone_album_bottom_delete:
	       RelativeLayout galleryRoot_new = (RelativeLayout) ((Activity) mActivity)
	                .findViewById(R.id.gallery_root);
	        if (galleryRoot_new != null) {
		      mBottomDeletePopWindow = new IphoneBottomDeletePopWindow(this, mActivity, galleryRoot_new);
	        	}
	       refreshBottomDeleteControlsWhenReady();
	       return;
            default:
                return;
        }
    }

    @Override
    public void refreshBottomControlsWhenReady() {
        if (mBottomNavigationBarControls == null) {
            return;
        }
        mBottomNavigationBarControls.refresh();
        if(mParentSelectedAction == FilterUtils.CLUSTER_BY_ALBUM){
		mBottomNavigationBarControls.setSelectedId(R.id.iphone_album_bottom_navigation_bar_albums);
		if(mAlbumTopBar != null) mAlbumTopBar.setBackBtnText(true);
        }else{
        		mBottomNavigationBarControls.setSelectedId(R.id.iphone_album_bottom_navigation_bar_photos);
		if(mAlbumTopBar != null) mAlbumTopBar.setBackBtnText(false);
        }
    }

        @Override
    public boolean canSharePopDisplayBottomControls() {
        return mIsActive && !mGetContent;
    }

    @Override
    public boolean canSharePopDisplayBottomControl(int control) {
	return mIsActive && !mGetContent;
    }

    @Override
    public void onSharePopBottomControlClicked(int control) {
        switch(control) {
            case R.id.iphone_share_pop_action_to_slideshow:
	       android.util.Log.d(TAG, "onSharePopBottomControlClicked *** slideshow");
	       if (mSelectionManager.inSelectionMode()) {
	            mSelectionManager.leaveSelectionMode();
	        }
	       mInCameraAndWantQuitOnPause = false;
                Bundle data = new Bundle();
                data.putString(SlideshowPage.KEY_SET_PATH,
                        mMediaSetPath.toString());
                data.putBoolean(SlideshowPage.KEY_REPEAT, true);
                //add for DRM feature: pass drm inclusio info to next ActivityState
                if (mIsDrmSupported || mIsStereoDisplaySupported) {
                    data.putInt(DrmHelper.DRM_INCLUSION, mMtkInclusion);
                }
                mActivity.getStateManager().startStateForResult(
                        SlideshowPage.class, REQUEST_SLIDESHOW, data);
	       return;
	   case R.id.iphone_share_pop_action_to_contacts:
	       android.util.Log.d(TAG, "onSharePopBottomControlClicked *** contacts");
	       setAsContactsOrWallpaper(true);
	       return;
	   case R.id.iphone_share_pop_action_to_wallpaper:
	       android.util.Log.d(TAG, "onSharePopBottomControlClicked *** wallpaper");
	       setAsContactsOrWallpaper(false);
	       return;
	   case R.id.iphone_share_pop_action_to_edit:
	       android.util.Log.d(TAG, "onSharePopBottomControlClicked *** edit");
	       actionToEdit();
	       return;
	   case R.id.iphone_share_pop_action_to_rotate_left:
	       android.util.Log.d(TAG, "onSharePopBottomControlClicked *** rotate_left");
	        if(mActionModeHandler != null){
			mActionModeHandler.performClickRotate(true);
	        }
	       return;
	   case R.id.iphone_share_pop_action_to_rotate_right:
	       android.util.Log.d(TAG, "onSharePopBottomControlClicked *** rotate_right");
	        if(mActionModeHandler != null){
			mActionModeHandler.performClickRotate(false);
	        }
	       return;
	   case R.id.iphone_share_pop_action_to_clip:
	       android.util.Log.d(TAG, "onSharePopBottomControlClicked *** clip");	   
	       if(mActionModeHandler != null){
			mActionModeHandler.performClickCrop();
	        }
	       return;
            default:
                return;
        }
    }

    @Override
    public void refreshSharePopBottomControlsWhenReady() {
        android.util.Log.d(TAG, "notifySharePopCleanUp *** mBottomSharePopWindow = " + mBottomSharePopWindow);
        if (mBottomSharePopWindow == null) {
            return;
        }
        if(mBottomDeletePopWindow != null){
		mBottomDeletePopWindow.cleanup();
		mBottomDeletePopWindow = null;
	}
        mBottomSharePopWindow.refresh();
    }

    @Override
    public void notifySharePopCleanUp(){
    	android.util.Log.d(TAG, "notifySharePopCleanUp *** mBottomSharePopWindow = " + mBottomSharePopWindow);
    	if (mBottomSharePopWindow == null) {
            return;
        }
        mBottomSharePopWindow.cleanup();
        mBottomSharePopWindow = null;
    }

    public void updateBottomPopWindowActionVisibility(){
	if(mBottomSharePopWindow != null && mActionModeHandler != null){
		//ArrayList<Integer> visibleList = mActionModeHandler.getVisibleMenuItems();
		int count = mSelectionManager.getSelectedCount();
		ArrayList<Integer> visibleIconList  = new ArrayList<Integer>();
		if(count > 0){
			visibleIconList.add(R.id.iphone_share_pop_action_to_slideshow);
			if(count == 1){
				visibleIconList.add(R.id.iphone_share_pop_action_to_rotate_left);
				visibleIconList.add(R.id.iphone_share_pop_action_to_rotate_right);
				visibleIconList.add(R.id.iphone_share_pop_action_to_clip);
				visibleIconList.add(R.id.iphone_share_pop_action_to_contacts);
				visibleIconList.add(R.id.iphone_share_pop_action_to_wallpaper);
				visibleIconList.add(R.id.iphone_share_pop_action_to_edit);
			}else if(count < 5){
				visibleIconList.add(R.id.iphone_share_pop_action_to_rotate_left);
				visibleIconList.add(R.id.iphone_share_pop_action_to_rotate_right);
			}
		/*
		for(int i = 0; i < visibleList.size(); i++){
			switch(visibleList.get(i)){
			case R.id.action_rotate_ccw:
				visibleIconList.add(R.id.iphone_share_pop_action_to_rotate_left);
				break;
			case R.id.action_rotate_cw:
				visibleIconList.add(R.id.iphone_share_pop_action_to_rotate_right);
				break;
			case R.id.action_crop:
				visibleIconList.add(R.id.iphone_share_pop_action_to_clip);
				break;
			case R.id.action_setas:
				visibleIconList.add(R.id.iphone_share_pop_action_to_contacts);
				visibleIconList.add(R.id.iphone_share_pop_action_to_wallpaper);
				break;
			case R.id.action_edit:
				visibleIconList.add(R.id.iphone_share_pop_action_to_edit);
				break;
			}
		}
		*/
		}
		mBottomSharePopWindow.updateVisibleIcons(visibleIconList);
		mBottomSharePopWindow.setShareIntent(mActionModeHandler.getSharedIntent());
	}
    }

    public static String getMimeType(int type) {
        switch (type) {
            case MediaObject.MEDIA_TYPE_IMAGE :
                return GalleryUtils.MIME_TYPE_IMAGE;
            case MediaObject.MEDIA_TYPE_VIDEO :
                return GalleryUtils.MIME_TYPE_VIDEO;
            default: return GalleryUtils.MIME_TYPE_ALL;
        }
    }

   private void setAsContactsOrWallpaper(boolean isContacts){
   	       String targetActivityName = isContacts ? mShareContactsName : mShareWallpaperName;
   	       DataManager manager = mActivity.getDataManager();
	       ArrayList<Path> ids = mSelectionManager.getSelected(true);
        	       Utils.assertTrue(ids.size() == 1);
	       Path path = ids.get(0);
	       String mimeType = getMimeType(manager.getMediaType(path));
	       Intent intent = new Intent(Intent.ACTION_ATTACH_DATA).setDataAndType(manager.getContentUri(path), mimeType);
	       intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra("mimeType", intent.getType());
                Activity activity = mActivity;
	       Intent intentNew = Intent.createChooser(intent, null);

	        Parcelable targetParcelable = intentNew.getParcelableExtra(Intent.EXTRA_INTENT);
	        android.util.Log.d(TAG, "(targetParcelable instanceof Intent) = " + (targetParcelable instanceof Intent));
	        if (!(targetParcelable instanceof Intent)) {
	            return;
	        }
	        Intent target = (Intent)targetParcelable;
	      
	      PackageManager mPm = mActivity.getAndroidContext().getPackageManager();
	      target.setComponent(null);
	      List<ResolveInfo> mCurrentResolveList = mPm.queryIntentActivities(
                        target, PackageManager.MATCH_DEFAULT_ONLY
                        | 0);
	      ActivityInfo targetAi = null;
	      for(int j = 0; j < mCurrentResolveList.size(); j++){
		  ActivityInfo aj = mCurrentResolveList.get(j).activityInfo;
		  android.util.Log.d(TAG, "setAsContactsOrWallpaper *** mCurrentResolveList[" + j + "] = " + aj.applicationInfo.packageName
		  	+ " *** aj.name = " + aj.name);
		  if(targetActivityName.equals(aj.name)){
		  	targetAi = aj;
		  }
		  
	      }
	      if(targetAi != null){
		   target.setComponent(new ComponentName(
                    	targetAi.applicationInfo.packageName, targetAi.name));
		   target.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT
                    	|Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
		   if(target != null){
		  	activity.startActivity(target);
		   }
	       }
   } 

   private void actionToEdit(){
   	      /*
   	       DataManager manager = mActivity.getDataManager();
	       ArrayList<Path> ids = mSelectionManager.getSelected(true);
        	       Utils.assertTrue(ids.size() == 1);
                Path path = ids.get(0);
                MediaObject obj = manager.getMediaObject(path);
                Log.i(TAG,"actionToEdit:obj="+obj);
                Log.i(TAG,"actionToEdit:MediatekFeature.isStereoImage(obj)="+MediatekFeature.isStereoImage(obj));
                if (MediatekFeature.isStereoImage(obj)) {
                    String edit = ((Activity) mActivity).getString(R.string.edit);
                    String convertEdit = ((Activity) mActivity).getString(
                                     R.string.stereo3d_convert2d_dialog_text,edit);
                    clickStereoPhoto(action, listener, convertEdit);
                    return;
                }
	      */
	        DataManager manager = mActivity.getDataManager();
	        ArrayList<Path> ids = mSelectionManager.getSelected(true);
        	        Utils.assertTrue(ids.size() == 1);
                 Path path = ids.get(0);
	        String mimeType = getMimeType(manager.getMediaType(path));
	        Intent intent =  new Intent(Intent.ACTION_EDIT).setDataAndType(manager.getContentUri(path), mimeType);
                 intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                ((Activity) mActivity).startActivity(Intent.createChooser(intent, null));
   }

    @Override
   public boolean canDisplayDeleteBottomControls(){
   	return mIsActive && !mGetContent;
   }
    @Override
   public boolean canDisplayDeleteBottomControl(int control){
   	return mIsActive && !mGetContent;
   }
   @Override
   public void onBottomDeleteControlClicked(int control){
   	switch(control) {
            case R.id.iphone_delete_pop_delete_btn:
	       if(mActionModeHandler != null){
			mActionModeHandler.performClickDelete();
	        }
                return;
            default:
                return;
        }
   }
    @Override
   public void notifyDeletePopCleanUp(){
        if (mBottomDeletePopWindow == null) {
            return;
        }
        mBottomDeletePopWindow.cleanup();
        mBottomDeletePopWindow = null;
   }
   @Override
    public void refreshBottomDeleteControlsWhenReady(){
	if (mBottomDeletePopWindow == null) {
            return;
        }
        if(mBottomSharePopWindow != null){
		mBottomSharePopWindow.cleanup();
		mBottomSharePopWindow = null;
	}
        mBottomDeletePopWindow.refresh();
    }

    @Override
    public boolean canDisplayAlbumTopBarControls(){
    	return mIsActive;
    }
    @Override
    public void onAlbumTopBarControlClicked(int control){
        switch(control) {
            case R.id.iphone_album_top_select_btn:
	       android.util.Log.d(TAG, "onAlbumTopBarControlClicked *** select_btn");
	       mSelectionManager.setAutoLeaveSelectionMode(false);
                mSelectionManager.enterSelectionMode();
                return;
	   case R.id.iphone_album_top_cancel_select_btn:
	       android.util.Log.d(TAG, "onAlbumTopBarControlClicked *** cancel_select_btn");
	       if (mSelectionManager.inSelectionMode()) {
	            mSelectionManager.leaveSelectionMode();
	        }
	       return;
	   case R.id.iphone_album_top_cancel_btn:
	       android.util.Log.d(TAG, "onAlbumTopBarControlClicked *** cancel_btn");
	       mActivity.getGalleryActionBar().disableAlbumModeMenu(true);
	       mActivity.getStateManager().finishState(this, false);
                return;
	   case R.id.iphone_album_top_back_btn:
	       android.util.Log.d(TAG, "onAlbumTopBarControlClicked *** back_btn");
	       mActivity.getGalleryActionBar().disableAlbumModeMenu(true);
	       mActivity.getStateManager().finishState(this, false);
                return;
            default:
                return;
        }
    }
    @Override
    public void refreshAlbumTopBarControlsWhenReady(){
    	if (mAlbumTopBar== null) {
             return;
         }
         mAlbumTopBar.refresh();
    }

    /* End: added by yuanhuawei 20130809*/

    private void initializeViews() {
        mSelectionManager = new SelectionManager(mActivity, false);
        mSelectionManager.setSelectionListener(this);
        Config.AlbumPage config = Config.AlbumPage.get(mActivity);
        mSlotView = new SlotView(mActivity, config.slotViewSpec);
        mAlbumView = new AlbumSlotRenderer(mActivity, mSlotView,
                mSelectionManager, config.placeholderColor);
        mSlotView.setSlotRenderer(mAlbumView);
        mRootPane.addComponent(mSlotView);
        mSlotView.setListener(new SlotView.SimpleListener() {
            @Override
            public void onDown(int index) {
                AlbumPage.this.onDown(index);
            }

            @Override
            public void onUp(boolean followedByLongPress) {
                AlbumPage.this.onUp(followedByLongPress);
            }

            @Override
            public void onSingleTapUp(int slotIndex) {
                AlbumPage.this.onSingleTapUp(slotIndex);
            }

            @Override
            public void onLongTap(int slotIndex) {
                AlbumPage.this.onLongTap(slotIndex);
            }
        });
        mActionModeHandler = new ActionModeHandler(mActivity, mSelectionManager);
        mActionModeHandler.setActionModeListener(new ActionModeListener() {
            @Override
            public boolean onActionItemClicked(MenuItem item) {
                return onItemSelected(item);
            }
        });
    }

    private void initializeData(Bundle data) {
        //add drm info to MediaSetPath
        if (mIsDrmSupported || mIsStereoDisplaySupported) {
            mMtkInclusion = data.getInt(DrmHelper.DRM_INCLUSION, 
                                        DrmHelper.NO_DRM_INCLUSION);
            Log.i(TAG,"initializeData:mMtkInclusion="+mMtkInclusion);
            mMediaSetPath = Path.fromString(data.getString(KEY_MEDIA_PATH),
                                            mMtkInclusion);
            mMediaSetPath.setMtkInclusion(mMtkInclusion);
        } else {
            mMediaSetPath = Path.fromString(data.getString(KEY_MEDIA_PATH));
        }
        mParentMediaSetString = data.getString(KEY_PARENT_MEDIA_PATH);
        mMediaSet = mActivity.getDataManager().getMediaSet(mMediaSetPath);
        if (mMediaSet == null) {
            Utils.fail("MediaSet is null. Path = %s", mMediaSetPath);
        }
        mSelectionManager.setSourceMediaSet(mMediaSet);
        mAlbumDataAdapter = new AlbumDataLoader(mActivity, mMediaSet);
        mAlbumDataAdapter.setLoadingListener(new MyLoadingListener());
        mAlbumView.setModel(mAlbumDataAdapter);
    }

    private void showDetails() {
        mShowDetails = true;
        if (mDetailsHelper == null) {
            mDetailsHelper = new DetailsHelper(mActivity, mRootPane, mDetailsSource);
            mDetailsHelper.setCloseListener(new CloseListener() {
                @Override
                public void onClose() {
                    hideDetails();
                }
            });
        }
        mDetailsHelper.show();
    }

    private void hideDetails() {
        mShowDetails = false;
        mDetailsHelper.hide();
        mAlbumView.setHighlightItemPath(null);
        mSlotView.invalidate();
    }

    @Override
    protected boolean onCreateActionBar(Menu menu) {
        GalleryActionBar actionBar = mActivity.getGalleryActionBar();
        /* Begin: added by yuanhuawei 20130813*/
        actionBar.hide();
        /* End: added by yuanhuawei 20130813*/
        MenuInflater inflator = getSupportMenuInflater();
        /// M: put code related with ActionBar here, not in onResume @{
        boolean enableHomeButton = (mActivity.getStateManager().getStateCount() > 1)
                | mParentMediaSetString != null;
        actionBar.setDisplayOptions(enableHomeButton, false);
        /// @}
        
        if (mGetContent) {
            inflator.inflate(R.menu.pickup, menu);
            int typeBits = mData.getInt(Gallery.KEY_TYPE_BITS,
                    DataManager.INCLUDE_IMAGE);
            actionBar.setTitle(GalleryUtils.getSelectionModePrompt(typeBits));
	    /* Begin: added by yuanhuawei 20130809*/
        	    if (mBottomNavigationBarControls != null) mBottomNavigationBarControls.cleanup();
	    if(mAlbumTopBar != null) {
		   mAlbumTopBar.setAlbumTiltle(mActivity.getString(GalleryUtils.getSelectionModePrompt(typeBits)));
		   mAlbumTopBar.setCancelBtnVisible(true);
	     }
            /* End: added by yuanhuawei 20130809*/
        } else {
            inflator.inflate(R.menu.album, menu);
            actionBar.setTitle(mMediaSet.getName());
            /// M: put code related with ActionBar here, not in onResume @{
            // After gallery was killed when in AlbumPage, when restart,
            // the title of ActionBar shows not right, so we have to enableAlbumModeMenu after set right title
            actionBar.enableAlbumModeMenu(GalleryActionBar.ALBUM_GRID_MODE_SELECTED, this);
            /// @}
            android.util.Log.d(TAG, "onCreateActionBar ** action_slideshow visible = " +( !(mMediaSet instanceof MtpDevice)));
            menu.findItem(R.id.action_slideshow)
                    .setVisible(!(mMediaSet instanceof MtpDevice));

            FilterUtils.setupMenuItems(actionBar, mMediaSetPath, true);

            menu.findItem(R.id.action_group_by).setVisible(false/*mShowClusterMenu*/); //changed by yuanhuawei 20131105
            menu.findItem(R.id.action_camera).setVisible(
                    MediaSetUtils.isCameraSource(mMediaSetPath)
                    && GalleryUtils.isCameraAvailable(mActivity));
	   /* Begin: added by yuanhuawei 20130813*/
	   if(mAlbumTopBar != null) {
	   	mAlbumTopBar.setAlbumTiltle(mMediaSet.getName());
	   	mAlbumTopBar.setCancelBtnVisible(false);
	    }
	   /* End: added by yuanhuawei 20130813*/

        }
        actionBar.setSubtitle(null);
        return true;
    }

    private void prepareAnimationBackToFilmstrip(int slotIndex) {
        if (mAlbumDataAdapter == null || !mAlbumDataAdapter.isActive(slotIndex)) return;
        MediaItem item = mAlbumDataAdapter.get(slotIndex);
        if (item == null) return;
        TransitionStore transitions = mActivity.getTransitionStore();
        transitions.put(PhotoPage.KEY_INDEX_HINT, slotIndex);
        transitions.put(PhotoPage.KEY_OPEN_ANIMATION_RECT,
                mSlotView.getSlotRect(slotIndex, mRootPane));
    }

    private void switchToFilmstrip() {
        if (mAlbumDataAdapter.size() < 1) return;
        int targetPhoto = mSlotView.getVisibleStart();
        prepareAnimationBackToFilmstrip(targetPhoto);
        if(mLaunchedFromPhotoPage) {
            onBackPressed();
        } else {
            pickPhoto(targetPhoto, true);
        }
    }

    @Override
    protected boolean onItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onUpPressed();
                return true;
            }
            case R.id.action_cancel:
                mActivity.getStateManager().finishState(this);
                return true;
            case R.id.action_select:
                mSelectionManager.setAutoLeaveSelectionMode(false);
                mSelectionManager.enterSelectionMode();
                return true;
            case R.id.action_group_by: {
                mActivity.getGalleryActionBar().showClusterDialog(this);
                return true;
            }
            case R.id.action_slideshow: {
                mInCameraAndWantQuitOnPause = false;
                Bundle data = new Bundle();
                data.putString(SlideshowPage.KEY_SET_PATH,
                        mMediaSetPath.toString());
                data.putBoolean(SlideshowPage.KEY_REPEAT, true);
                //add for DRM feature: pass drm inclusio info to next ActivityState
                if (mIsDrmSupported || mIsStereoDisplaySupported) {
                    data.putInt(DrmHelper.DRM_INCLUSION, mMtkInclusion);
                }
                mActivity.getStateManager().startStateForResult(
                        SlideshowPage.class, REQUEST_SLIDESHOW, data);
                return true;
            }
            case R.id.action_details: {
                if (mShowDetails) {
                    hideDetails();
                } else {
                    showDetails();
                }
                return true;
            }
            case R.id.action_camera: {
                GalleryUtils.startCameraActivity(mActivity);
                return true;
            }
            default:
                return false;
        }
    }

    @Override
    protected void onStateResult(int request, int result, Intent data) {
        switch (request) {
            case REQUEST_SLIDESHOW: {
                // data could be null, if there is no images in the album
                if (data == null) return;
                mFocusIndex = data.getIntExtra(SlideshowPage.KEY_PHOTO_INDEX, 0);
                mSlotView.setCenterIndex(mFocusIndex);
                break;
            }
            case REQUEST_PHOTO: {
                if (data == null) return;
                mFocusIndex = data.getIntExtra(PhotoPage.KEY_RETURN_INDEX_HINT, 0);
                mSlotView.makeSlotVisible(mFocusIndex);
                break;
            }
            case REQUEST_DO_ANIMATION: {
                mSlotView.startRisingAnimation();
                break;
            }
            
            // M: default case is added for MTK-specific pick-and-crop flow
            default:
                handleMtkCropResult(request, result, data);
        }
    }

    @Override
    public void onSelectionModeChange(int mode) {
        switch (mode) {
            case SelectionManager.ENTER_SELECTION_MODE: {
                //mActionModeHandler.startActionMode();
                if (mHapticsEnabled) mVibrator.vibrate(100);
	       /* Begin: added by yuanhuawei 20130809*/
	       mAlbumTopBar.switchSelectLayout(true);
	       if (mBottomNavigationBarControls != null) mBottomNavigationBarControls.switchSlectedState(true);
	        updateBottomPopWindowActionVisibility();
	       /* End: added by yuanhuawei 20130809*/
                break;
            }
            case SelectionManager.LEAVE_SELECTION_MODE: {
                //mActionModeHandler.finishActionMode();
                mRootPane.invalidate();
	       /* Begin: added by yuanhuawei 20130809*/
	       mAlbumTopBar.setSelectTitle(null);
	       mAlbumTopBar.switchSelectLayout(false);
	       if (mBottomNavigationBarControls != null) mBottomNavigationBarControls.switchSlectedState(false);
		if(mBottomSharePopWindow!= null){
			mBottomSharePopWindow.cleanup();
	        		mBottomSharePopWindow = null;
        		}
		if(mBottomDeletePopWindow != null){
			mBottomDeletePopWindow.cleanup();
			mBottomDeletePopWindow = null;
		}
	       /* End: added by yuanhuawei 20130809*/
                break;
            }
            // M: when click deselect all in menu, not leave selection mode
            case SelectionManager.DESELECT_ALL_MODE:
	       /* Begin: added by yuanhuawei 20130810*/
	       mActionModeHandler.updateSupportedOperation();
                mRootPane.invalidate();
		    if (mBottomNavigationBarControls != null)  {
		       mBottomNavigationBarControls.switchSlectedState(true);
		       mBottomNavigationBarControls.switchEnableState(false);
	        }
	       mHandler.postDelayed(new Runnable() {
	                    @Override
	                    public void run() {
	                    	updateBottomPopWindowActionVisibility();
	                    }
          	}, 200);
	       break;
	       /* End: added by yuanhuawei 20130810*/
            case SelectionManager.SELECT_ALL_MODE: {
                mActionModeHandler.updateSupportedOperation();
                mRootPane.invalidate();
	       /* Begin: added by yuanhuawei 20130809*/
		    if (mBottomNavigationBarControls != null)  {
			   mBottomNavigationBarControls.switchSlectedState(true);
			   mBottomNavigationBarControls.switchEnableState(true);
	        }
	       mHandler.postDelayed(new Runnable() {
	                    @Override
	                    public void run() {
	                    	updateBottomPopWindowActionVisibility();
	                    }
           	}, 200);
	       /* End: added by yuanhuawei 20130809*/
                break;
            }
        }
    }

    @Override
    public void onSelectionChange(Path path, boolean selected) {
        int count = mSelectionManager.getSelectedCount();
        String format = mActivity.getResources().getQuantityString(
                R.plurals.number_of_items_selected, count);
        //mActionModeHandler.setTitle(String.format(format, count));
        mActionModeHandler.updateSupportedOperation(path, selected);
        /* Begin: added by yuanhuawei 20130810*/
        if(count != 0){
        	    mAlbumTopBar.setSelectTitle(String.format(format, count));
        }else{
        	    mAlbumTopBar.setSelectTitle(null);
        }
        if (mBottomNavigationBarControls != null) mBottomNavigationBarControls.switchEnableState(count != 0);
        mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                    	updateBottomPopWindowActionVisibility();
                    }
           }, 200);
        /* End: added by yuanhuawei 20130810*/
    }

    @Override
    public void onSyncDone(final MediaSet mediaSet, final int resultCode) {
        Log.d(TAG, "onSyncDone: " + Utils.maskDebugInfo(mediaSet.getName()) + " result="
                + resultCode);
        ((Activity) mActivity).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GLRoot root = mActivity.getGLRoot();
                root.lockRenderThread();
                mSyncResult = resultCode;
                try {
                    if (resultCode == MediaSet.SYNC_RESULT_SUCCESS) {
                        mInitialSynced = true;
                    }
                    clearLoadingBit(BIT_LOADING_SYNC);
                    showSyncErrorIfNecessary(mLoadingFailed);
                } finally {
                    root.unlockRenderThread();
                }
            }
        });
    }

    // Show sync error toast when all the following conditions are met:
    // (1) both loading and sync are done,
    // (2) sync result is error,
    // (3) the page is still active, and
    // (4) no photo is shown or loading fails.
    private void showSyncErrorIfNecessary(boolean loadingFailed) {
        if ((mLoadingBits == 0) && (mSyncResult == MediaSet.SYNC_RESULT_ERROR) && mIsActive
                && (loadingFailed || (mAlbumDataAdapter.size() == 0))) {
            Toast.makeText(mActivity, R.string.sync_album_error,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void setLoadingBit(int loadTaskBit) {
        mLoadingBits |= loadTaskBit;
    }

    private void clearLoadingBit(int loadTaskBit) {
        mLoadingBits &= ~loadTaskBit;
        if (mLoadingBits == 0 && mIsActive) {
            if (mAlbumDataAdapter.size() == 0) {
                Intent result = new Intent();
                result.putExtra(KEY_EMPTY_ALBUM, true);
                setStateResult(Activity.RESULT_OK, result);
                mActivity.getStateManager().finishState(this);
            }
        }
    }

    private class MyLoadingListener implements LoadingListener {
        @Override
        public void onLoadingStarted() {
            setLoadingBit(BIT_LOADING_RELOAD);
            mLoadingFailed = false;
        }

        @Override
        public void onLoadingFinished(boolean loadingFailed) {
            clearLoadingBit(BIT_LOADING_RELOAD);
            mLoadingFailed = loadingFailed;
            showSyncErrorIfNecessary(loadingFailed);
            
            // M: we have to notify SelectionManager about data change,
            // and this is the most proper place we could find till now
            boolean inSelectionMode = (mSelectionManager != null && mSelectionManager.inSelectionMode());
            int itemCount = mMediaSet != null ? mMediaSet.getMediaItemCount() : 0;
            MtkLog.d(TAG, "onLoadingFinished: item count=" + itemCount);
            mSelectionManager.onSourceContentChanged();
            if (itemCount > 0 && inSelectionMode) {
                if (mNeedUpdateSelection) {
                    mNeedUpdateSelection = false;
                    mSelectionManager.restoreSelection();
                }
                //mActionModeHandler.updateSupportedOperation();
                //mActionModeHandler.updateSelectionMenu();
	       /* Begin: added by yuanhuawei 20130812*/
	        updateBottomPopWindowActionVisibility();
	        /* End: added by yuanhuawei 20130812*/
            }
        }
    }

    private class MyDetailsSource implements DetailsHelper.DetailsSource {
        private int mIndex;

        @Override
        public int size() {
            return mAlbumDataAdapter.size();
        }

        @Override
        public int setIndex() {
            Path id = mSelectionManager.getSelected(false).get(0);
            mIndex = mAlbumDataAdapter.findItem(id);
            return mIndex;
        }

        @Override
        public MediaDetails getDetails() {
            // this relies on setIndex() being called beforehand
            MediaObject item = mAlbumDataAdapter.get(mIndex);
            if (item != null) {
                mAlbumView.setHighlightItemPath(item.getPath());
                return item.getDetails();
            } else {
                return null;
            }
        }
    }

    @Override
    public void onAlbumModeSelected(int mode) {
        if (mode == GalleryActionBar.ALBUM_FILMSTRIP_MODE_SELECTED) {
            switchToFilmstrip();
        }
    }
    // M: added for MTK-specific pick-and-crop flow
    private boolean startMtkCropFlow(final MediaItem item) {
        if (!MediatekFeature.MTK_CHANGE_PICK_CROP_FLOW) {
            return false;
        }
        
        mPickedItem = item;
        DataManager dm = mActivity.getDataManager();
        Activity activity = (Activity) mActivity;
        Uri uri = dm.getContentUri(item.getPath());
        // M: for MTK pick-and-crop flow, we do not forward activity result anymore;
        // instead, all crop results will be handled here in onStateResult
        Intent intent = new Intent(CropImage.ACTION_CROP, uri)
                .putExtras(getData());
        MtkLog.d(TAG, "startMtkCropFlow: EXTRA_OUTPUT=" + mData.getParcelable(MediaStore.EXTRA_OUTPUT));
        boolean cropForWallpaper = Wallpaper.EXTRA_CROP_FOR_WALLPAPER.equals(
                mData.getString(Gallery.EXTRA_CROP));
        boolean shouldReturnData = !cropForWallpaper && 
                (mData.getParcelable(MediaStore.EXTRA_OUTPUT) == null);
        if (shouldReturnData) {
            intent.putExtra(CropImage.KEY_RETURN_DATA, true);
            MtkLog.i(TAG, "startMtkCropFlow: KEY_RETURN_DATA");
        }
        
        if (cropForWallpaper) {
            activity.startActivityForResult(intent, REQUEST_CROP_WALLPAPER);
            MtkLog.d(TAG, "startMtkCropFlow: start for result: REQUEST_CROP_WALLPAPER");
        } else {
            activity.startActivityForResult(intent, REQUEST_CROP);
            MtkLog.d(TAG, "startMtkCropFlow: start for result: REQUEST_CROP");
        }
        return true;
    }
    
    // M: this holds the item picked in onGetContent
    private MediaItem mPickedItem;
    private void handleMtkCropResult(int request, int result, Intent data) {
        MtkLog.d(TAG, "handleMtkCropFlow: request=" + request + ", result=" + result + 
                ", dataString=" + (data != null ? data.getDataString() : "null"));
        switch (request) {
        case REQUEST_CROP:
            /* Fall through */
        case REQUEST_CROP_WALLPAPER:
            if (result == Activity.RESULT_OK) {
                // M: as long as the result is OK, we just setResult and finish
                Activity activity = (Activity) mActivity;
                // M: if data does not contain uri, we add the one we pick;
                // otherwise don't modify data
                if (data != null && mPickedItem != null) {
                    data.setDataAndType(mPickedItem.getContentUri(), data.getType());
                }
                activity.setResult(Activity.RESULT_OK, data);
                activity.finish();
            }
            break;
        default:
            MtkLog.w(TAG, "unknown MTK crop request!!");
        }
    }
    
}
