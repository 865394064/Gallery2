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
import android.widget.ImageView;

import com.android.gallery3d.R;

import java.util.HashMap;
import java.util.Map;

public class IphoneAlbumBottomNavigationBar implements OnClickListener {
    public interface Delegate {
        public boolean canDisplayBottomControls();
        public boolean canDisplayBottomControl(int control);
        public void onBottomControlClicked(int control);
        public void refreshBottomControlsWhenReady();
    }

    private static final String TAG = "IphoneAlbumBottomNavigationBar";
    private Delegate mDelegate;
    private ViewGroup mParentLayout;
    private ViewGroup mContainer;

    private ViewGroup mNormalLayout;
    private ViewGroup mSelectLayout;
    private boolean mIsInSelected = false;
    private ImageView mShareIcon;
    private ImageView mDeleteIcon;
    private TextView mAddToText;

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

    public IphoneAlbumBottomNavigationBar(Delegate delegate, Context context, RelativeLayout layout) {
        mDelegate = delegate;
        mParentLayout = layout;

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContainer = (ViewGroup) inflater
                .inflate(R.layout.iphone_album_bottom_navigation_bar, mParentLayout, false);
        mParentLayout.addView(mContainer);

	mNormalLayout = (ViewGroup) mContainer.findViewById(R.id.iphone_album_bottom_navigation_bar_layout_normal);
	mSelectLayout = (ViewGroup) mContainer.findViewById(R.id.iphone_album_bottom_navigation_bar_layout_select);
	if(mIsInSelected){
		mNormalLayout.setVisibility(View.GONE);
		mSelectLayout.setVisibility(View.VISIBLE);
	}else{
		mSelectLayout.setVisibility(View.GONE);
		mNormalLayout.setVisibility(View.VISIBLE);
	}

        TextView photoTextView = (TextView) mContainer.findViewById(R.id.iphone_album_bottom_navigation_bar_photos);
        photoTextView.setOnClickListener(this);
        mControlsVisible.put(photoTextView, false);
        TextView albumTextView = (TextView) mContainer.findViewById(R.id.iphone_album_bottom_navigation_bar_albums);
        albumTextView.setOnClickListener(this);
        mControlsVisible.put(albumTextView, false);

        mShareIcon = (ImageView) mContainer.findViewById(R.id.iphone_album_bottom_share);
        mShareIcon.setOnClickListener(this);
        mControlsVisible.put(mShareIcon, false);

        mAddToText = (TextView) mContainer.findViewById(R.id.iphone_album_bottom_add_to);
        mAddToText.setOnClickListener(this);
        mControlsVisible.put(mAddToText, false);

        mDeleteIcon = (ImageView) mContainer.findViewById(R.id.iphone_album_bottom_delete);
        mDeleteIcon.setOnClickListener(this);
        mControlsVisible.put(mDeleteIcon, false);

        /*for (int i = mContainer.getChildCount() - 1; i >= 0; i--) {
            View child = mContainer.getChildAt(i);
            child.setOnClickListener(this);
            mControlsVisible.put(child, false);
        }*/

        mContainerAnimIn.setDuration(CONTAINER_ANIM_DURATION_MS);
        mContainerAnimOut.setDuration(CONTAINER_ANIM_DURATION_MS);

        mDelegate.refreshBottomControlsWhenReady();
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
        boolean visible = mDelegate.canDisplayBottomControls();
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
        for (View control : mControlsVisible.keySet()) {
            Boolean prevVisibility = mControlsVisible.get(control);
            boolean curVisibility = mDelegate.canDisplayBottomControl(control.getId());
            if (prevVisibility.booleanValue() != curVisibility) {
                if (!containerVisibilityChanged) {
                    control.clearAnimation();
                    control.startAnimation(getControlAnimForVisibility(curVisibility));
                }
                control.setVisibility(curVisibility ? View.VISIBLE : View.INVISIBLE);
                mControlsVisible.put(control, curVisibility);
            }
        }
        // Force a layout change
        mContainer.requestLayout(); // Kick framework to draw the control.
    }

    public void cleanup() {
        mParentLayout.removeView(mContainer);
        mControlsVisible.clear();
    }

   /* Begin: added by yuanhuawei 20130809*/
   public void setSelectedId(int id){
	for (View control : mControlsVisible.keySet()){
		if(id == control.getId()){
			control.setSelected(true);
		}else{
			control.setSelected(false);
		}
	}
   }

   public void switchSlectedState(boolean inSelected){
   	mIsInSelected = inSelected;
	if(mIsInSelected){
		mNormalLayout.setVisibility(View.GONE);
		mSelectLayout.setVisibility(View.VISIBLE);
		mShareIcon.setEnabled(false);
		mDeleteIcon.setEnabled(false);
		mAddToText.setEnabled(false);
	}else{
		mSelectLayout.setVisibility(View.GONE);
		mNormalLayout.setVisibility(View.VISIBLE);
	}
   }

   public void switchEnableState(boolean enable){
   	mShareIcon.setEnabled(enable);
	mDeleteIcon.setEnabled(enable);
	mAddToText.setEnabled(enable);
   }
   /* End: added by yuanhuawei 20130809*/

    @Override
    public void onClick(View view) {
        if (mContainerVisible && mControlsVisible.get(view).booleanValue()) {
            mDelegate.onBottomControlClicked(view.getId());
        }
    }
}
