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

package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;

import com.android.gallery3d.R;
import com.android.gallery3d.data.BitmapPool;
import com.android.gallery3d.data.DataSourceType;
import com.android.gallery3d.util.ThreadPool;
import com.android.gallery3d.util.ThreadPool.JobContext;

public class AlbumLabelMaker {
    private static final int BORDER_SIZE = 0;

    private final AlbumSetSlotRenderer.LabelSpec mSpec;
    private final TextPaint mTitlePaint;
    private final TextPaint mCountPaint;
    private final Context mContext;

    private int mLabelWidth;
    private BitmapPool mBitmapPool;

    private final LazyLoadedBitmap mLocalSetIcon;
    private final LazyLoadedBitmap mPicasaIcon;
    private final LazyLoadedBitmap mCameraIcon;
    private final LazyLoadedBitmap mMtpIcon;
    /* Begin: added by yuanhuawei 20130808 */
    private final LazyLoadedBitmap mRightArrowIcon;
    private final int mTitleAndCountPadding = 5;
    /* End: added by yuanhuawei 20130808 */

    // M: added for stereo feature
    private final LazyLoadedBitmap mStereoIcon;

    public AlbumLabelMaker(Context context, AlbumSetSlotRenderer.LabelSpec spec) {
        mContext = context;
        mSpec = spec;
        mTitlePaint = getTextPaint(spec.titleFontSize, spec.titleColor, false);
        mCountPaint = getTextPaint(spec.countFontSize, spec.countColor, false);

        mLocalSetIcon = new LazyLoadedBitmap(R.drawable.frame_overlay_gallery_folder);
        mPicasaIcon = new LazyLoadedBitmap(R.drawable.frame_overlay_gallery_picasa);
        mCameraIcon = new LazyLoadedBitmap(R.drawable.frame_overlay_gallery_camera);
        mMtpIcon = new LazyLoadedBitmap(R.drawable.frame_overlay_gallery_ptp);

        // M: added for stereo feature
        mStereoIcon = new LazyLoadedBitmap(R.drawable.frame_overlay_gallery_stereo);

	/* Begin: added by yuanhuawei 20130808*/
	mRightArrowIcon = new LazyLoadedBitmap(R.drawable.frame_overlay_gallery_right_arrow);
	/* End: added by  yuanhuawei 20130808*/
    }

    public static int getBorderSize() {
        return BORDER_SIZE;
    }

    private Bitmap getOverlayAlbumIcon(int sourceType) {
        switch (sourceType) {
            case DataSourceType.TYPE_CAMERA:
                return mCameraIcon.get();
            case DataSourceType.TYPE_LOCAL:
                return mLocalSetIcon.get();
            case DataSourceType.TYPE_MTP:
                return mMtpIcon.get();
            case DataSourceType.TYPE_PICASA:
                return mPicasaIcon.get();
            case DataSourceType.TYPE_STEREO:
                return mStereoIcon.get();
        }
        return null;
    }

    private static TextPaint getTextPaint(int textSize, int color, boolean isBold) {
        TextPaint paint = new TextPaint();
        paint.setTextSize(textSize);
        paint.setAntiAlias(true);
        paint.setColor(color);
        //paint.setShadowLayer(2f, 0f, 0f, Color.LTGRAY);
        if (isBold) {
            paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        }
        return paint;
    }

    public class LazyLoadedBitmap {
        private Bitmap mBitmap;
        private int mResId;

        public LazyLoadedBitmap(int resId) {
            mResId = resId;
        }

        public synchronized Bitmap get() {
            if (mBitmap == null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                mBitmap = BitmapFactory.decodeResource(
                        mContext.getResources(), mResId, options);
            }
            return mBitmap;
        }
    }

    public synchronized void setLabelWidth(int width) {
        if (mLabelWidth == width) return;
        mLabelWidth = width;
        int borders = 2 * BORDER_SIZE;
		Log.i("ssbb","===youar===");
        mBitmapPool = new BitmapPool(
                width + borders, mSpec.labelBackgroundHeight + borders, 16);
    }

    public ThreadPool.Job<Bitmap> requestLabel(
            String title, String count, int sourceType) {
            Log.i("ssbb","++++=start====");
        return new AlbumLabelJob(title, count, sourceType);
    }

