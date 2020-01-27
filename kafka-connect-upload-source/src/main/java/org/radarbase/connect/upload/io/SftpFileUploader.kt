package org.radarbase.connect.upload.io

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpException
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.lang.Exception
import java.nio.file.Path
import java.util.*


class SftpFileUploader(credentials: SftpCredentials) : FileUploader {
    private val jsch = JSch()
    private val session: Session
    private val sftpChannel: ChannelSftp

    init {
        credentials.privateKeyFile?.let {
            jsch.addIdentity(it, credentials.privateKeyPassphrase)
        }
        session = jsch.getSession(credentials.username, credentials.host, credentials.port)
        session.setPassword(credentials.password)

        val config = Properties()
        config["StrictHostKeyChecking"] = "no"
        session.setConfig(config)
        session.connect()
        sftpChannel = session.openChannel("sftp") as ChannelSftp
        sftpChannel.connect()
    }

    override fun upload(path: Path, stream: InputStream) {
        // copy remote log file to localhost.
        // copy remote log file to localhost.
        sftpChannel.run {
            try {
                logger.info("Uploading data to ${path}")
                put(stream, path.toString())
            } catch (ex: SftpException) {
                mkdirs(path)
                put(stream, path.toString())
            }
        }
    }

    override fun close() {
        var exception: Exception? = null
        try {
            sftpChannel.disconnect()
        } catch (ex: Exception) {
            exception = ex
        }

        session.disconnect()
        exception?.let { throw it }
    }

    data class SftpCredentials(
            val host: String,
            val port: Int = 22,
            val username: String,
            val privateKeyFile: String? = null,
            val password: String? = null,
            val privateKeyPassphrase: String? = null)

    companion object {
        private val logger = LoggerFactory.getLogger(SftpFileUploader::class.java)
        fun ChannelSftp.mkdirs(path: Path) {
            var isMissing = false
            for (i in 1 until path.nameCount) {
                val nextDirectory = path.subpath(0, i).toString()
                if (!isMissing) {
                    try {
                        lstat(nextDirectory)
                    } catch (ex: SftpException) {
                        if (ex.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                            isMissing = true
                        } else throw ex // unknown exception
                    }
                }
                if (isMissing) {
                    logger.info("Creating $nextDirectory")
                    mkdir(nextDirectory)
                }
            }
        }
    }
}
