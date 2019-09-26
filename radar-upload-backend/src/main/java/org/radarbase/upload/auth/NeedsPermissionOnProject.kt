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

/**
 * Indicates that a method needs an authenticated user that has a certain permission.
 */
@Target(AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class NeedsPermissionOnProject(
        /**
         * Entity that the permission is needed on.
         */
        val entity: Permission.Entity,
        /**
         * Operation on given entity that the permission is needed for.
         */
        val operation: Permission.Operation,
        /** Project path parameter */
        val projectPathParam: String)
