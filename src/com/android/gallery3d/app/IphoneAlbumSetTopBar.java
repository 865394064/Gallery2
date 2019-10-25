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
import android.widget.ImageButton;

import com.android.gallery3d.R;
import com.android.gallery3d.ui.CustomDialog;
import java.util.HashMap;
import java.util.Map;
import android.app.DialogFragment;
import android.content.DialogInterface;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.app.CropImage;

public class IphoneAlbumSetTopBar implements OnClickListener {
    public interface Delegate {
        public boolean canDisplayAlbumSetTopBarControls();
        public void onAlbumSetTopBarControlClicked(int control);
        public void refreshAlbumSetTopBarControlsWhenReady();
	 public void onFinish();//added by scq	
    }

    private static final String TAG = "IphoneAlbumSetTopBar";
    private Delegate mDelegate;
    private ViewGroup mParentLayout;
    private ViewGroup mContainer;

    private TextView  mAlbumSetTitle;
    private Context mContext;
    private Button mCancelBtn;
    //Begin:added by xiashuaishuai on 20150112	
    private ImageButton mSearchBtn;
    private TextView mEditBtn;	
    //End:added by xiashuaishuai on 20150112
    private boolean mContainerVisible = false;
    private Map<View, Boolean> mControlsVisible = new HashMap<View, Boolean>();

    private Animation mContainerAnimIn = new AlphaAnimation(0f, 1f);
    private Animation mContainerAnimOut = new AlphaAnimation(1f, 0f);
    private static final int CONTAINER_ANIM_DURATION_MS = 200;

    private static final int CONTROL_ANIM_DURATION_MS = 150;
	private ImageButton maddbtn; // add by gaojunbin 
	private ImageButton mBackBtn;//added by scq
    private static Animation getControlAnimForVisibility(boolean visible) {
        Animation anim = visible ? new AlphaAnimation(0f, 1f)
                : new AlphaAnimation(1f, 0f);
        anim.setDuration(CONTROL_ANIM_DURATION_MS);
        return anim;
    }

