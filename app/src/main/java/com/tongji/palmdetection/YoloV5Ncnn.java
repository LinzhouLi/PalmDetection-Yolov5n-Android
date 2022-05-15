package com.tongji.palmdetection;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;

public class YoloV5Ncnn {

    static {
        System.loadLibrary("palmdetection");
    }

    static private String[] labels = { "finger gap", "palm center" };
    static private int[] colors = { Color.RED, Color.BLUE };

    public class Obj
    {
        public float x;
        public float y;
        public float w;
        public float h;
        public int label;
        public float prob;
    }

    static public String getLabel(int i) {
        return labels[i];
    }

    static public int getColor(int i) {
        return colors[i];
    }

    public native boolean init(AssetManager mgr);
    public native Obj[] detect(Bitmap bitmap, boolean use_gpu);

}
