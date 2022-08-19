package com.example.webviewinputtest

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
    }

    lateinit var webView: TestWebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        webView.inputConnection.callback = {
            Log.d(TAG, "trigger del event")
            // TODO 通知观察者软键盘删除键点击事件触发了
            false
        }

        webView.settings.apply {
            domStorageEnabled = true
            javaScriptEnabled = true
        }

        //访问网页
        webView.loadUrl("https://www.baidu.com");
    }

    override fun onDestroy() {
        // 加载空页面
        webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null)
        webView.clearHistory()
        (webView.parent as ViewGroup).removeView(webView)
        webView.destroy()

        super.onDestroy()
    }
}