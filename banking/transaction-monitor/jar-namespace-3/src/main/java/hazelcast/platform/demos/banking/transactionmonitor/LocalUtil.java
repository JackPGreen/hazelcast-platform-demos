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

package hazelcast.platform.demos.banking.transactionmonitor;

import java.net.InetSocketAddress;

import com.hazelcast.cluster.Member;

/**
 * <p>Utilities for this jar.
 * </p>
 */
public class LocalUtil {

    /**
     * <p>Address formatting.
     * </p>
     *
     * @param member
     * @return
     */
    static String prettyPrintMember(Member member) {
        try {
            InetSocketAddress inetSocketAddress = member.getAddress().getInetSocketAddress();
            String host = inetSocketAddress.getAddress().getHostAddress();
            int port = inetSocketAddress.getPort();
            return host + ":" + port;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

}
