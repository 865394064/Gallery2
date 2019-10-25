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
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.content.Intent;
import android.widget.ActivityChooserModel;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.Gravity;
import android.graphics.drawable.Drawable;

import com.android.gallery3d.R;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

public class IphoneBottomSharePopWindow implements OnClickListener {
    public interface Delegate {
        public boolean canSharePopDisplayBottomControls();
        public boolean canSharePopDisplayBottomControl(int control);
        public void onSharePopBottomControlClicked(int control);
        public void refreshSharePopBottomControlsWhenReady();
        public void notifySharePopCleanUp();
    }

    private static final String TAG = "IphoneBottomSharePopWindow";
    private Delegate mDelegate;
    private ViewGroup mParentLayout;
    private ViewGroup mContainer;

    private HorizontalScrollView mShareScrollContainer;
    private LinearLayout mShareScrollContainerLayout;
    private HorizontalScrollView mActionScrollContainer;
    private TextView mSlideshowTextView;
    private TextView mContactsTextView;
    private TextView mWallpaperTextView;
    private TextView mEditTextView;
    private TextView mRotateLeftTextView;
    private TextView mRotateRightTextView;
    private TextView mClipTextView;
    private TextView mCancelTextView;

    private Intent mShareIntent;
    private String mShareHistoryFileName = "share_history.xml";
    private Context mContext;
    private View.OnClickListener mShareIconClickListener;

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

    public IphoneBottomSharePopWindow(Delegate delegate, Context context, RelativeLayout layout) {
        mDelegate = delegate;
        mParentLayout = layout;
        mContext = context;

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContainer = (ViewGroup) inflater
                .inflate(R.layout.iphone_bottom_share_pop_window, mParentLayout, false);
        mParentLayout.addView(mContainer);

        mShareScrollContainer = (HorizontalScrollView) mContainer.findViewById(R.id.iphone_share_pop_share_scroll_view);
        mShareScrollContainerLayout = (LinearLayout) mContainer.findViewById(R.id.iphone_share_pop_share_scroll_view_container);
        mActionScrollContainer = (HorizontalScrollView) mContainer.findViewById(R.id.iphone_share_pop_action_scroll_view);

        mSlideshowTextView = (TextView) mContainer.findViewById(R.id.iphone_share_pop_action_to_slideshow);
        mSlideshowTextView.setOnClickListener(this);
        mControlsVisible.put(mSlideshowTextView, false);

	mContactsTextView = (TextView) mContainer.findViewById(R.id.iphone_share_pop_action_to_contacts);
	mContactsTextView.setOnClickListener(this);
         mControlsVisible.put(mContactsTextView, false);

	mWallpaperTextView = (TextView) mContainer.findViewById(R.id.iphone_share_pop_action_to_wallpaper);
	mWallpaperTextView.setOnClickListener(this);
         mControlsVisible.put(mWallpaperTextView, false);

	mEditTextView = (TextView) mContainer.findViewById(R.id.iphone_share_pop_action_to_edit);
	mEditTextView.setOnClickListener(this);
         mControlsVisible.put(mEditTextView, false);

	mRotateLeftTextView = (TextView) mContainer.findViewById(R.id.iphone_share_pop_action_to_rotate_left);
	mRotateLeftTextView.setOnClickListener(this);
         mControlsVisible.put(mRotateLeftTextView, false);

	mRotateRightTextView = (TextView) mContainer.findViewById(R.id.iphone_share_pop_action_to_rotate_right);
	mRotateRightTextView.setOnClickListener(this);
         mControlsVisible.put(mRotateRightTextView, false);

	mClipTextView = (TextView) mContainer.findViewById(R.id.iphone_share_pop_action_to_clip);
	mClipTextView.setOnClickListener(this);
         mControlsVisible.put(mClipTextView, false);

	mCancelTextView = (TextView) mContainer.findViewById(R.id.iphone_share_pop_cancel);
	mCancelTextView.setOnClickListener(this);

        /*for (int i = mContainer.getChildCount() - 1; i >= 0; i--) {
            View child = mContainer.getChildAt(i);
            child.setOnClickListener(this);
            mControlsVisible.put(child, false);
        }*/

        mContainerAnimIn.setDuration(CONTAINER_ANIM_DURATION_MS);
        mContainerAnimOut.setDuration(CONTAINER_ANIM_DURATION_MS);

        mDelegate.refreshSharePopBottomControlsWhenReady();

        /* Begin: added by yuanhuawei 20130812*/
        mShareIconClickListener = new OnClickListener() {
			
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			ActivityChooserModel dataModel = ActivityChooserModel.get(mContext,
                    		mShareHistoryFileName);
			int iconIndex = (Integer)v.getTag();
			Intent launchIntent = dataModel.chooseActivity(iconIndex);
			if (launchIntent != null) {
		                launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		                mContext.startActivity(launchIntent);
		          }
		}
	};
	/* End: added by yuanhuawei 20130812*/
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
        boolean visible = mDelegate.canSharePopDisplayBottomControls();
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
	/*
        for (View control : mControlsVisible.keySet()) {
            Boolean prevVisibility = mControlsVisible.get(control);
            boolean curVisibility = mDelegate.canSharePopDisplayBottomControl(control.getId());
            if (prevVisibility.booleanValue() != curVisibility) {
                if (!containerVisibilityChanged) {
                    control.clearAnimation();
                    control.startAnimation(getControlAnimForVisibility(curVisibility));
                }
                control.setVisibility(curVisibility ? View.VISIBLE : View.INVISIBLE);
                mControlsVisible.put(control, curVisibility);
            }
        }
	*/
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

