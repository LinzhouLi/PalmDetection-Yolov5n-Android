package com.tongji.palmdetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ImageAnalyse implements ImageAnalysis.Analyzer {

    public static class Result{

        public Result(long costTime, Bitmap bitmap) {
            this.costTime = costTime;
            this.bitmap = bitmap;
        }
        long costTime;
        Bitmap bitmap;
    }

    private final ImageView boxLabelCanvas;
    private final PreviewView previewView;
    private final ImageProcess imageProcess;
    private final YoloV5Ncnn yolov5Detector;
    private final int rotation;

    public ImageAnalyse(
            Context context,
            PreviewView previewView,
            ImageView boxLabelCanvas,
            int rotation,
            YoloV5Ncnn yolov5Detector
    ) {

        this.previewView = previewView;
        this.boxLabelCanvas = boxLabelCanvas;
        this.yolov5Detector = yolov5Detector;
        this.rotation = rotation;
        this.imageProcess = new ImageProcess();

    }

    @Override
    public void analyze(@NonNull ImageProxy image) {

        int previewHeight = previewView.getHeight();
        int previewWidth = previewView.getWidth();

        Observable.create( (ObservableEmitter<Result> emitter) -> {

            long startTime = System.currentTimeMillis();

            byte[][] yuvBytes = new byte[3][];
            ImageProxy.PlaneProxy[] planes = image.getPlanes();
            int imageHeight = image.getHeight();
            int imageWidth = image.getWidth();

            imageProcess.fillBytes(planes, yuvBytes);
            int yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            int[] rgbBytes = new int[imageHeight * imageWidth];
            imageProcess.YUV420ToARGB8888(
                    yuvBytes[0], yuvBytes[1], yuvBytes[2],
                    imageWidth, imageHeight,
                    yRowStride,
                    uvRowStride, uvPixelStride,
                    rgbBytes
            );

            // 原图bitmap
            Bitmap imageBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
            imageBitmap.setPixels(rgbBytes, 0, imageWidth, 0, 0, imageWidth, imageHeight);

            // 图片适应屏幕fill_start格式的bitmap
            double scale = Math.max(
                    previewHeight / (double) (rotation % 180 == 0 ? imageWidth : imageHeight),
                    previewWidth / (double) (rotation % 180 == 0 ? imageHeight : imageWidth)
            );
            Matrix fullScreenTransform = imageProcess.getTransformationMatrix(
                    imageWidth, imageHeight,
                    (int) (scale * imageHeight), (int) (scale * imageWidth),
                    rotation % 180 == 0 ? 90 : 0, false
            );

            // 适应preview的全尺寸bitmap
            Bitmap fullImageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageWidth, imageHeight, fullScreenTransform, false);
            // 裁剪出跟preview在屏幕上一样大小的bitmap
            Bitmap cropImageBitmap = Bitmap.createBitmap(fullImageBitmap, 0, 0, previewWidth, previewHeight);

            // 调用yolov5预测接口
            YoloV5Ncnn.Obj[] objects = yolov5Detector.detect(cropImageBitmap, true);

            // 画出预测结果
            Bitmap resultBitmap = drawObjects(objects);

            long endTime = System.currentTimeMillis();
            long costTime = (endTime - startTime);
            image.close();
            emitter.onNext(new Result(costTime, resultBitmap));

        }).subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe( (Result result) -> {
              boxLabelCanvas.setImageBitmap(result.bitmap);
              Log.i("image", Long.toString(result.costTime) + "ms");
          });

    }

    private Bitmap drawObjects(YoloV5Ncnn.Obj[] objects) {

        int previewHeight = previewView.getHeight();
        int previewWidth = previewView.getWidth();

        Bitmap emptyCropSizeBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        Canvas cropCanvas = new Canvas(emptyCropSizeBitmap);

        // 边框画笔
        Paint boxPaint = new Paint();
        boxPaint.setStrokeWidth(5);
        boxPaint.setStyle(Paint.Style.STROKE);

        // 字体画笔
        Paint textPain = new Paint();
        textPain.setTextSize(50);
        textPain.setStyle(Paint.Style.FILL);

        for (YoloV5Ncnn.Obj res : objects) {
            int label = res.label;
            textPain.setColor(YoloV5Ncnn.getColor(label));
            boxPaint.setColor(YoloV5Ncnn.getColor(label));

            RectF location = new RectF();
            location.left = res.x;
            location.top = res.y;
            location.right = res.x + res.w;
            location.bottom = res.y + res.h;

            cropCanvas.drawRect(location, boxPaint);
            cropCanvas.drawText(YoloV5Ncnn.getLabel(label) + ":" + String.format("%.2f", res.prob), location.left, location.top, textPain);
        }

        return emptyCropSizeBitmap;

    }

}
