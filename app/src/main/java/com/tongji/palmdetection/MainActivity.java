package com.tongji.palmdetection;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private YoloV5Ncnn yolov5ncnn = new YoloV5Ncnn();
    private CameraProcess cameraProcess = new CameraProcess();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE); // 去掉标题栏
        setContentView(R.layout.activity_main);

        // 打开app的时候隐藏顶部状态栏
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        // 申请权限
        if (!cameraProcess.allPermissionsGranted(this)) {
            cameraProcess.requestPermissions(this);
        }

        // 获取手机摄像头拍照旋转参数
        int rotation = getWindowManager().getDefaultDisplay().getRotation();

        // 模型初始化
        boolean ret_init = yolov5ncnn.init(getAssets());
        if (ret_init)
            Toast.makeText(this, "yolov5n初始化成功", Toast.LENGTH_LONG).show();
        else
            Toast.makeText(this, "yolov5n初始化失败", Toast.LENGTH_LONG).show();

        // 开始检测
        PreviewView cameraPreview = findViewById(R.id.camera_preview);
        ImageView canvas = findViewById(R.id.box_label_canvas);

        ImageAnalyse imageAnalyse = new ImageAnalyse(
                MainActivity.this,
                cameraPreview,
                canvas,
                rotation,
                yolov5ncnn
        );

        cameraProcess.startCamera(MainActivity.this, imageAnalyse, cameraPreview);
    }

}