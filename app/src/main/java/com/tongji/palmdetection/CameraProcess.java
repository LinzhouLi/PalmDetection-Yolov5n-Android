package com.tongji.palmdetection;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class CameraProcess {

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private final int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[] {
            "android.permission.CAMERA"
    };

    /**
     * 判断是否提供所有权限
     * @param context
     * @return
     */
    public boolean allPermissionsGranted(Context context) {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    /**
     * 申请权限
     * @param activity
     */
    public void requestPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
    }

    public void startCamera(Context context, ImageAnalysis.Analyzer analyzer, PreviewView previewView) {

        cameraProviderFuture = ProcessCameraProvider.getInstance(context);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build();
                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer);
                    Preview previewBuilder = new Preview.Builder()
                            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                            .build();
                    CameraSelector cameraSelector = new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build();
                    previewBuilder.setSurfaceProvider(previewView.getSurfaceProvider());
                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle((LifecycleOwner) context, cameraSelector, imageAnalysis, previewBuilder);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(context));

    }

}
