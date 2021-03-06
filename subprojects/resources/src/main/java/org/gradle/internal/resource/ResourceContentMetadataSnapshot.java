/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.resource;

import com.google.common.hash.HashCode;
import org.gradle.internal.file.FileType;

/**
 * An immutable snapshot of the type and content of a resource. Does not include any information about the identity of the resource. The resource may not exist.
 */
public interface ResourceContentMetadataSnapshot {
    FileType getType();

    HashCode getContentMd5();
}
