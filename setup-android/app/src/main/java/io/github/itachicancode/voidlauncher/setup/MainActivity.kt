package io.github.itachicancode.voidlauncher.setup

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
import java.io.File

/** Phone companion: downloads the latest Void Launcher APK and installs it on an Android TV over ADB (TCP). */
class MainActivity : Activity() {

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
            text = "Void Launcher Setup"; textSize = 26f; setTextColor(Color.WHITE)
        })
        root.addView(TextView(this).apply {
            text = "On the TV: Settings > Device Preferences > About > tap Build 7 times, then " +
                "Developer options > enable USB/Network debugging. Phone and TV must be on the same Wi-Fi. " +
                "Enter the TV's IP address below and accept the prompt on the TV."
            textSize = 14f; setTextColor(Color.parseColor("#B3FFFFFF")); setPadding(0, pad / 2, 0, pad / 2)
        })
        ipField = EditText(this).apply {
            hint = "TV IP address (for example 192.168.1.50)"
            setHintTextColor(Color.GRAY); setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT
            setText(prefs.getString("ip", ""))
        }
        root.addView(ipField)
        installBtn = Button(this).apply {
            text = "Install Void Launcher on TV"
            setOnClickListener { startInstall() }
        }
        root.addView(installBtn)
        log = TextView(this).apply {
            textSize = 13f; setTextColor(Color.parseColor("#D0FFFFFF")); gravity = Gravity.START
            setTextIsSelectable(true)
        }
        root.addView(ScrollView(this).apply { addView(log) }, LinearLayout.LayoutParams(-1, 0, 1f))
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
                say("Downloading latest Void Launcher APK...")
                val apk = File(cacheDir, "VoidLauncher.apk")
                Installer.download(Installer.APK_URL, apk)
                say("Downloaded ${apk.length() / 1024} KB.")
                say("Connecting to $host:$port (accept the prompt on the TV)...")
                val homeSet = Installer.install(host, port, Installer.keyPair(filesDir), apk, ::say)
                say(
                    if (homeSet) "Done! Void Launcher is now the home app."
                    else "Done! If the TV asks, choose Void Launcher as the home app. " +
                        "Some TVs only allow changing it in the TV's own settings."
                )
            } catch (t: Throwable) {
                say("Error: ${t.javaClass.simpleName}: ${t.message}")
            } finally {
                runOnUiThread { installBtn.isEnabled = true }
            }
        }.start()
    }
}