    public IphoneAlbumSetTopBar(Delegate delegate, Context context, RelativeLayout layout) {
        mDelegate = delegate;
        mParentLayout = layout;
        mContext = context;
    
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContainer = (ViewGroup) inflater
                .inflate(R.layout.iphone_album_set_top_bar, mParentLayout, false);
        mParentLayout.addView(mContainer);
	//Begin:added by xiashuaishuai on 20150112	
	mSearchBtn=(ImageButton)mContainer.findViewById(R.id.btn_iphone_search);
	mEditBtn=(TextView)mContainer.findViewById(R.id.btn_iphone_edit);
	 //End:added by xiashuaishuai on 20150112
        mAlbumSetTitle = (TextView) mContainer.findViewById(R.id.iphone_album_set_title);
        mCancelBtn = (Button) mContainer.findViewById(R.id.iphone_album_set_top_cancel_btn);
        mCancelBtn.setOnClickListener(this);
        mControlsVisible.put(mCancelBtn, false);
	 maddbtn=(ImageButton)mContainer.findViewById(R.id.btn_iphone_add);  // add by gaojunbin 20150128
	 mBackBtn = (ImageButton)mContainer.findViewById(R.id.btn_iphone_back);//added by scq
         /**begin add by gaojunbin **/
	 mBackBtn.setOnClickListener(new OnClickListener(){
		@Override
		public void onClick(View view) {
			mDelegate.onFinish();
		}
	  }); 
		/** 
	 maddbtn.setOnClickListener(new OnClickListener(){
                  @Override
			public void onClick(View arg0) {
			final CustomDialog.Builder builder = new CustomDialog.Builder(mContext);
			     builder.setMessage(mContext.getResources().getString(R.string.newname_gallery_video));
		            builder.setTitle(mContext.getResources().getString(R.string.new_gallery_video));
			builder.setPositiveButton(mContext.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int which) {
			         //FilterShowActivity  filetrshow=(FilterShowActivity)mContext;
			  CropImage newimage=new CropImage();
                           
				dialog.dismiss();		  
			}
		}); 
			builder.setNegativeButton(mContext.getResources().getString(R.string.cancel),
				new android.content.DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				builder.create().show();
				
			}
              
	 });
	   **/
		 /**end add by gaojunbin **/
        /*for (int i = mContainer.getChildCount() - 1; i >= 0; i--) {
            View child = mContainer.getChildAt(i);
            child.setOnClickListener(this);
            mControlsVisible.put(child, false);
        }*/

        mContainerAnimIn.setDuration(CONTAINER_ANIM_DURATION_MS);
        mContainerAnimOut.setDuration(CONTAINER_ANIM_DURATION_MS);

        mDelegate.refreshAlbumSetTopBarControlsWhenReady();
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
        boolean visible = mDelegate.canDisplayAlbumSetTopBarControls();
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
   public void setAlbumSetTiltle(String title){
   	android.util.Log.d(TAG, "setAlbumSetTiltle *** title = " + title);
	if(title != null){
   		mAlbumSetTitle.setText(title);
	}
   }

   public void setAlbumSetTiltle(boolean isAlbum){
   	if(isAlbum){
		mAlbumSetTitle.setText(mContext.getResources().getString(R.string.iphone_bottom_navigation_bar_albums_text));
	}else{
		mAlbumSetTitle.setText(mContext.getResources().getString(R.string.iphone_bottom_navigation_bar_photos_text));
	}
   }

   public void setAlbumSetTiltle(int isAlbumNumber){
   	switch(isAlbumNumber){
		case 1:{
			mAlbumSetTitle.setText(mContext.getResources().getString(R.string.iphone_bottom_navigation_bar_albums_text));
			break;
		}
		case 2:{
			mAlbumSetTitle.setText(mContext.getResources().getString(R.string.photo_camera_gallery_collections));
			break;
		}
		case 3:{
			mAlbumSetTitle.setText(mContext.getResources().getString(R.string.iphone_gallery_icloud_title));
			break;
		}
		case 4:{
			mAlbumSetTitle.setText(mContext.getResources().getString(R.string.iphone_gallery_add_photo_memory));
			break;
		}
		default:
			break;
	}
   }
   public void setCancelBtnVisible(boolean visible){
   	if(visible){
		mCancelBtn.setVisibility(View.VISIBLE);
		//Begin:added by xiashuaishuai on 20150112
		mSearchBtn.setVisibility(View.GONE);
		mEditBtn.setVisibility(View.GONE);
		mBackBtn.setVisibility(View.GONE);//added by scq
	}else{	
		//mSearchBtn.setVisibility(View.VISIBLE);
		//mEditBtn.setVisibility(View.VISIBLE);
		//End:added by xiashuaishuai on 20150112
		mCancelBtn.setVisibility(View.GONE);
   /*Begin: added by scq 20161201*/
		switch(selectMode){
		case 1:{
			mContainer.setVisibility(View.VISIBLE);
		mSearchBtn.setVisibility(View.VISIBLE);
			maddbtn.setVisibility(View.VISIBLE);
		mEditBtn.setVisibility(View.VISIBLE);
			mBackBtn.setVisibility(View.GONE);//added by scq
			break;
		}
		case 2:{
			mContainer.setVisibility(View.VISIBLE);
			mSearchBtn.setVisibility(View.VISIBLE);
			maddbtn.setVisibility(View.GONE);
			mEditBtn.setVisibility(View.GONE);
			mBackBtn.setVisibility(View.VISIBLE);//added by scq
			break;
		}
		case 3:{
			mContainer.setVisibility(View.VISIBLE);
			mSearchBtn.setVisibility(View.GONE);
			maddbtn.setVisibility(View.GONE);
			mEditBtn.setVisibility(View.GONE);
			mBackBtn.setVisibility(View.GONE);//added by scq
			break;
		}
		case 4:{
			if(!mIsNoMemory){
				mContainer.setVisibility(View.GONE);
				mBackBtn.setVisibility(View.GONE);//added by scq
			}else{
				mContainer.setVisibility(View.VISIBLE);
				mSearchBtn.setVisibility(View.VISIBLE);
				maddbtn.setVisibility(View.GONE);
				mEditBtn.setVisibility(View.GONE);
				mBackBtn.setVisibility(View.GONE);//added by scq
			}
			break;
		}
		default:
			break;
		}
	}
   }
   /* End: added by yuanhuawei 20130809*/

    @Override
    public void onClick(View view) {
        if (mContainerVisible/* && mControlsVisible.get(view).booleanValue()*/) {
            mDelegate.onAlbumSetTopBarControlClicked(view.getId());
        }
    }
   /*Begin: added by scq 20161201*/
    private int selectMode = 3;
    private boolean mIsNoMemory;

    public void setSelectMode(int mode){
	selectMode = mode;
    }
    public void setIsNoMemory(boolean memory){
	mIsNoMemory = memory;
    }
   /*End: added by scq 20161201*/
}
