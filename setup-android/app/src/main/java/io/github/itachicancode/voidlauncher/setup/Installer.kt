package io.github.itachicancode.voidlauncher.setup

import dadb.AdbKeyPair
import dadb.Dadb
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Install logic with no Android APIs, so it can be unit-tested against a real device or emulator. */
object Installer {
    const val APK_URL = "https://github.com/ItachiCanCode/Custom-TV/releases/latest/download/VoidLauncher.apk"
    const val LAUNCHER_PACKAGE = "io.github.itachicancode.voidlauncher"
    const val LAUNCHER_ACTIVITY = "$LAUNCHER_PACKAGE/.MainActivity"

    fun download(url: String, out: File) {
        val c = URL(url).openConnection() as HttpURLConnection
        c.instanceFollowRedirects = true
        c.connectTimeout = 15000
        c.readTimeout = 30000
        if (c.responseCode != 200) {
            throw RuntimeException("Download failed: HTTP ${c.responseCode} (is there a published release?)")
        }
        c.inputStream.use { i -> out.outputStream().use { o -> i.copyTo(o) } }
    }

    /** Loads this app's ADB key from [dir], creating it on first use. */
    fun keyPair(dir: File): AdbKeyPair {
        val priv = File(dir, "adbkey")
        val pub = File(dir, "adbkey.pub")
        if (!priv.exists()) AdbKeyPair.generate(priv, pub)
        return AdbKeyPair.read(priv, pub)
    }

    /**
     * Installs [apk] on the TV at [host]:[port], tries to make Void Launcher the home app and
     * opens the home screen. Returns true if the TV accepted the "set home app" command.
     */
    fun install(host: String, port: Int, keys: AdbKeyPair, apk: File, say: (String) -> Unit): Boolean {
        Dadb.create(host, port, keys).use { adb ->
            say("Installing...")
            adb.install(apk)
            say("Installed.")
            val r = try {
                adb.shell("cmd package set-home-activity $LAUNCHER_ACTIVITY")
            } catch (_: Throwable) {
                null
            }
            val homeSet = r != null && r.exitCode == 0 &&
                !r.allOutput.contains("Exception") && !r.allOutput.contains("Error")
            adb.shell("am start -a android.intent.action.MAIN -c android.intent.category.HOME")
            return homeSet
        }
    }
}
