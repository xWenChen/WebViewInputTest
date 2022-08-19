package com.example.webviewinputtest

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.WebView

/**
 * description:
 *
 * @author   wchenzhang
 * date：    2022/8/18 15:54
 * version   1.0
 * modify by
 */
class TestWebView : WebView {
    var inputConnection = WebInputConnection(context.applicationContext, null, true)

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val target = super.onCreateInputConnection(outAttrs)
        if (target == null) {
            return target
        }
        inputConnection.setTarget(target)
        return inputConnection
    }
}