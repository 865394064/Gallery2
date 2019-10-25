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

import com.android.gallery3d.R;
import android.os.Handler;//addded by scq
import android.app.Activity;//addded by scq
import android.content.SharedPreferences;//addded by scq

import java.util.HashMap;
import java.util.Map;

public class IphoneBottomNavigationBar implements OnClickListener {
    public interface Delegate {
        public boolean canDisplayBottomControls();
        public boolean canDisplayBottomControl(int control);
        public void onBottomControlClicked(int control);
        public void refreshBottomControlsWhenReady();
	 public void onMemoryVisible();//added by scq
    }

    private static final String TAG = "IphoneBottomNavigationBar";
    private Delegate mDelegate;
    private ViewGroup mParentLayout;
    private ViewGroup mContainer;
    private ViewGroup mshareing;	
    private ViewGroup mMemory;//added by scq

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

    public IphoneBottomNavigationBar(Delegate delegate, Context context, RelativeLayout layout) {
        mDelegate = delegate;
        mParentLayout = layout;

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContainer = (ViewGroup) inflater
                .inflate(R.layout.iphone_bottom_navigation_bar, mParentLayout, false);
        mParentLayout.addView(mContainer);

	mshareing=(ViewGroup)inflater
		.inflate(R.layout.photo_gallery_sharing,mParentLayout,false);
	mParentLayout.addView(mshareing);
	mshareing.setVisibility(View.GONE);
	/*Begin: added by scq 20161201*/
	mySharedPreferences = context.getSharedPreferences("Memory", Activity.MODE_PRIVATE);
	mMemory=(ViewGroup)inflater.inflate(R.layout.photo_gallery_memory,mParentLayout,false);
	mParentLayout.addView(mMemory);
	mMemory.setVisibility(View.GONE);
	/*End: added by scq 20161201*/
	  
        TextView photoTextView = (TextView) mContainer.findViewById(R.id.iphone_bottom_navigation_bar_photos);
        photoTextView.setOnClickListener(this);
        mControlsVisible.put(photoTextView, false);
        TextView albumTextView = (TextView) mContainer.findViewById(R.id.iphone_bottom_navigation_bar_albums);
        albumTextView.setOnClickListener(this);
        mControlsVisible.put(albumTextView, false);
	 TextView sharingTextView=(TextView)mContainer.findViewById(R.id.share_icon);
	 sharingTextView.setOnClickListener(this);
	 mControlsVisible.put(sharingTextView,false);
	 /*begin: added by scq*/
	 TextView memoryTextView=(TextView)mContainer.findViewById(R.id.iphone_bottom_navigation_bar_memory);
	 memoryTextView.setOnClickListener(this);
	 mControlsVisible.put(memoryTextView,false);

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
   /* End: added by yuanhuawei 20130809*/

    @Override  
    public void onClick(View view) {
        if (mContainerVisible && mControlsVisible.get(view).booleanValue()) {
	     int viewID = view.getId();
            mDelegate.onBottomControlClicked(viewID);
		/*if(viewID ==(R.id.share_icon)){
	               mshareing.setVisibility(View.VISIBLE);
			 mMemory.setVisibility(View.GONE);//added by scq
		}else if(viewID ==(R.id.iphone_bottom_navigation_bar_memory)){//added by scq
	                  mshareing.setVisibility(View.GONE);//added by scq
	                  mMemory.setVisibility(View.VISIBLE);//added by scq
		}else{
	               mshareing.setVisibility(View.GONE);
			 mMemory.setVisibility(View.GONE);//added by scq
		}*/
        }
    }
	/*Begin: added by scq 20161201*/
    public void setMemoryVisible(boolean visible){
	if(visible){
	     if(mMemory != null)mMemory.setVisibility(View.VISIBLE);
	     setMemoryView();//added by scq
			}else{
	     if(mMemory != null)mMemory.setVisibility(View.GONE);
	}
    }

   public void setShareingVisible(boolean visible){
	if(visible){
	     if(mshareing != null)mshareing.setVisibility(View.VISIBLE);
	}else{
	     if(mshareing != null)mshareing.setVisibility(View.GONE);
	}
			}
	
    private RelativeLayout mNoPhotoMemory;
    private TextView iphoneMemoryStartUser;
    private RelativeLayout mMemoryProgress;
    private RelativeLayout mNoMemory;
    private boolean mIsNoMemory;
    private Handler mMemoryHandler;
    private void setMemoryView(){
	if(mMemory.getVisibility() == View.GONE || mMemory == null){
		return;
	}
	mNoPhotoMemory = (RelativeLayout) mMemory.findViewById(R.id.iphone_no_photo_memory);
	iphoneMemoryStartUser = (TextView) mMemory.findViewById(R.id.iphone_memory_start_user);
	mMemoryProgress = (RelativeLayout) mMemory.findViewById(R.id.iphone_memory_progress);
	mNoMemory = (RelativeLayout) mMemory.findViewById(R.id.iphone_no_memory);
	if(mySharedPreferences != null)mEditor = mySharedPreferences.edit();
	mIsNoMemory = mySharedPreferences.getBoolean("isNoMemory", false);
	if(mIsNoMemory){
		if(mNoPhotoMemory != null)mNoPhotoMemory.setVisibility(View.GONE);//added by scq
		if(mMemoryProgress != null)mMemoryProgress.setVisibility(View.GONE);//added by scq
		if(mNoMemory != null)mNoMemory.setVisibility(View.VISIBLE);//added by scq
	}else{
		if(mNoPhotoMemory != null)mNoPhotoMemory.setVisibility(View.VISIBLE);//added by scq
		if(mMemoryProgress != null)mMemoryProgress.setVisibility(View.GONE);//added by scq
		if(mNoMemory != null)mNoMemory.setVisibility(View.GONE);//added by scq
		mMemoryHandler = new Handler();
	}

	if(mNoPhotoMemory.getVisibility() == View.VISIBLE){
		iphoneMemoryStartUser.setOnClickListener(setNoMemoryOnClickListener);
	}
    }

    private View.OnClickListener setNoMemoryOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            	 mMemoryProgress.setVisibility(View.VISIBLE);//added by scq
	        mMemoryHandler.postDelayed(new Runnable() {
	            @Override
	            public void run() {
			if(mMemoryProgress != null)mMemoryProgress.setVisibility(View.GONE);//added by scq
			if(mNoMemory != null)mNoMemory.setVisibility(View.VISIBLE);//added by scq
			mIsNoMemory = true;
			mDelegate.onMemoryVisible();
	            }
	        }, 300);
		 if(mEditor != null){
		 	mEditor.putBoolean("isNoMemory", true);
        	 	mEditor.commit();
		 }
        }
    };

   private SharedPreferences mySharedPreferences;
   private SharedPreferences.Editor mEditor;

   public boolean getIsNoMemory(){
	return mIsNoMemory;
   }	
	/*End: added by scq 20161201*/
}
