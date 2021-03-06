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

        // ??????????????????fill_start?????????bitmap
        if (fullScreenTransform == null) {
            // img -> previewView
            // ?????????imagewidth?????????previewWidth ????????????
            fullScreenTransform = imageProcess.getTransformationMatrix(
                    imageWidth, imageHeight,
                    previewWidth, previewHeight,
                    90, false
            );
        }

            // ??????bitmap
        Observable.create( (ObservableEmitter<Result> emitter) -> {

            long startTime = System.currentTimeMillis();

            // Bitmap
            Bitmap imageBitmap = convertImageProxyToBitmap(image);
            Bitmap fullImageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageWidth, imageHeight, fullScreenTransform, false);


            Log.i("fullImageBitmap", fullImageBitmap.getWidth() + "  " + fullImageBitmap.getHeight());

            // ??????yolov5????????????
            // obj ???????????????fullImageBitmap???
            YoloV5Ncnn.Obj[] objects = yolov5Detector.detect(fullImageBitmap, true);

            // ??????????????????
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

            // ???????????????????????????????????????
            // ??????????????????????????????????????????????????????????????????
            // ?????? ???????????????????????? ??????????????????????????????
            if (!breakSignal) {
                // ?????????????????????prob<0.6????????????????????????
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

                // ?????????????????????????????????????????????prob>0.6 ?????????
                if (credibility && hitCount[0] && hitCount[1]) {
                    // ????????????????????? ????????????????????????
                    // 1 ??????????????????????????????????????? ?????????????????????????????? register??????
                    // 2 ????????????prob<0.8??????????????????(??????????????? ??????????????????????????????
                    // ??????????????????????????????prob>0.8?????? ?????????bug)
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
                                Toast.makeText(context, "??????????????????????????????????????????", Toast.LENGTH_LONG).show();
                                Looper.loop();
                            } else {
                                Looper.prepare();
                                Toast.makeText(context, "??????????????????", Toast.LENGTH_LONG).show();
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

                // hitCount[0] ????????????????????? [1]?????????
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

        // ????????????
        Paint boxPaint = new Paint();
        boxPaint.setStrokeWidth(5);
        boxPaint.setStyle(Paint.Style.STROKE);

        // ????????????
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
