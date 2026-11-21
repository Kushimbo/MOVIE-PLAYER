package pt.pedro.cineplayer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var root: FrameLayout
    private var chooser: ValueCallback<Array<Uri>>? = null
    private var customView: View? = null
    private var customCb: WebChromeClient.CustomViewCallback? = null

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        root = FrameLayout(this)
        web = WebView(this)
        root.addView(web, ViewGroup.LayoutParams(-1, -1))
        setContentView(root)
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
        }
        web.setBackgroundColor(0xFF0B0B10.toInt())
        web.webViewClient = WebViewClient()
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(w: WebView, cb: ValueCallback<Array<Uri>>, p: FileChooserParams): Boolean {
                chooser?.onReceiveValue(null)
                chooser = cb
                val i = p.createIntent().apply {
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, p.mode == FileChooserParams.MODE_OPEN_MULTIPLE)
                    if (p.acceptTypes.any { it.contains("video") }) type = "*/*"
                }
                return try { startActivityForResult(i, 1); true } catch (e: Exception) { chooser = null; false }
            }
            override fun onShowCustomView(v: View, cb: CustomViewCallback) {
                customView = v; customCb = cb
                root.addView(v, ViewGroup.LayoutParams(-1, -1))
                web.visibility = View.GONE
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
            override fun onHideCustomView() {
                customView?.let { root.removeView(it) }
                customView = null; customCb?.onCustomViewHidden(); customCb = null
                web.visibility = View.VISIBLE
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
        web.loadUrl("file:///android_asset/index.html")
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        if (req == 1) {
            val out: Array<Uri>? = if (res == RESULT_OK && data != null) {
                val clip = data.clipData
                if (clip != null) Array(clip.itemCount) { clip.getItemAt(it).uri }
                else data.data?.let { arrayOf(it) }
            } else null
            chooser?.onReceiveValue(out); chooser = null
        } else super.onActivityResult(req, res, data)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (customView != null) { web.webChromeClient?.onHideCustomView(); return }
        web.evaluateJavascript("(function(){var p=document.getElementById('player');if(p&&p.classList.contains('on')){closePlayer();return 1}return 0})()") {
            if (it != "1") finish()
        }
    }

    override fun onPause() { super.onPause(); web.onPause() }
    override fun onResume() { super.onResume(); web.onResume() }
}
