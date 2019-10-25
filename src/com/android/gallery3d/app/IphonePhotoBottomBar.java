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

import android.os.Bundle;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;
import android.widget.ImageButton;
import com.android.gallery3d.R;
import android.annotation.SuppressLint;
import android.app.Activity;

import java.util.HashMap;
import java.util.Map;

public class IphonePhotoBottomBar extends Activity implements OnClickListener, OnTouchListener {
    public interface Delegate {
        public boolean canDisplayPhotoBottomBarControls();
        public void onPhotoBottomBarControlClicked(int control);
        public void refreshPhotoBottomBarControlsWhenReady();
    }

    private static final String TAG = "IphonePhotoBottomBar";
    private Delegate mDelegate;
    private ViewGroup mParentLayout;
    private ViewGroup mContainer;

    private ImageView mShareIcon;
    private ImageView mDeleteIcon;
    private ImageView mLoveIcon;
    private ImageView mEditText;

    private boolean mContainerVisible = false;
    private Map<View, Boolean> mControlsVisible = new HashMap<View, Boolean>();

    private Animation mContainerAnimIn = new AlphaAnimation(0f, 1f);
    private Animation mContainerAnimOut = new AlphaAnimation(1f, 0f);
    private static final int CONTAINER_ANIM_DURATION_MS = 200;
    private int ima=0;

    private static final int CONTROL_ANIM_DURATION_MS = 150;
    private static Animation getControlAnimForVisibility(boolean visible) {
        Animation anim = visible ? new AlphaAnimation(0f, 1f)
                : new AlphaAnimation(1f, 0f);
        anim.setDuration(CONTROL_ANIM_DURATION_MS);
        return anim;
    }

    public IphonePhotoBottomBar(Delegate delegate, Context context, RelativeLayout layout) {
        mDelegate = delegate;
        mParentLayout = layout;

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContainer = (ViewGroup) inflater
                .inflate(R.layout.iphone_photo_bottom_bar, mParentLayout, false);
        mContainer.setOnTouchListener(this); //added by yuanhuawei 20131109 
        mParentLayout.addView(mContainer);

        mShareIcon = (ImageView) mContainer.findViewById(R.id.iphone_photo_bottom_share);
        mShareIcon.setOnClickListener(this);
        mControlsVisible.put(mShareIcon, false);

        mEditText = (ImageView) mContainer.findViewById(R.id.iphone_photo_bottom_edit);
        mEditText.setOnClickListener(this);
        mControlsVisible.put(mEditText, false);

        mDeleteIcon = (ImageView) mContainer.findViewById(R.id.iphone_photo_bottom_delete);
        mDeleteIcon.setOnClickListener(this);
        mControlsVisible.put(mDeleteIcon, false);

        mLoveIcon=(ImageView) mContainer.findViewById(R.id.iphone_photo_bottom_love);
	  mLoveIcon.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
			 if(ima==0){    
				   mLoveIcon.setImageResource(R.drawable.iphone_gallery_love_unpress);
				   ima=1;
				} else{
                                    mLoveIcon.setImageResource(R.drawable.iphone_gallery_love_press);
					  ima=0;
					}
			}
		});
	 

        /*for (int i = mContainer.getChildCount() - 1; i >= 0; i--) {
            View child = mContainer.getChildAt(i);
            child.setOnClickListener(this);
            mControlsVisible.put(child, false);
        }*/

        mContainerAnimIn.setDuration(CONTAINER_ANIM_DURATION_MS);
        mContainerAnimOut.setDuration(CONTAINER_ANIM_DURATION_MS);

        mDelegate.refreshPhotoBottomBarControlsWhenReady();
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
        boolean visible = mDelegate.canDisplayPhotoBottomBarControls();
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
   
   /* End: added by yuanhuawei 20130809*/

    @Override
    public void onClick(View view) {
        if (mContainerVisible/* && mControlsVisible.get(view).booleanValue()*/) {
            mDelegate.onPhotoBottomBarControlClicked(view.getId());
        }
    }
//Begin: zzd add for Bug3113 20140510
//WhenIntentFromMms
   public void setViewMmsMode(boolean isMode){
   	enableShareAndDeleteBtn(!isMode);
   }

   private void enableShareAndDeleteBtn(boolean isEnabled){
	mShareIcon.setEnabled(isEnabled);
	mDeleteIcon.setEnabled(isEnabled);
   }
//End: zzd add for Bug3113 20140510
}
