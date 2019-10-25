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
import android.widget.LinearLayout;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;

import com.android.gallery3d.R;

import java.util.HashMap;
import java.util.Map;

public class IphonePhotoTopBar implements OnClickListener, OnTouchListener {
    public interface Delegate {
        public boolean canDisplayPhotoTopBarControls();
        public void onPhotoTopBarControlClicked(int control);
        public void refreshPhotoTopBarControlsWhenReady();
    }

    private static final String TAG = "IphonePhotoTopBar";
    private Delegate mDelegate;
    private ViewGroup mParentLayout;
    private ViewGroup mContainer;

    private TextView  mCurrentIndexText;
    private TextView mTotalCountText;
    private LinearLayout mTopTitleLayout;
    private Context mContext;
    private Button mDetailsBtn; //modify by scq
    private Button mCancenBtn;
    private Button mBackBtn;

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

    public IphonePhotoTopBar(Delegate delegate, Context context, RelativeLayout layout) {
        mDelegate = delegate;
        mParentLayout = layout;
        mContext = context;
    
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContainer = (ViewGroup) inflater
                .inflate(R.layout.iphone_photo_top_bar, mParentLayout, false);
        mContainer.setOnTouchListener(this); //added by yuanhuawei 20131109 
        mParentLayout.addView(mContainer);

        mCurrentIndexText = (TextView) mContainer.findViewById(R.id.iphone_photo_current_count);
        mTotalCountText = (TextView) mContainer.findViewById(R.id.iphone_photo_total_count);
        mTopTitleLayout = (LinearLayout) mContainer.findViewById(R.id.iphone_photo_top_progress_layout);
        
        mDetailsBtn = (Button) mContainer.findViewById(R.id.iphone_photo_top_details_btn); //modify by scq
        mDetailsBtn.setOnClickListener(this); //modify by scq
        mControlsVisible.put(mDetailsBtn, false); //modify by scq

        mCancenBtn = (Button) mContainer.findViewById(R.id.iphone_photo_top_cancel_btn);
        mCancenBtn.setOnClickListener(this);
        mControlsVisible.put(mCancenBtn, false);

	mBackBtn = (Button) mContainer.findViewById(R.id.iphone_photo_top_back_btn);
	mBackBtn.setOnClickListener(this);
         mControlsVisible.put(mBackBtn, false);

        /*for (int i = mContainer.getChildCount() - 1; i >= 0; i--) {
            View child = mContainer.getChildAt(i);
            child.setOnClickListener(this);
            mControlsVisible.put(child, false);
        }*/

        mContainerAnimIn.setDuration(CONTAINER_ANIM_DURATION_MS);
        mContainerAnimOut.setDuration(CONTAINER_ANIM_DURATION_MS);

        mDelegate.refreshPhotoTopBarControlsWhenReady();
    }

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		return true;
	}

    public void hide() {
        mContainer.clearAnimation();
        mContainerAnimOut.reset();
        mContainer.startAnimation(mContainerAnimOut);
        mContainer.setVisibility(View.INVISIBLE);
    }

    public void show() {
        mContainer.clearAnimation();
        mContainerAnimIn.reset();
        mContainer.startAnimation(mContainerAnimIn);
        mContainer.setVisibility(View.VISIBLE);
    }

    public void refresh() {
        boolean visible = mDelegate.canDisplayPhotoTopBarControls();
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
   public void setPhotoTiltle(String currentTitle, String totalTitle){
	if(currentTitle != null){
   		mCurrentIndexText.setText(currentTitle);
		mCurrentIndexText.setVisibility(View.VISIBLE);
	}else{
   		mCurrentIndexText.setText(null);
		mCurrentIndexText.setVisibility(View.GONE);
	}
	if(totalTitle != null){
		mTotalCountText.setText(totalTitle);
	}else{
   		mTotalCountText.setText(null);
		mTotalCountText.setVisibility(View.GONE);
	}
   }
   public void setPhotoTiltle(int currentIndex, int totalCount){
   	android.util.Log.d(TAG, "setAlbumSetTiltle *** currentIndex = " + currentIndex
		+ " *** totalCount = " + totalCount);
	if(currentIndex != 0){
   		mCurrentIndexText.setText(currentIndex + "");
	}
	if(totalCount != 0){
		mTotalCountText.setText(totalCount + "");
	}
   }

   public void setBackBtnText(String backText){
   	if(backText != null){
		if(!backText.equals(mContext.getResources().getString(R.string.folder_camera))){//added by scq
			mBackBtn.setText(null);//added by scq
		}else{
			mBackBtn.setText(backText);
		}
	}
   }

   public void setCancelBtnVisible(boolean visible){
   	if(visible){
		mBackBtn.setVisibility(View.GONE);
		mDetailsBtn.setVisibility(View.GONE); //modify by scq
		//mTopTitleLayout.setVisibility(View.GONE);
		mCancenBtn.setVisibility(View.VISIBLE);
	}else{
		mCancenBtn.setVisibility(View.GONE);
		mBackBtn.setVisibility(View.VISIBLE);
		mDetailsBtn.setVisibility(View.VISIBLE); //modify by scq
		//mTopTitleLayout.setVisibility(View.VISIBLE);
	}
   }
   /* End: added by yuanhuawei 20130809*/

    @Override
    public void onClick(View view) {
        android.util.Log.d(TAG, "onClick *** mContainerVisible = " + mContainerVisible);
        if (mContainerVisible/* && mControlsVisible.get(view).booleanValue()*/) {
            mDelegate.onPhotoTopBarControlClicked(view.getId());
        }
    }
   //Begin: zzd add for Bug3113 20140510
//WhenIntentFromMms
   public void setViewMmsMode(boolean isMode){
   	setEditBtnEnabled(!isMode);
	setTopTitleVisibility(!isMode);
   }
   private void setEditBtnEnabled(boolean isEnabled){
	mDetailsBtn.setEnabled(isEnabled); //modify by scq
	mDetailsBtn.setTextColor(0xffc2c2c2);//gray  disable color  //modify by scq
   }

   private void setTopTitleVisibility(boolean visibility){
   	if(visibility){
		mTopTitleLayout.setVisibility(View.VISIBLE);
   	}else{
   		mTopTitleLayout.setVisibility(View.GONE);
   	}
   }
//End: zzd add for Bug3113 20140510
}
