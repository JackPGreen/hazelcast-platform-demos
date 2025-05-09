/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.platform.demos.telco.churn;

/**
 * <p>Utility constants unique to this module, but that make
 * sense to store here, even if only used once from Java (as
 * may be elsewhere such as Javascript).
 * </p>
 */
public class MyLocalConstants {

    // Web Sockets
    public static final String WEBSOCKET_ENDPOINT = "hazelcast";
    public static final String WEBSOCKET_TOPICS_PREFIX = "topics";
    public static final String WEBSOCKET_SIZES_SUFFIX = "sizes";

}
