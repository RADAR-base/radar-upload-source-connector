package org.radarbase.connect.upload.io

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpException
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.lang.Exception
import java.net.ConnectException
import java.net.URI
import java.nio.file.Path
import java.util.*


class SftpFileUploader(override val config: FileUploaderFactory.FileUploaderConfig) : FileUploaderFactory.FileUploader {
    override val type: String
        get() = "sftp"
    private val jsch = JSch()
    private val session: Session
    private val sftpChannel: ChannelSftp

    init {
        if (config.targetEndpoint.isEmpty()) throw ConnectException("upload.source.file.target.endpoint should have a valid url with format sftp://hostname or sftp://hostname:port")
        config.username ?: throw ConnectException("upload.source.file.uploader.username must be configured for one or more of the selected converters")
        config.password ?: throw ConnectException("upload.source.file.uploader.password must be configured for one or more of the selected converters")

        logger.info("Initializing sftp file uploader")
        val endpoint = URI(config.targetEndpoint)
        config.sshPrivateKey?.let {
            jsch.addIdentity(it, config.sshPassPhrase)
        }
        session = jsch.getSession(config.username, endpoint.host, if (endpoint.port == -1) 22 else endpoint.port)
        session.setPassword(config.password)

        val sessionConfig = Properties()
        sessionConfig["StrictHostKeyChecking"] = "no"
        session.setConfig(sessionConfig)
        session.connect()
        sftpChannel = session.openChannel("sftp") as ChannelSftp
        sftpChannel.connect()
        logger.info("SftpFileUploader Connection established ...")
        logger.info("Files will be uploaded using SFTP to $endpoint and root directory ${config.targetRoot}")
    }

    override fun upload(path: Path, stream: InputStream, size: Long?) {
        // copy remote log file to localhost.
        sftpChannel.run {
            try {
                logger.info("Uploading data to ${path}")
                put(stream, path.toString())
            } catch (ex: SftpException) {
                logger.error("Could not upload file... Retrying", ex)
                mkdirs(path)
                put(stream, path.toString())
            }
        }
    }

    override fun close() {
        logger.debug("Closing SftpFileUploader")
        var exception: Exception? = null
        try {
            sftpChannel.disconnect()
        } catch (ex: Exception) {
            exception = ex
        }

        session.disconnect()
        exception?.let { throw it }
    }


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
