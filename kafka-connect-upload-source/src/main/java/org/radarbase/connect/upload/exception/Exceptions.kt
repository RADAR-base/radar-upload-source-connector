package org.radarbase.connect.upload.exception

class NotAuthorizedException(message: String) : RuntimeException(message)

class BadGatewayException(message: String) : RuntimeException(message)

class InvalidFormatException(message: String) : RuntimeException(message)
