package com.tongji.palmdetection

import android.os.Bundle
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import com.tongji.palmdetection.model.GlobalModel


class YoloActivity : AppCompatActivity() {
    private val yolov5ncnn = YoloV5Ncnn()
    private val cameraProcess = CameraProcess()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val pattern = intent.getStringExtra("pattern")



        supportRequestWindowFeature(Window.FEATURE_NO_TITLE) // 去掉标题栏
        setContentView(R.layout.camera_layout)

        GlobalModel.setPath(this@YoloActivity.cacheDir)

//        window.statusBarColor = Color.Transparent

        // 模型初始化
        val ret_init = yolov5ncnn.init(assets)
        if (!ret_init) Toast.makeText(this, "yolov5n初始化失败", Toast.LENGTH_LONG).show()

        // 开始检测
        val cameraPreview = findViewById<PreviewView>(R.id.camera_preview)
        val canvas = findViewById<ImageView>(R.id.box_label_canvas)
        val costTimeText = findViewById<TextView>(R.id.cost_time)
        val button = findViewById<ComposeView>(R.id.button)


        val imageAnalyzer = ImageAnalyzer(
            this@YoloActivity,
            cameraPreview,
            costTimeText,
            canvas,
            yolov5ncnn,
            pattern,
            button,
        )

        button.setContent {
            Button(onClick = {
                finish()
            },colors = ButtonDefaults.buttonColors(
                Color(0xD83D59FC)
            )){
                Text(text = "返回")
            }
        }

        cameraProcess.startCamera(this@YoloActivity, imageAnalyzer, cameraPreview)


    }
}

