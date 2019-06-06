package org.radarbase.connect.upload.util


class VersionUtil {
    companion object {
        fun getVersion(): String = VersionUtil::class.java.`package`.implementationVersion ?: "0.0.0.0"
    }
}