   public void updateVisibleIcons(ArrayList<Integer > visibleList){
  	android.util.Log.d(TAG, "updateVisibleIcons **** visibleList.size() = " + visibleList.size());
	resetVisibleIcons();
   	for(int id : visibleList){
		for (View control : mControlsVisible.keySet()){
			if(control.getId() == id){
				control.setVisibility(View.VISIBLE);
				mControlsVisible.put(control, true);
			}
		}
	}
	
	mContainer.requestLayout();
   }

   public void resetVisibleIcons(){
   	for (View control : mControlsVisible.keySet()){
		control.setVisibility(View.GONE);
		mControlsVisible.put(control, false);
	}
	mContainer.requestLayout();
   }

   public void setShareIntent(Intent intent){
   	mShareIntent = intent;
	mShareScrollContainerLayout.removeAllViews();
	if(mShareIntent != null){
		ActivityChooserModel dataModel = ActivityChooserModel.get(mContext,
	            mShareHistoryFileName);
	        dataModel.setIntent(intent);
	        PackageManager packageManager = mContext.getPackageManager();
	        final int expandedActivityCount = dataModel.getActivityCount();
	        for (int i = 0; i < expandedActivityCount; i++){
			 ResolveInfo activity = dataModel.getActivity(i);
			 //activity.loadLabel(packageManager);
			 //activity.loadIcon(packageManager)
			 TextView textview = new TextView(mContext);

			/* Begin: changed by yuanhuawie 20130928 */
			 Drawable drawable = null;
			 /*if(activity.activityInfo.packageName.equals("com.facebook.katana")){
			 	drawable = mContext.getResources().getDrawable(R.drawable.app_facebook_icon);
			 }else{*/
			 drawable = activity.loadIcon(packageManager);
			 //}
			 drawable.setBounds(0, 0, 101, 101);
			 textview.setCompoundDrawablesRelative(null, drawable != null ? drawable : activity.loadIcon(packageManager), null, null);
			 /* End: changed by yuanhuawei 20130928 */
			 
			 textview.setTag(i);
			 textview.setPadding(10, 10, 10, 10);
			 textview.setGravity(Gravity.CENTER);
			 textview.setTextSize(12);
			 textview.setTextColor(R.color.iphone_share_pop_icon_text_color);
			 textview.setText(activity.loadLabel(packageManager));
			 textview.setOnClickListener(mShareIconClickListener);
			 mShareScrollContainerLayout.addView(textview);
	        }
	}
   }
   /* End: added by yuanhuawei 20130809*/

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.iphone_share_pop_cancel){
	   	mDelegate.notifySharePopCleanUp();
		return;
	}
        if (mContainerVisible && mControlsVisible.get(view).booleanValue()) {
            mDelegate.onSharePopBottomControlClicked(view.getId());
        }
    }
}
