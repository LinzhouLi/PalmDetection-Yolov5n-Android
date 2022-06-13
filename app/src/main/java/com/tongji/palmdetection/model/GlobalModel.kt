package com.tongji.palmdetection.model

import java.io.File

object GlobalModel {
    private var path: File? = null
    private var userName: String = ""
    private var picNum: Int = 0
    private var registeLeft: Boolean = false
    private var waitFlag: Boolean = true
    private var matchRes: String = ""

    fun isWait(): Boolean {
        return waitFlag
    }

    fun setWaitFlag(boolean: Boolean) {
        waitFlag = boolean
    }

    fun setMatchRes(res: String) {
        matchRes = res
    }

    fun getMatchRes(): String {
        return matchRes
    }

    fun convertRegisteLeft() {
        registeLeft = !registeLeft
    }

    fun getRegisteLeft(): Boolean {
        return registeLeft
    }

    fun getLorR(): String {
        if (registeLeft) return "right"
        else return "left"
    }

    fun setPath(p:File) {
        path = p
    }

    fun getPath(): File? {
        return path
    }

    fun setUserName(n: String) {
        userName = n
    }

    fun getUserName(): String? {
        return userName
    }

    fun addNum() {
        picNum++
    }


    fun resetNum() {
        picNum = 0
    }

    fun isRegister(): Boolean {
        return picNum == 4
    }

}