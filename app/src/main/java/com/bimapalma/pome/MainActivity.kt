package com.bimapalma.pome

import android.app.Activity
import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Base64
import androidx.core.content.FileProvider
import android.webkit.*
import android.widget.Toast
import androidx.webkit.WebViewAssetLoader
import java.io.File

class MainActivity : Activity() {
    private lateinit var wv: WebView
    private var cb: ValueCallback<Array<Uri>>? = null
    private var geoCb: GeolocationPermissions.Callback? = null
    private var geoOrigin: String? = null

    private fun hasLoc() = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    inner class Bridge {
        @JavascriptInterface fun build(): Int = versionCodeNow()
        @JavascriptInterface fun version(): String =
            try { packageManager.getPackageInfo(packageName, 0).versionName ?: "" } catch (e: Exception) { "" }
        @JavascriptInterface fun download(url: String) { runOnUiThread { startDownload(url) } }
        @JavascriptInterface fun shareText(t: String) {
            runOnUiThread {
                startActivity(Intent.createChooser(
                    Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, t), "Bagikan rekap"))
            }
        }
        @JavascriptInterface fun shareFile(name: String, b64: String, mime: String) {
            try {
                val dir = File(cacheDir, "share").apply { mkdirs() }
                dir.listFiles()?.forEach { it.delete() }
                val f = File(dir, name.replace(Regex("[^A-Za-z0-9._-]"), "_"))
                f.writeBytes(Base64.decode(b64, Base64.DEFAULT))
                val u = FileProvider.getUriForFile(this@MainActivity, "$packageName.fileprovider", f)
                runOnUiThread {
                    startActivity(Intent.createChooser(
                        Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, u)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Bagikan file"))
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Gagal membagikan file", Toast.LENGTH_LONG).show() }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun versionCodeNow(): Int = try {
        val p = packageManager.getPackageInfo(packageName, 0)
        if (Build.VERSION.SDK_INT >= 28) p.longVersionCode.toInt() else p.versionCode
    } catch (e: Exception) { 0 }

    private fun startDownload(url: String) {
        if (Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) {
            Toast.makeText(this, "Izinkan install dari sumber ini, lalu tekan Perbarui lagi", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
            return
        }
        val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "POME-Monitor.apk").delete()
        val id = dm.enqueue(
            DownloadManager.Request(Uri.parse(url))
                .setTitle("POME Monitor")
                .setDescription("Mengunduh pembaruan")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "POME-Monitor.apk")
        )
        Toast.makeText(this, "Mengunduh pembaruan...", Toast.LENGTH_SHORT).show()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                if (i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) != id) return
                unregisterReceiver(this)
                val u = dm.getUriForDownloadedFile(id)
                if (u == null) {
                    Toast.makeText(this@MainActivity, "Unduhan gagal, coba lagi", Toast.LENGTH_LONG).show()
                    return
                }
                startActivity(
                    Intent(Intent.ACTION_VIEW)
                        .setDataAndType(u, "application/vnd.android.package-archive")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this)).build()
        wv = WebView(this)
        setContentView(wv)
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.addJavascriptInterface(Bridge(), "Android")
        wv.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(v: WebView, r: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(r.url)

            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                if (r.url.host == "appassets.androidplatform.net") return false
                try { startActivity(Intent(Intent.ACTION_VIEW, r.url)) } catch (e: Exception) {}
                return true
            }
        }
        wv.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                if (hasLoc()) { callback.invoke(origin, true, false); return }
                geoCb = callback; geoOrigin = origin
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 2)
            }
            override fun onShowFileChooser(v: WebView, c: ValueCallback<Array<Uri>>, p: FileChooserParams): Boolean {
                cb?.onReceiveValue(null)
                cb = c
                return try { startActivityForResult(p.createIntent(), 1); true } catch (e: Exception) { cb = null; false }
            }
        }
        wv.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }

    override fun onRequestPermissionsResult(rq: Int, perms: Array<out String>, res: IntArray) {
        super.onRequestPermissionsResult(rq, perms, res)
        if (rq == 2) {
            val ok = res.any { it == PackageManager.PERMISSION_GRANTED }
            geoCb?.invoke(geoOrigin, ok, false); geoCb = null; geoOrigin = null
        }
    }

    override fun onActivityResult(rq: Int, rs: Int, d: Intent?) {
        if (rq == 1) { cb?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(rs, d)); cb = null }
    }

    override fun onBackPressed() { if (wv.canGoBack()) wv.goBack() else super.onBackPressed() }
}
