package com.cfryan.beyondchat.util;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;

/**
 * Created by cf on 2016/1/22.
 */
public class DensityUtil {
    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static DisplayMeasure getDisplayMeasure(Context context)
    {
        DisplayMeasure displayData = new DisplayMeasure();
        DisplayMetrics dm = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(dm);
        displayData.width = dm.widthPixels;
        displayData.height = dm.heightPixels;
        return displayData;
    }

    public static class DisplayMeasure
    {
        public int width;
        public int height;
        public int getWidth()
        {
            return width;
        }
        public int getHeight()
        {
            return height;
        }
    }


}
