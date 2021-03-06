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

package org.radarbase.connect.upload.exception

class ConflictException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

open class ConversionFailedException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class DataProcessorNotFoundException(message: String) : ConversionFailedException(message)

class InvalidFormatException(message: String) : ConversionFailedException(message)

open class ConversionTemporarilyFailedException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class NotAuthorizedException(message: String) : ConversionTemporarilyFailedException(message)

class BadGatewayException(message: String, cause: Throwable? = null) : ConversionTemporarilyFailedException(message, cause)
