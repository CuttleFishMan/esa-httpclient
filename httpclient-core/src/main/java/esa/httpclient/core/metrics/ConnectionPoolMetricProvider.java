/*
 * Copyright 2020 OPPO ESA Stack Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package esa.httpclient.core.metrics;

import java.net.SocketAddress;
import java.util.Map;

public interface ConnectionPoolMetricProvider {

    /**
     * Obtains all {@link ConnectionPoolMetric}s
     *
     * @return all, must be not null
     */
    Map<SocketAddress, ConnectionPoolMetric> all();

    /**
     * Obtains {@link SocketAddress} of specified {@link SocketAddress}
     *
     * @param address address
     * @return metric
     */
    default ConnectionPoolMetric get(SocketAddress address) {
        return all().get(address);
    }

}
