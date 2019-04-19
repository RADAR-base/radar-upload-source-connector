package org.radarbase.upload.inject

import org.radarbase.upload.Config
import org.radarbase.upload.api.SourceTypeMapper
import org.radarbase.upload.doa.SourceTypeRepository
import org.slf4j.LoggerFactory
import javax.ws.rs.core.Context

class SourceTypeLoaderImpl{

    @Context
    lateinit var sourceTypeRepository: SourceTypeRepository

    @Context
    lateinit var config: Config

    @Context
    lateinit var sourceTypeMapper: SourceTypeMapper

    fun loadSourceTypes() {
        logger.info("Loading....")
        this.config.sourceTypes?.map { logger.info(it.toString()) }
    }

    companion object {

        val logger = LoggerFactory.getLogger(SourceTypeLoaderImpl::class.java)
    }
}
