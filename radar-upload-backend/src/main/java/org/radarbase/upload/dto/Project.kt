package org.radarbase.upload.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class Project(val id: Long, @JsonProperty("projectName") val name: String, @JsonProperty("humanReadableProjectName") val humanReadableName: String? = null, val location: String? = null, val organization: String? = null, val description: String? = null)

data class User(val id: String, val projectId: String, val externalId: String? = null, val status: String)
