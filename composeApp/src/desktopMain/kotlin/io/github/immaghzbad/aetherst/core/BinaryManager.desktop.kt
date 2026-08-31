package io.github.immaghzbad.aetherst.shared.core

import io.github.immaghzbad.aetherst.platform.PlatformContext
import java.io.File

class DesktopBinaryManager : BinaryManager {
    override fun prepareBinary(name: String): String {
        val binName = if (System.getProperty("os.name").lowercase().contains("win")) {
            if (name.endsWith(".exe")) name else "$name.exe"
        } else {
            name
        }

        System.getProperty("compose.application.resources.dir")?.let {
            val file = File(it, binName)
            if (file.exists()) return file.absolutePath
        }

        val userDir = File(System.getProperty("user.dir"))
        val devDirs = listOf(userDir, File(userDir, "bin"), File(userDir, "src/desktopMain/resources/bin"))
        for (dir in devDirs) {
            val file = File(dir, binName)
            if (file.exists()) return file.absolutePath
        }

        val appData = System.getenv("AppData") ?: System.getProperty("user.home")
        val targetDir = File(appData, "AetherST-Tunnel/bin")
        if (!targetDir.exists()) targetDir.mkdirs()
        val targetFile = File(targetDir, binName)

        if (!targetFile.exists()) {
            try {
                javaClass.getResourceAsStream("/bin/$binName")?.use { input ->
                    targetFile.outputStream().use { output -> input.copyTo(output) }
                    if (!System.getProperty("os.name").lowercase().contains("win")) targetFile.setExecutable(true)
                }
                if (binName.contains("hev-socks5-tunnel")) {
                    extractResource("wintun.dll", targetDir)
                    extractResource("msys-2.0.dll", targetDir)
                }
            } catch (_: Exception) {}
        } else {
            try {
                if (binName.contains("hev-socks5-tunnel")) {
                    extractResource("wintun.dll", targetDir)
                    extractResource("msys-2.0.dll", targetDir)
                }
            } catch (_: Exception) {}
        }

        return if (targetFile.exists()) targetFile.absolutePath else binName
    }

    private fun extractResource(name: String, targetDir: File) {
        val targetFile = File(targetDir, name)
        if (!targetFile.exists()) {
            javaClass.getResourceAsStream("/bin/$name")?.use { input ->
                targetFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }
}

actual fun getBinaryManager(context: PlatformContext): BinaryManager = DesktopBinaryManager()
