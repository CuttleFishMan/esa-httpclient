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
package esa.httpclient.core.netty;

import esa.httpclient.core.Context;
import esa.httpclient.core.HttpRequest;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;

class ServerSelectorTest {

    @Test
    void testDefault() {
        ServerSelector selector = ServerSelector.DEFAULT;

        final Context context = mock(Context.class);
        final HttpRequest request1 = HttpRequest.get("http://127.0.0.1").build();

        then(selector.select(request1, context))
                .isEqualTo(InetSocketAddress.createUnresolved("127.0.0.1", 80));

        final HttpRequest request2 = HttpRequest.get("https://127.0.0.1").build();
        then(selector.select(request2, context))
                .isEqualTo(InetSocketAddress.createUnresolved("127.0.0.1", 443));

        final HttpRequest request3 = HttpRequest.get("https://127.0.0.1:8989").build();
        then(selector.select(request3, context))
                .isEqualTo(InetSocketAddress.createUnresolved("127.0.0.1", 8989));
    }
}
