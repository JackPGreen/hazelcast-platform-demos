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

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * <p>For remote task execution, return the CPU count available
 * to the JVM on the execution target host.
 * </p>
 */
public class CountProcessorsCallable implements Callable<Integer>, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public Integer call() throws Exception {
        return Runtime.getRuntime().availableProcessors();
    }

}