    static void drawText(Canvas canvas,
            int x, int y, String text, int lengthLimit, TextPaint p) {
        // The TextPaint cannot be used concurrently
        synchronized (p) {
           Log.i("ssbb","====ooolol==");
            text = TextUtils.ellipsize(
                    text, p, lengthLimit, TextUtils.TruncateAt.END).toString();
            canvas.drawText(text, x, y - p.getFontMetricsInt().ascent, p);
        }
    }

    private class AlbumLabelJob implements ThreadPool.Job<Bitmap> {
        private final String mTitle;
        private final String mCount;
        private final int mSourceType;

        public AlbumLabelJob(String title, String count, int sourceType) {
            mTitle = title;
            mCount = count;
            mSourceType = sourceType;
        }

        @Override
        public Bitmap run(JobContext jc) {
            AlbumSetSlotRenderer.LabelSpec s = mSpec;

            String title = mTitle;
            String count = mCount;
            Bitmap icon = getOverlayAlbumIcon(mSourceType);
	   Bitmap arrowIcon = mRightArrowIcon.get();

            Bitmap bitmap;
            int labelWidth;

            synchronized (this) {
                labelWidth = mLabelWidth;
                bitmap = mBitmapPool.getBitmap();
            }

            if (bitmap == null) {
                int borders = 2 * BORDER_SIZE;
                bitmap = Bitmap.createBitmap(labelWidth + borders,
                        s.labelBackgroundHeight + borders, Config.ARGB_8888);
            }

            Canvas canvas = new Canvas(bitmap);
            canvas.clipRect(BORDER_SIZE, BORDER_SIZE,
                    bitmap.getWidth() - BORDER_SIZE,
                    bitmap.getHeight() - BORDER_SIZE);
            canvas.drawColor(mSpec.backgroundColor, PorterDuff.Mode.SRC);

            canvas.translate(BORDER_SIZE, BORDER_SIZE);

            // draw title
            if (jc.isCancelled()) return null;
            int x = s.leftMargin + s.iconSize;
            // TODO: is the offset relevant in new reskin?
            // int y = s.titleOffset;
            /* Begin: changed by yuanhuawei 20130808 */
	   //changed from (s.labelBackgroundHeight - s.titleFontSize) / 2
            int y = (s.labelBackgroundHeight - s.titleFontSize - s.countFontSize - mTitleAndCountPadding) / 2;
            drawText(canvas, x-40, y+10, title, labelWidth - s.leftMargin - x - 
                    s.titleRightMargin, mTitlePaint);                  //mod by gaojunbin

            // draw count
            if (jc.isCancelled()) return null;
	  //changed from labelWidth - s.titleRightMargin;
            x = s.leftMargin + s.iconSize;
	  //changed from (s.labelBackgroundHeight - s.countFontSize) / 2;
            y = y + s.titleFontSize + mTitleAndCountPadding;
            drawText(canvas, x-40, y+10, count,
                    labelWidth - x , mCountPaint);

	   if(arrowIcon != null){
	   	if (jc.isCancelled()) return null;
		//float scaleArrow = (float) s.iconSize / arrowIcon.getWidth();
                //canvas.translate(labelWidth - s.titleRightMargin -arrowIcon.getWidth(), (s.labelBackgroundHeight -
                      //  Math.round(scaleArrow * arrowIcon.getHeight()))/2f);
                //canvas.scale(scaleArrow, scaleArrow);
                x = labelWidth - s.titleRightMargin -arrowIcon.getWidth();
	       y = (s.labelBackgroundHeight - arrowIcon.getHeight()) / 2;
                canvas.drawBitmap(arrowIcon, x+10 , y+10, null);
	   }
	   /* End: changed by yuanhuawei 20130808 */

	 /* Begin: deleted by yuanhuawei 20130808*/
            // draw the icon
          /*
            if (icon != null) {
                if (jc.isCancelled()) return null;
                float scale = (float) s.iconSize / icon.getWidth();
                canvas.translate(s.leftMargin, (s.labelBackgroundHeight -
                        Math.round(scale * icon.getHeight()))/2f);
                canvas.scale(scale, scale);
                canvas.drawBitmap(icon, 0, 0, null);
            }
	*/
	/* End: deleted by yuanhuawei 20130808*/

            return bitmap;
        }
    }

    public void recycleLabel(Bitmap label) {
        mBitmapPool.recycle(label);
    }

    public void clearRecycledLabels() {
        if (mBitmapPool != null) mBitmapPool.clear();
    }
}
