/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Button;

import com.android.gallery3d.R;

import java.util.HashMap;
import java.util.Map;

public class IphoneAlbumTopBar implements OnClickListener {
    public interface Delegate {
        public boolean canDisplayAlbumTopBarControls();
        public void onAlbumTopBarControlClicked(int control);
        public void refreshAlbumTopBarControlsWhenReady();
    }

    private static final String TAG = "IphoneAlbumTopBar";
    private Delegate mDelegate;
    private ViewGroup mParentLayout;
    private ViewGroup mContainer;

    private TextView  mAlbumTitle;
    private Context mContext;
    private Button mSelectBtn;
    private Button mCancenBtn;
    private Button mBackBtn;
    private ViewGroup mNormalContainer;
    private ViewGroup mSelectContainer;
    private TextView mSelectItemsTitle;
    private Button mCancelSelectBtn;

    private boolean mContainerVisible = false;
    private Map<View, Boolean> mControlsVisible = new HashMap<View, Boolean>();

    private Animation mContainerAnimIn = new AlphaAnimation(0f, 1f);
    private Animation mContainerAnimOut = new AlphaAnimation(1f, 0f);
    private static final int CONTAINER_ANIM_DURATION_MS = 200;

    private static final int CONTROL_ANIM_DURATION_MS = 150;
    private static Animation getControlAnimForVisibility(boolean visible) {
        Animation anim = visible ? new AlphaAnimation(0f, 1f)
                : new AlphaAnimation(1f, 0f);
        anim.setDuration(CONTROL_ANIM_DURATION_MS);
        return anim;
    }

    public IphoneAlbumTopBar(Delegate delegate, Context context, RelativeLayout layout) {
        mDelegate = delegate;
        mParentLayout = layout;
        mContext = context;
    
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContainer = (ViewGroup) inflater
                .inflate(R.layout.iphone_album_top_bar, mParentLayout, false);
        mParentLayout.addView(mContainer);

        mNormalContainer = (ViewGroup) mContainer.findViewById(R.id.iphone_album_top_normal_layout);
        mSelectContainer = (ViewGroup) mContainer.findViewById(R.id.iphone_album_top_select_layout);

        mAlbumTitle = (TextView) mContainer.findViewById(R.id.iphone_album_title);
        mSelectBtn = (Button) mContainer.findViewById(R.id.iphone_album_top_select_btn);
        mSelectBtn.setOnClickListener(this);
        mControlsVisible.put(mSelectBtn, false);

        mCancenBtn = (Button) mContainer.findViewById(R.id.iphone_album_top_cancel_btn);
        mCancenBtn.setOnClickListener(this);
        mControlsVisible.put(mCancenBtn, false);

	mBackBtn = (Button) mContainer.findViewById(R.id.iphone_album_top_back_btn);
	mBackBtn.setOnClickListener(this);
         mControlsVisible.put(mBackBtn, false);

	mSelectItemsTitle = (TextView) mContainer.findViewById(R.id.iphone_album_select_count);
	mCancelSelectBtn = (Button) mContainer.findViewById(R.id.iphone_album_top_cancel_select_btn);
	mCancelSelectBtn.setOnClickListener(this);
	mControlsVisible.put(mCancelSelectBtn, false);

        /*for (int i = mContainer.getChildCount() - 1; i >= 0; i--) {
            View child = mContainer.getChildAt(i);
            child.setOnClickListener(this);
            mControlsVisible.put(child, false);
        }*/

        mContainerAnimIn.setDuration(CONTAINER_ANIM_DURATION_MS);
        mContainerAnimOut.setDuration(CONTAINER_ANIM_DURATION_MS);

        mDelegate.refreshAlbumTopBarControlsWhenReady();
    }

    private void hide() {
        mContainer.clearAnimation();
        mContainerAnimOut.reset();
        mContainer.startAnimation(mContainerAnimOut);
        mContainer.setVisibility(View.INVISIBLE);
    }

    private void show() {
        mContainer.clearAnimation();
        mContainerAnimIn.reset();
        mContainer.startAnimation(mContainerAnimIn);
        mContainer.setVisibility(View.VISIBLE);
    }

    public void refresh() {
        boolean visible = mDelegate.canDisplayAlbumTopBarControls();
        boolean containerVisibilityChanged = (visible != mContainerVisible);
        if (containerVisibilityChanged) {
            if (visible) {
                show();
            } else {
                hide();
            }
            mContainerVisible = visible;
        }
        if (!mContainerVisible) {
            return;
        }
        // Force a layout change
        mContainer.requestLayout(); // Kick framework to draw the control.
    }

    public void cleanup() {
        mParentLayout.removeView(mContainer);
        mControlsVisible.clear();
    }

   /* Begin: added by yuanhuawei 20130809*/
   public void setAlbumTiltle(String title){
   	android.util.Log.d(TAG, "setAlbumSetTiltle *** title = " + title);
	if(title != null){
   		mAlbumTitle.setText(title);
	}
   }

   public void setBackBtnText(boolean isAlbum){
   	if(isAlbum){
		mBackBtn.setText(mContext.getResources().getString(R.string.iphone_bottom_navigation_bar_albums_text));
	}else{
		mBackBtn.setText(mContext.getResources().getString(R.string.iphone_bottom_navigation_bar_photos_text));
	}
   }

   public void setCancelBtnVisible(boolean visible){
   	if(visible){
		mBackBtn.setVisibility(View.GONE);
		mSelectBtn.setVisibility(View.GONE);
		mCancenBtn.setVisibility(View.VISIBLE);
	}else{
		mCancenBtn.setVisibility(View.GONE);
		mBackBtn.setVisibility(View.VISIBLE);
		mSelectBtn.setVisibility(View.VISIBLE);
	}
   }

   public void switchSelectLayout(boolean isInSelected){
   	if(isInSelected){
		mNormalContainer.setVisibility(View.GONE);
		mSelectContainer.setVisibility(View.VISIBLE);
	}else{
		mSelectContainer.setVisibility(View.GONE);
		mNormalContainer.setVisibility(View.VISIBLE);
	}
   }

   public void setSelectTitle(String title){
   	if(title == null){
		mSelectItemsTitle.setText(R.string.iphone_album_select_count_default_text);
	}else{
		mSelectItemsTitle.setText(title);
	}
   }
   /* End: added by yuanhuawei 20130809*/

    @Override
    public void onClick(View view) {
        android.util.Log.d(TAG, "onClick *** mContainerVisible = " + mContainerVisible);
        if (mContainerVisible/* && mControlsVisible.get(view).booleanValue()*/) {
            mDelegate.onAlbumTopBarControlClicked(view.getId());
        }
    }
}
