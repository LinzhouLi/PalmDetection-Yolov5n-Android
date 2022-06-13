package com.tongji.palmdetection.view

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tongji.palmdetection.R
import com.tongji.palmdetection.YoloContract
import com.tongji.palmdetection.model.GlobalModel
import com.tongji.palmdetection.model.GlobalModel.setWaitFlag

@Composable
fun RegisterScreen() {

    val context = LocalContext.current
    val isRegisterLeft = remember {
        mutableStateOf(false)
    }

    isRegisterLeft.value = GlobalModel.getRegisteLeft()
    val launcher = rememberLauncherForActivityResult(contract = YoloContract()) {
        isRegisterLeft.value = GlobalModel.getRegisteLeft()

//        it?.let {
//            android.widget.Toast.makeText(context, "$it", android.widget.Toast.LENGTH_LONG)
//        }
    }

    val textState = remember {
        mutableStateOf("")
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        IconButton(onClick = {
            if (isRegisterLeft.value || textState.value != "") {
                GlobalModel.setUserName(textState.value)
                launcher.launch("register")
            } else {
                Toast.makeText(context, "请输入姓名！", Toast.LENGTH_LONG)
            }
        },
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            Image(
                painterResource(id =
                if (!isRegisterLeft.value) R.drawable.left_palm
                else R.drawable.right_palm
                ),
                contentDescription = "Home Background",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction = 0.8f),
            )
        }

        Spacer(Modifier.height(12.dp))

        if (!isRegisterLeft.value) {
            TextField(value = textState.value,
                onValueChange = {
                    textState.value = it
                },
                colors = TextFieldDefaults.textFieldColors(
                    textColor = Color(0xFF0D0D0E),
                    backgroundColor = Color.Transparent,
                    cursorColor = Color(0xFF045DA0),
                ),
                placeholder = {
                    Text(
                        "输入姓名",
                        modifier = Modifier
                            .fillMaxWidth(),
                        color = Color(0xFFBBB4B4)
                    )
                },
                shape = RoundedCornerShape(30.dp)
            )
        }
    }

}