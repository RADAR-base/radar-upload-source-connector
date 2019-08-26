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

package org.radarbase.upload.auth

import java.security.Principal
import javax.ws.rs.core.SecurityContext

/**
 * Security context from currently parsed authentication.
 */
class RadarSecurityContext(
        /** Get the parsed authentication.  */
        val auth: Auth) : SecurityContext {

    override fun getUserPrincipal() = Principal { auth.userId }

    /**
     * Maps roles in the shape `"project:role"` to a Management Portal role. Global roles
     * take the shape of `":global_role"`. This allows for example a
     * `@RolesAllowed(":SYS_ADMIN")` annotation to resolve correctly.
     * @param role role to be mapped
     * @return `true` if the authentication contains given project/role,
     * `false` otherwise
     */
    override fun isUserInRole(role: String): Boolean {
        val projectRole = role.split(":")
        return projectRole.size == 2 && auth.hasRole(projectRole[0], projectRole[1])
    }

    override fun isSecure() = true

    override fun getAuthenticationScheme() = "JWT"
}
