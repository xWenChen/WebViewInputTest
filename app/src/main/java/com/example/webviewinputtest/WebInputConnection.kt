package com.example.webviewinputtest

import android.content.Context
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.*

class WebInputConnection(
    context: Context,
    target: InputConnection?,
    mutable: Boolean
) : InputConnectionWrapper(target, mutable) {
    // 回退键监听
    var callback: (() -> Boolean)? = null

    // 是否需要手动触发 Del 事件
    private var generateDelEvent = false
    // 是否正在预测输入
    private var inComposingMode = false
    // 上次的 composingText 长度
    private var lastComposingTextLength: Int = 0

    init {
        val inputId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        } catch (e: Exception) {
            Log.e(TAG, "", e)
            ""
        }
        Log.d(TAG, "System input type = $inputId")
        // 搜狗输入法，英文输入场景下，预测输入时需要单独生成软键盘 Del 键的点击
        // 前后缀都判断，双重保险
        if (inputId.startsWith(SOU_GOU_NAME_PREFIX) || inputId.endsWith(SOU_GOU_NAME_SUFFIX)) {
            generateDelEvent = true
        }
    }

    // 预测模式下设置文本
    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
        Log.d(TAG, "setComposingText, text=$text, newCursorPosition=$newCursorPosition")

        checkWhenComposingStart(text)

        return super.setComposingText(text, newCursorPosition)
    }

    // 预测模式下已选择文本后，点击删除按钮，会再次触发预测，会走到该方法
    override fun setComposingRegion(start: Int, end: Int): Boolean {
        Log.d(TAG, "setComposingRegion, start=$start, end=$end")

        checkWhenComposingStart(end)

        return super.setComposingRegion(start, end)
    }

    // 预测模式结束会出发这个方法
    override fun finishComposingText(): Boolean {
        Log.d(TAG, "finishComposingText")
        checkWhenComposingEnd(lastComposingTextLength)
        return super.finishComposingText()
    }

    // 预测模式下，输入框中的文本一直删，删到为空，最后会调用这个方法
    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        Log.d(TAG, "commitText, text=$text, newCursorPosition=$newCursorPosition")
        checkWhenComposingEnd(text)
        return super.commitText(text, newCursorPosition)
    }

    private fun checkWhenComposingStart(obj: Any?) {
        // 预测模式下，输入框中直接输入文本，会直接调用这个方法
        if (generateDelEvent && !inComposingMode) {
            inComposingMode = true
        }

        if (!inComposingMode) {
            return
        }

        val nowLength = when(obj) {
            is CharSequence? -> {
                // setComposingText 传过来的新文本
                obj.lengthOrZero
            }
            is Int -> {
                // setComposingRegion 传过来的新的文本长度
                obj
            }
            else -> 0
        }
        // 上次的文本大于此次的文本
        if (lastComposingTextLength > nowLength) {
            keyBackIntercepted()
        }

        lastComposingTextLength = nowLength
    }

    private fun checkWhenComposingEnd(obj: Any?) {
        if (!generateDelEvent || !inComposingMode) {
            return
        }
        inComposingMode = false

        val nowLength = when(obj) {
            is CharSequence? -> {
                // commitText 传过来的最新的文本的长度
                obj.lengthOrZero
            }
            is Int -> {
                // finishComposingText 传过来的上次的文本长度
                obj
            }
            else -> 0
        }

        // 上次的文本大于此次的文本
        if (lastComposingTextLength > nowLength) {
            keyBackIntercepted()
        }

        lastComposingTextLength = 0
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        Log.d(TAG, "deleteSurroundingText, beforeLength=$beforeLength, afterLength=$afterLength")
        val result = super.deleteSurroundingText(beforeLength, afterLength)
        val intercepted = keyBackIntercepted()
        return if (intercepted) {
            intercepted
        } else {
            result
        }
    }

    override fun sendKeyEvent(event: KeyEvent?): Boolean {
        Log.d(TAG, "sendKeyEvent, { action = ${actionToString(event?.action)}, " +
            "keyCode = ${KeyEvent.keyCodeToString(event?.keyCode ?: 0)}}")
        val result = super.sendKeyEvent(event)
        if (event?.keyCode == KeyEvent.KEYCODE_DEL
            && event.action == KeyEvent.ACTION_DOWN
            && keyBackIntercepted()
        ) {
            return true
        }
        return result
    }

    // 触发删除键回调
    private fun keyBackIntercepted(): Boolean {
        Log.d(TAG, "trigger del >>> keyBackIntercepted")
        return callback?.invoke() == true
    }

    private val CharSequence?.lengthOrZero: Int
        get() = this?.length ?: 0

    private fun actionToString(action: Int?): String {
        return when (action) {
            KeyEvent.ACTION_DOWN -> "ACTION_DOWN"
            KeyEvent.ACTION_UP -> "ACTION_UP"
            else -> action.toString()
        }
    }

    companion object {
        const val TAG = "WebInputConnection"

        // 黑鲨搜狗输入法描述：com.sohu.inputmethod.sogou/.SogouIME
        // 小米搜狗输入法描述：com.sohu.inputmethod.sogou.xiaomi/.SogouIME
        const val SOU_GOU_NAME_PREFIX = "com.sohu.inputmethod.sogou"
        const val SOU_GOU_NAME_SUFFIX = "SogouIME"
    }
}