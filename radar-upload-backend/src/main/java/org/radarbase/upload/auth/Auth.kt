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
    fun authorizedProjects(permission: Permission): AccessRestriction
    val isClientCredentials: Boolean
}

sealed class AccessRestriction

object AllAccess : AccessRestriction()
data class RestrictedAccess(var access: Set<String>) : AccessRestriction()
