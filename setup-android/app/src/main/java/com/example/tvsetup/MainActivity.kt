package com.example.tvsetup

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import dadb.AdbKeyPair
import dadb.Dadb
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Phone companion: downloads the latest TvLauncher APK and installs it on an
 * Android TV over the network (ADB over TCP, port 5555 by default).
 */
class MainActivity : Activity() {

    companion object {
        const val APK_URL = "https://github.com/ItachiCanCode/Custom-TV/releases/latest/download/app-debug.apk"
        const val LAUNCHER_ACTIVITY = "com.example.tvlauncher/.MainActivity"
    }

    private lateinit var ipField: EditText
    private lateinit var installBtn: Button
    private lateinit var log: TextView
    private val prefs by lazy { getSharedPreferences("setup", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dp = resources.displayMetrics.density
        val pad = (20 * dp).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
            setBackgroundColor(Color.parseColor("#101826"))
        }
        root.addView(TextView(this).apply {
            text = "TV Launcher Setup"; textSize = 26f; setTextColor(Color.WHITE)
        })
        root.addView(TextView(this).apply {
            text = "On the TV: Settings > Device Preferences > About > tap Build 7 times, then " +
                "Developer options > enable USB/Network debugging. Phone and TV must be on the same Wi-Fi. " +
                "Enter the TV's IP address below and accept the prompt on the TV."
            textSize = 14f; setTextColor(Color.parseColor("#B3FFFFFF")); setPadding(0, pad / 2, 0, pad / 2)
        })
        ipField = EditText(this).apply {
            hint = "TV IP address (e.g. 192.168.1.50)"
            setHintTextColor(Color.GRAY); setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT
            setText(prefs.getString("ip", ""))
        }
        root.addView(ipField)
        installBtn = Button(this).apply {
            text = "Install launcher on TV"
            setOnClickListener { startInstall() }
        }
        root.addView(installBtn)
        log = TextView(this).apply {
            textSize = 13f; setTextColor(Color.parseColor("#D0FFFFFF")); gravity = Gravity.START
            setTextIsSelectable(true)
        }
        root.addView(ScrollView(this).apply { addView(log) },
            LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun say(msg: String) = runOnUiThread { log.append(msg + "\n") }

    private fun startInstall() {
        val raw = ipField.text.toString().trim()
        if (raw.isEmpty()) { say("Enter the TV IP address first."); return }
        val host = raw.substringBefore(":")
        val port = raw.substringAfter(":", "5555").toIntOrNull() ?: 5555
        prefs.edit().putString("ip", raw).apply()
        installBtn.isEnabled = false
        Thread {
            try {
                say("Downloading latest launcher APK...")
                val apk = File(cacheDir, "launcher.apk")
                download(APK_URL, apk)
                say("Downloaded ${apk.length() / 1024} KB.")

                val priv = File(filesDir, "adbkey")
                val pub = File(filesDir, "adbkey.pub")
                if (!priv.exists()) AdbKeyPair.generate(priv, pub)
                val keys = AdbKeyPair.read(priv, pub)

                say("Connecting to $host:$port (accept the prompt on the TV)...")
                Dadb.create(host, port, keys).use { adb ->
                    say("Installing...")
                    adb.install(apk)
                    say("Installed. Opening the home chooser on the TV...")
                    try { adb.shell("cmd package set-home-activity $LAUNCHER_ACTIVITY") } catch (_: Throwable) {}
                    adb.shell("am start -a android.intent.action.MAIN -c android.intent.category.HOME")
                }
                say("Done! On the TV choose \"My Launcher\" as the home app if asked.")
            } catch (t: Throwable) {
                say("Error: ${t.javaClass.simpleName}: ${t.message}")
            } finally {
                runOnUiThread { installBtn.isEnabled = true }
            }
        }.start()
    }

    private fun download(url: String, out: File) {
        val c = URL(url).openConnection() as HttpURLConnection
        c.instanceFollowRedirects = true
        c.connectTimeout = 15000; c.readTimeout = 30000
        if (c.responseCode != 200) throw RuntimeException("Download failed: HTTP ${c.responseCode} (is there a published release?)")
        c.inputStream.use { i -> out.outputStream().use { o -> i.copyTo(o) } }
    }
}
