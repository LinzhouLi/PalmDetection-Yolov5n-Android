package com.tongji.palmdetection.view

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role.Companion.Image
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import com.tongji.palmdetection.R
import com.tongji.palmdetection.YoloContract
import com.tongji.palmdetection.model.GlobalModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    registerEvent: () -> Unit = {}
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(contract = YoloContract()) {
//        it?.let {
//            Toast.makeText(context, "$it", android.widget.Toast.LENGTH_LONG).show()
//        }
    }
//    Button(onClick = {
//        launcher.launch(null)
//    }) {
//        Text(text = "Click")
//    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painterResource(id = R.drawable.background),
            contentDescription = "Home Background",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(fraction = 0.8f)
        )
        Column {
            Spacer(Modifier.height(12.dp))

            Button(onClick = {
                registerEvent()
            },
                colors = ButtonDefaults.buttonColors(
                    Color(0xD83D59FC)
                ),
                modifier = Modifier.fillMaxWidth(0.8f) ) {
                Text(
                    text = "注册",
                    style = TextStyle(
                        fontSize = 20.sp,
                        letterSpacing = 25.sp
                    )
                )
            }

            Spacer(Modifier.height(12.dp))

            Button(onClick = {
                launcher.launch("detect")
            },
                colors = ButtonDefaults.buttonColors(
                Color(0xD83D59FC)
            ),
                modifier = Modifier.fillMaxWidth(0.8f)) {
                Text(
                    text = "登录",
                    style = TextStyle(
                        fontSize = 20.sp,
                        letterSpacing = 25.sp
                    )
                )
            }

        }

    }
}
