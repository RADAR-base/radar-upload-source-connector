/*
 *
 *  * Copyright 2019 The Hyve
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */

package org.radarbase.upload.inject

import org.radarbase.upload.auth.NeedsPermission
import org.radarbase.upload.auth.NeedsPermissionOnProject
import org.radarbase.upload.auth.NeedsPermissionOnUser
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
        val resourceMethod = resourceInfo.resourceMethod
        if (resourceMethod.isAnnotationPresent(NeedsPermission::class.java)
                || resourceMethod.isAnnotationPresent(NeedsPermissionOnProject::class.java)
                || resourceMethod.isAnnotationPresent(NeedsPermissionOnUser::class.java)) {
            context.register(PermissionFilter::class.java, Priorities.AUTHORIZATION)
        }
    }
}
