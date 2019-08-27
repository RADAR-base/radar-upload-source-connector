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

import org.radarcns.auth.authorization.Permission

interface Auth {
    val defaultProject: String?
    val userId: String?

    fun checkSourcePermission(permission: Permission, projectId: String?, userId: String?, sourceId: String?)
    fun checkUserPermission(permission: Permission, projectId: String?, userId: String?)
    fun checkProjectPermission(permission: Permission, projectId: String?)
    fun hasRole(projectId: String, role: String): Boolean
    fun hasPermission(permission: Permission): Boolean
    fun hasPermissionOnProject(permission: Permission, projectId: String): Boolean
    fun hasPermissionOnSubject(permission: Permission, projectId: String, userId: String): Boolean
    fun authorizedProjects(permission: Permission): AccessRestriction
    val isClientCredentials: Boolean
}

sealed class AccessRestriction

object AllAccess : AccessRestriction()
data class RestrictedAccess(var access: Set<String>) : AccessRestriction()
