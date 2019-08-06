package org.radarbase.upload.dto

data class ProjectList(val projects: List<Project>)

data class Project(val id: String,  val name: String? = null, val location: String? = null, val organization: String? = null, val description: String? = null)

data class UserList(val users: List<User>)

data class User(val id: String, val projectId: String, val externalId: String? = null, val status: String)
