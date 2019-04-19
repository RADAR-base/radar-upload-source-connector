package org.radarbase.connect.upload.util


class VersionUtil {
    companion object {
        fun getVersion() = VersionUtil::class.java.`package`.implementationVersion
    }
}
