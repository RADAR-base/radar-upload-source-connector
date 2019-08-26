package org.radarbase.connect.upload.exception

class NotAuthorizedException(message: String) : RuntimeException(message)

class BadGatewayException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class ConflictException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class InvalidFormatException(message: String) : RuntimeException(message)
