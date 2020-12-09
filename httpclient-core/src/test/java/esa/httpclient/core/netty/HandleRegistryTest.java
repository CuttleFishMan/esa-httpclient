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

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;

class HandleRegistryTest {

    @Test
    void testBasic() {
        final HandleRegistry registry = new HandleRegistry(1, 0);
        then(registry.remove(1)).isNull();

        final NettyHandle handle1 = mock(NettyHandle.class);
        then(registry.put(handle1)).isEqualTo(1);
        then(registry.get(1)).isSameAs(handle1);
        then(registry.remove(1)).isSameAs(handle1);

        final NettyHandle handle2 = mock(NettyHandle.class);
        then(registry.put(handle2)).isEqualTo(2);
    }

    @Test
    void testParallel() throws InterruptedException {
        final HandleRegistry registry = new HandleRegistry(1, 0);
        final CountDownLatch count = new CountDownLatch(10);
        final AtomicInteger error = new AtomicInteger();

        for (int i = 0; i < 10; i++) {
            final int j = i % 3;
            if (j == 0) {
                new Thread(() -> {
                    try {
                        registry.put(mock(NettyHandle.class));
                    } catch (Throwable ex) {
                        error.incrementAndGet();
                    } finally {
                        count.countDown();
                    }
                }).start();
            } else if (j == 1) {
                new Thread(() -> {
                    try {
                        registry.get(j);
                    } catch (Throwable ex) {
                        error.incrementAndGet();
                    } finally {
                        count.countDown();
                    }
                }).start();
            } else {
                new Thread(() -> {
                    try {
                        registry.remove(j);
                    } catch (Throwable ex) {
                        error.incrementAndGet();
                    } finally {
                        count.countDown();
                    }
                }).start();
            }
        }

        count.await();
        then(error.intValue()).isEqualTo(0);
    }
}
