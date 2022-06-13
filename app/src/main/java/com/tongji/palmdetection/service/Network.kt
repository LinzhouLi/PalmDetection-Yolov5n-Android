package com.tongji.palmdetection.service

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.tongji.palmdetection.model.DetectResponse
import com.tongji.palmdetection.model.GlobalModel
import com.tongji.palmdetection.model.RegisterResponse
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*

object Network {

    //service:
    private val userService = ServiceCreator.create(UserService:: class.java)

    //suspend fun:
    fun register(name: String, LorR: String, dfgx1: String, dfgy1: String,
                         dfgx2: String, dfgy2: String, pcx: String, pcy: String,
                         image: Bitmap) {

        val reqFile = bitmap2file(image)
        val requestBody: RequestBody =
            RequestBody.create(MediaType.parse("image/*"), reqFile)
        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", reqFile.name, requestBody)
            .addFormDataPart("name", name)
            .addFormDataPart("LorR", LorR)
            .addFormDataPart("dfgx1", dfgx1)
            .addFormDataPart("dfgy1", dfgy1)
            .addFormDataPart("dfgx2", dfgx2)
            .addFormDataPart("dfgy2", dfgy2)
            .addFormDataPart("pcx", pcx)
            .addFormDataPart("pcy", pcy)
            .build()

        userService.register(multipartBody).enqueue(
            object : Callback<RegisterResponse> {
                override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                    val body = response.body()
                    if (body != null) {
                        if (body.success) {
                            GlobalModel.addNum()
                            println("add!!!")
                        }
                    }
                }
                override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                    println("error")
                    println(t)
                }
            }
        )
    }


    fun detect(dfgx1: String, dfgy1: String,
                       dfgx2: String, dfgy2: String, pcx: String, pcy: String,
               image: Bitmap) {
        val reqFile = bitmap2file(image)
        val requestBody: RequestBody =
            RequestBody.create(MediaType.parse("image/*"), reqFile)
        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", reqFile.name, requestBody)
            .addFormDataPart("dfgx1", dfgx1)
            .addFormDataPart("dfgy1", dfgy1)
            .addFormDataPart("dfgx2", dfgx2)
            .addFormDataPart("dfgy2", dfgy2)
            .addFormDataPart("pcx", pcx)
            .addFormDataPart("pcy", pcy)
            .build()

        println("request")

        userService.detect(multipartBody).enqueue(
            object : Callback<DetectResponse> {
                override fun onResponse(call: Call<DetectResponse>, response: Response<DetectResponse>) {
                    println("response")
                    println(response)
                    val body = response.body()
                    if (body != null) {
                        if (body.match) {
                            GlobalModel.setMatchRes("识别通过，您的是" + body.person)
                        } else {
                            GlobalModel.setMatchRes("不存在该用户，请注册")
                        }
                        GlobalModel.setWaitFlag(false)
                    } else {
                        GlobalModel.setMatchRes("服务器故障，请重试")
                        GlobalModel.setWaitFlag(false)
                    }
                }
                override fun onFailure(call: Call<DetectResponse>, t: Throwable) {
                    println("error")
                    println(t)
                }
            }
        )
    }

    fun bitmap2file(bitmap: Bitmap): File{
        println(GlobalModel.getPath())

        var f = File( GlobalModel.getPath(),"palm_image.jpg");
        f.createNewFile();

        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
        val bitmapdata = bos.toByteArray();

        try {
            var fos = FileOutputStream(f)
            fos.write(bitmapdata);
            fos.flush();
            fos.close();
        } catch (e: FileNotFoundException) {
            e.printStackTrace();
        } catch (e: IOException) {
            e.printStackTrace();
        }
        return f
    }
}
