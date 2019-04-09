package org.radarbase.upload.inject

import org.radarbase.upload.auth.NeedsPermission
import org.radarbase.upload.filter.PermissionFilter
import javax.ws.rs.Priorities
import javax.ws.rs.container.DynamicFeature
import javax.ws.rs.container.ResourceInfo
import javax.ws.rs.core.FeatureContext
import javax.ws.rs.ext.Provider

/** Authorization for different auth tags. */
@Provider
class AuthorizationFeature : DynamicFeature {
    override fun configure(resourceInfo: ResourceInfo, context: FeatureContext) {
        if (resourceInfo.resourceMethod.isAnnotationPresent(NeedsPermission::class.java)) {
            context.register(PermissionFilter::class.java, Priorities.AUTHORIZATION)
        }
    }
}
