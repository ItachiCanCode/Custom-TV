package io.github.itachicancode.voidlauncher.setup

import dadb.Dadb
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Integration test: runs the real install flow against an Android device or emulator.
 * Needs TV_ADB_HOST, TV_ADB_PORT and TV_APK in the environment (CI sets them); skipped otherwise.
 */
class InstallerTest {
    private val host: String? = System.getenv("TV_ADB_HOST")
    private val port: Int = System.getenv("TV_ADB_PORT")?.toIntOrNull() ?: 5555
    private val apk: File? = System.getenv("TV_APK")?.let { File(it) }

    @Test
    fun installsLauncherAndReportsHomeStateTruthfully() {
        assumeTrue("no TV or emulator configured", host != null && apk != null && apk.exists())
        val keys = Installer.keyPair(Files.createTempDirectory("adbkeys").toFile())
        val steps = mutableListOf<String>()
        val homeSet = Installer.install(host!!, port, keys, apk!!) { steps += it }
        println("installer steps: $steps, homeSet=$homeSet")

        Dadb.create(host, port, keys).use { adb ->
            val packages = adb.shell("pm list packages").allOutput
            assertTrue("launcher not installed: $packages", packages.contains("package:" + Installer.LAUNCHER_PACKAGE))
            val home = adb.shell(
                "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME"
            ).allOutput
            if (homeSet) assertTrue("reported home set but resolved home is: $home", home.contains(Installer.LAUNCHER_PACKAGE))
        }
    }
}
