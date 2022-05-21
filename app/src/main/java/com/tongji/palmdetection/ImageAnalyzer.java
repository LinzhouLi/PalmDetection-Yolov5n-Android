package com.tongji.palmdetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ImageAnalyzer implements ImageAnalysis.Analyzer {

    public static class Result{

        public Result(long costTime, Bitmap bitmap) {
            this.costTime = costTime;
            this.bitmap = bitmap;
        }
        long costTime;
        Bitmap bitmap;
    }

    private final TextView costTimeText;
    private final ImageView boxLabelCanvas;
    private final PreviewView previewView;
    private final ImageProcess imageProcess;
    private final YoloV5Ncnn yolov5Detector;
    private Matrix fullScreenTransform = null;

    public ImageAnalyzer(
            Context context,
            PreviewView previewView,
            TextView costTimeText,
            ImageView boxLabelCanvas,
            YoloV5Ncnn yolov5Detector
    ) {

        this.previewView = previewView;
        this.boxLabelCanvas = boxLabelCanvas;
        this.costTimeText = costTimeText;
        this.yolov5Detector = yolov5Detector;
        this.imageProcess = new ImageProcess();

    }

    private Bitmap convertImageProxyToBitmap(ImageProxy image) {
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer vuBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int vuSize = vuBuffer.remaining();

        byte[] nv21 = new byte[ySize + vuSize];

        yBuffer.get(nv21, 0, ySize);
        vuBuffer.get(nv21, ySize, vuSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 50, out);
        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {

        int previewHeight = previewView.getHeight();
        int previewWidth = previewView.getWidth();

        int imageHeight = image.getHeight();
        int imageWidth = image.getWidth();

        // 图片适应屏幕fill_start格式的bitmap
        if (fullScreenTransform == null) {
            fullScreenTransform = imageProcess.getTransformationMatrix(
                    imageWidth, imageHeight,
                    imageWidth, previewHeight,
                    90, false
            );
        }

        Observable.create( (ObservableEmitter<Result> emitter) -> {

            long startTime = System.currentTimeMillis();

            // bitmap
            Bitmap imageBitmap = convertImageProxyToBitmap(image);
            Bitmap fullImageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageWidth, imageHeight, fullScreenTransform, false);


            Log.i("fullImageBitmap", fullImageBitmap.getWidth() + "  " + fullImageBitmap.getHeight());

            // 调用yolov5预测接口
            YoloV5Ncnn.Obj[] objects = yolov5Detector.detect(fullImageBitmap, true);

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
              costTimeText.setText(Long.toString(result.costTime) + "ms");
//              Log.i("image", Long.toString(result.costTime) + "ms");
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
