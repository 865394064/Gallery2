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
import android.view.View.OnTouchListener;
import android.view.MotionEvent;

import com.android.gallery3d.R;

import java.util.HashMap;
import java.util.Map;

public class IphoneBottomDeletePopWindow implements OnClickListener, OnTouchListener {
    public interface Delegate {
        public boolean canDisplayDeleteBottomControls();
        public boolean canDisplayDeleteBottomControl(int control);
        public void onBottomDeleteControlClicked(int control);
        public void refreshBottomDeleteControlsWhenReady();
        public void notifyDeletePopCleanUp();
    }

    private static final String TAG = "IphoneBottomDeletePopWindow";
    private Delegate mDelegate;
    private ViewGroup mParentLayout;
    private ViewGroup mContainer;

    private TextView mCancelTextView;
    private TextView mDescriptionTextView;
    private View mSeperatorView;

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

    public IphoneBottomDeletePopWindow(Delegate delegate, Context context, RelativeLayout layout) {
        mDelegate = delegate;
        mParentLayout = layout;

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContainer = (ViewGroup) inflater
                .inflate(R.layout.iphone_bottom_delete_pop_window, mParentLayout, false);
        mContainer.setOnTouchListener(this); //added by yuanhuawei 20131109 
        mParentLayout.addView(mContainer);

        mDescriptionTextView =  (TextView) mContainer.findViewById(R.id.iphone_delete_pop_delete_description);
       mSeperatorView =  mContainer.findViewById(R.id.iphone_delete_pop_delete_seperator);

        TextView deleteTextView = (TextView) mContainer.findViewById(R.id.iphone_delete_pop_delete_btn);
        deleteTextView.setOnClickListener(this);
        mControlsVisible.put(deleteTextView, false);
		
        mCancelTextView = (TextView) mContainer.findViewById(R.id.iphone_delete_pop_cancel);
        mCancelTextView.setOnClickListener(this);

        /*for (int i = mContainer.getChildCount() - 1; i >= 0; i--) {
            View child = mContainer.getChildAt(i);
            child.setOnClickListener(this);
            mControlsVisible.put(child, false);
        }*/

        mContainerAnimIn.setDuration(CONTAINER_ANIM_DURATION_MS);
        mContainerAnimOut.setDuration(CONTAINER_ANIM_DURATION_MS);

        mDelegate.refreshBottomDeleteControlsWhenReady();
    }

   	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		return true;
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
        boolean visible = mDelegate.canDisplayDeleteBottomControls();
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
            boolean curVisibility = mDelegate.canDisplayDeleteBottomControl(control.getId());
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

   /* Begin: added by yuanhuawei 20130815 */
   public void setHeaderVisible(boolean visible){
   	mDescriptionTextView.setVisibility(visible ? View.VISIBLE : View.GONE);
	mSeperatorView.setVisibility(visible ? View.VISIBLE : View.GONE);
   }
   /* End: added by yuanhuawei 20130815*/

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.iphone_delete_pop_cancel){
	   	mDelegate.notifyDeletePopCleanUp();
		return;
	}
        if (mContainerVisible && mControlsVisible.get(view).booleanValue()) {
            mDelegate.onBottomDeleteControlClicked(view.getId());
        }
    }
}
