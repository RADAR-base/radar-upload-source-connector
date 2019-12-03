package org.radarbase.connect.upload.io

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.io.Closeable
import java.io.InputStream
import java.nio.file.Path
import java.util.*


class SftpFileUploader(credentials: SftpCredentials) : Closeable {
    private val jsch = JSch()
    private val session: Session

    init {
        jsch.addIdentity(credentials.privateKeyFile, credentials.privateKeyPassphrase)
        session = jsch.getSession(credentials.username, credentials.host, credentials.port)
        session.setPassword(credentials.password)

        val config = Properties()
        config["StrictHostKeyChecking"] = "no"
        session.setConfig(config)
        session.connect()
    }

    fun upload(path: Path, stream: InputStream) {
        // copy remote log file to localhost.
        // copy remote log file to localhost.
        (session.openChannel("sftp") as ChannelSftp).run {
            connect()
            put(stream, path.toString())
            exit()
        }
    }

    override fun close() {
        session.disconnect()
    }

    data class SftpCredentials(
            val host: String,
            val port: Int = 22,
            val username: String,
            val privateKeyFile: String? = null,
            val password: String? = null,
            val privateKeyPassphrase: String? = null)
}

