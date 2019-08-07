package org.radarbase.upload.inject

import org.glassfish.jersey.internal.inject.AbstractBinder
import org.glassfish.jersey.server.ResourceConfig
import org.radarbase.auth.jersey.AuthConfig
import org.radarbase.auth.jersey.ManagementPortalResourceEnhancer
import org.radarbase.auth.jersey.ProjectService
import org.radarbase.auth.jersey.RadarJerseyResourceEnhancer
import org.radarbase.upload.Config
import org.radarbase.upload.service.managementportal.MPClient
import org.radarbase.upload.service.managementportal.MPProjectService
import org.radarbase.upload.service.UploadProjectService
import javax.inject.Singleton

/** This binder needs to register all non-Jersey classes, otherwise initialization fails. */
class ManagementPortalResourceConfig : UploadResourceConfig() {
    override fun registerAuthentication(resources: ResourceConfig, binder: AbstractBinder, config: Config) {
        binder.apply {
            bind(MPClient::class.java)
                    .to(MPClient::class.java)
                    .`in`(Singleton::class.java)

            bind(MPProjectService::class.java)
                    .to(UploadProjectService::class.java)
                    .`in`(Singleton::class.java)

            bind(MPProjectService::class.java)
                    .to(ProjectService::class.java)
                    .`in`(Singleton::class.java)

        }
        RadarJerseyResourceEnhancer(AuthConfig(
                managementPortalUrl = config.managementPortalUrl,
                jwtResourceName = "res_upload")).enhance(resources, binder)
        ManagementPortalResourceEnhancer()
                .enhance(resources, binder)
    }
}
