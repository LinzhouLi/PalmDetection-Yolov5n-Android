package com.tongji.palmdetection

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

class YoloContract : ActivityResultContract<String?, Uri?>() {
    override fun createIntent(context: Context, input: String?): Intent {
        val intent = Intent(context, YoloActivity::class.java)
        intent.putExtra("pattern", input)
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (resultCode == Activity.RESULT_OK) {
            return null
        }
        return null
    }

}