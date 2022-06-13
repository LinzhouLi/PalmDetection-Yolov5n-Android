package com.tongji.palmdetection

import android.Manifest
import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tongji.palmdetection.view.HomeScreen
import com.tongji.palmdetection.view.RegisterScreen


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE) // 去掉标题栏
        setContentView(R.layout.activity_main)

        // 请求权限
        val permission =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                // true: 用户同意   false：用户不同意 or 用户不处理
//                for(i in it){
//                    if(i.value) Toast.makeText(this, "SUCCESSFUL", Toast.LENGTH_LONG).show()
//                }
            }

        permission.launch(arrayOf(
            Manifest.permission.CAMERA, Manifest.permission.INTERNET,
        Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.MANAGE_EXTERNAL_STORAGE))

        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController,
                startDestination = "home",
                route = "root"
            ) {
                composable("home") {
                    HomeScreen { navController.navigate("register") }
                }
                composable("register") {
                    RegisterScreen()
                }
            }
        }
    }
}

