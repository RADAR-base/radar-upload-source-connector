package org.radarbase.upload.listener

import org.slf4j.LoggerFactory
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener
import javax.servlet.annotation.WebListener



@WebListener
class UploaderListener : ServletContextListener {

    override fun contextInitialized(servletContextEvent: ServletContextEvent) {
        log.info(" Initializing context.")
    }

    override fun contextDestroyed(servletContextEvent: ServletContextEvent) {
        log.info("SampleAdminListener Destroying context.")
    }

    companion object {
        private val log = LoggerFactory.getLogger(UploaderListener::class.java)
    }
}
