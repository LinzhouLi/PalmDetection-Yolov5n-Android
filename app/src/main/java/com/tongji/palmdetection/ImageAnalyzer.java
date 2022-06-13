package com.tongji.palmdetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.graphics.Rect;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;
import androidx.compose.ui.platform.ComposeView;

import com.tongji.palmdetection.model.GlobalModel;
import com.tongji.palmdetection.service.Network;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlin.Unit;


public class ImageAnalyzer implements ImageAnalysis.Analyzer {

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
    private final TextView costTimeText;
    private ComposeView button;
    private Matrix fullScreenTransform = null;
    private boolean[] hitCount = new boolean[2];
    private int picNum = 0;
    private boolean breakSignal = false;
    private String pattern = "detect";
    private Context context;

    public ImageAnalyzer(
            Context context,
            PreviewView previewView,
            TextView costTimeText,
            ImageView boxLabelCanvas,
            YoloV5Ncnn yolov5Detector,
            String pattern,
            ComposeView button
    ) {
        this.previewView = previewView;
        this.boxLabelCanvas = boxLabelCanvas;
        this.costTimeText = costTimeText;
        this.yolov5Detector = yolov5Detector;
        this.imageProcess = new ImageProcess();
        this.pattern = pattern;
        this.button = button;
        this.context = context;
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
            // img -> previewView
            // 源碼用imagewidth代替了previewWidth 錯了吧？
            fullScreenTransform = imageProcess.getTransformationMatrix(
                    imageWidth, imageHeight,
                    previewWidth, previewHeight,
                    90, false
            );
        }

            // 原图bitmap
        Observable.create( (ObservableEmitter<Result> emitter) -> {

            long startTime = System.currentTimeMillis();

            // Bitmap
            Bitmap imageBitmap = convertImageProxyToBitmap(image);
            Bitmap fullImageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageWidth, imageHeight, fullScreenTransform, false);


            Log.i("fullImageBitmap", fullImageBitmap.getWidth() + "  " + fullImageBitmap.getHeight());

            // 调用yolov5预测接口
            // obj 坐标是基于fullImageBitmap的
            YoloV5Ncnn.Obj[] objects = yolov5Detector.detect(fullImageBitmap, true);

            // 画出预测结果
            Bitmap resultBitmap = drawObjects(objects);


            long endTime = System.currentTimeMillis();
            long costTime = (endTime - startTime);

            float dfgx1 = 0;
            float dfgy1 = 0;
            float dfgx2 = 0;
            float dfgy2 = 0;
            float pcx = 0;
            float pcy = 0;
            boolean saveAnother = false;

            // 成功后弹出提示用户手动回退
            // 需要一个信号量，防止用户反应过程中再不断上传
            // 同时 后端检测时间过长 所以也需要及时的中断
            if (!breakSignal) {
                // 只要有一个区域prob<0.6，则本次检测无效
                boolean credibility = true;
                for (YoloV5Ncnn.Obj res : objects) {
                    if (res.prob < 0.6||objects.length!=3) {
                        credibility = false;
                        Log.i("credibility", "prob < 0.6");
                        break;
                    }

                    if (res.label == 1) {
                        pcx = res.x + res.w / 2;
                        pcy = res.y + res.h / 2;
                    } else if (!saveAnother){
                        saveAnother = true;
                        dfgx1 = res.x + res.w / 2;
                        dfgy1 = res.y + res.h / 2;
                    } else {
                        dfgx2 = res.x + res.w / 2;
                        dfgy2 = res.y + res.h / 2;
                    }

                    Log.i("credibility!", String.valueOf(res.label) + "prob > 0.6");
                }
                if((pcx==dfgx1&&pcy==dfgy1)||(pcx==dfgx2&&pcy==dfgy2)||(dfgx2==dfgx1&&dfgy2==dfgy1)){
                    credibility = false;
                }

                // 如果本次检测有效且之前连续两次prob>0.6 则上传
                if (credibility && hitCount[0] && hitCount[1]) {
                    // 每上传一次图片 之前累计全部清空
                    // 1 使两次上传之间的时间足够长 防止信号量来不及更新 register多张
                    // 2 防止出现prob<0.8的图片被上传(如果不清空 那么只要一张图片上传
                    // 其一下张图只要自己的prob>0.8即可 容易出bug)
                    hitCount[0] = false;
                    hitCount[1] = false;
                    credibility = false;

                    System.out.println("ok to upload" + pattern);

                    if (pattern.equals("register")) {

                        Network.INSTANCE.register(GlobalModel.INSTANCE.getUserName(),
                                GlobalModel.INSTANCE.getLorR(), String.valueOf((int)Math.floor(dfgx1)),
                                String.valueOf((int)Math.floor(dfgy1)), String.valueOf((int)Math.floor(dfgx2)),
                                String.valueOf((int)Math.floor(dfgy2)), String.valueOf((int)Math.floor(pcx)),
                                String.valueOf((int)Math.floor(pcy)), fullImageBitmap);

                        if (GlobalModel.INSTANCE.isRegister()) {
                            GlobalModel.INSTANCE.resetNum();
                            GlobalModel.INSTANCE.convertRegisteLeft();
                            breakSignal = true;
                            if (GlobalModel.INSTANCE.getRegisteLeft()) {
                                Looper.prepare();
                                Toast.makeText(context, "左手注册完毕，请返回注册右手", Toast.LENGTH_LONG).show();
                                Looper.loop();
                            } else {
                                Looper.prepare();
                                Toast.makeText(context, "右手注册完毕", Toast.LENGTH_LONG).show();
                                Looper.loop();
                            }
                        }
                    }
                    else if (pattern.equals("detect")) {
                        breakSignal = true;
                        System.out.println("ok to upload" + breakSignal);

                        Network.INSTANCE.detect(String.valueOf((int)Math.floor(dfgx1)),
                                String.valueOf((int)Math.floor(dfgy1)), String.valueOf((int)Math.floor(dfgx2)),
                                String.valueOf((int)Math.floor(dfgy2)), String.valueOf((int)Math.floor(pcx)),
                                String.valueOf((int)Math.floor(pcy)), fullImageBitmap);
                    }
                }

                // hitCount[0] 上一次是否有效 [1]上上次
                hitCount[1] = hitCount[0];
                hitCount[0] = credibility;
            }

            if(!GlobalModel.INSTANCE.isWait()) {
                Looper.prepare();
                Toast.makeText(context, GlobalModel.INSTANCE.getMatchRes(), Toast.LENGTH_LONG).show();
                GlobalModel.INSTANCE.setWaitFlag(true);
                Looper.loop();
            }

            image.close();
            emitter.onNext(new Result(costTime, resultBitmap));
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( (Result result) -> {
                    costTimeText.setText(Long.toString(result.costTime) + "ms");
                    boxLabelCanvas.setImageBitmap(result.bitmap);
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
