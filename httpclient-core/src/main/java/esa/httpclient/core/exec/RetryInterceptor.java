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
package esa.httpclient.core.exec;

import esa.commons.Checks;
import esa.commons.logging.Logger;
import esa.httpclient.core.HttpRequest;
import esa.httpclient.core.HttpResponse;
import esa.httpclient.core.RequestType;
import esa.httpclient.core.exception.RetryException;
import esa.httpclient.core.util.Futures;
import esa.httpclient.core.util.LoggerUtils;

import java.util.concurrent.CompletableFuture;
import java.util.function.IntToLongFunction;

import static esa.httpclient.core.ContextNames.MAX_RETRIES;

public class RetryInterceptor implements Interceptor {

    static final String HAS_RETRIED_COUNT = "$retried.count";
    private static final Logger logger = LoggerUtils.logger();

    private final RetryPredicate predicate;
    private final IntToLongFunction intervalMs;

    public RetryInterceptor(RetryPredicate predicate, IntToLongFunction intervalMs) {
        Checks.checkNotNull(predicate, "RetryPredicate must not be null");
        this.predicate = predicate;
        this.intervalMs = intervalMs;
    }

    @Override
    public CompletableFuture<HttpResponse> proceed(HttpRequest request, ExecChain next) {
        // Pass directly if not configured
        final int maxRetries = next.ctx().getUncheckedAttr(MAX_RETRIES, 0);
        if (maxRetries < 1) {
            return next.proceed(request);
        }
        if (RequestType.CHUNK == request.type()) {
            next.ctx().removeAttr(MAX_RETRIES);
            if (logger.isDebugEnabled()) {
                logger.debug("Retry is ignored, request: {}, maxRetries: {}", request, maxRetries);
            }
            return next.proceed(request);
        }

        final CompletableFuture<HttpResponse> response = new CompletableFuture<>();
        doRetry(response, request, next, maxRetries);
        return response;
    }

    @Override
    public int getOrder() {
        return -4000;
    }

    protected void doRetry(CompletableFuture<HttpResponse> response,
                           HttpRequest request,
                           ExecChain next,
                           int maxRetries) {
        if (response.isDone()) {
            return;
        }

        next.proceed(request).whenComplete((rsp, th) -> {
            try {
                // Update hasRetriedCount immediately.
                // may be it will be used further, such as metrics, logging...
                int hasRetriedCount = next.ctx().getUncheckedAttr(HAS_RETRIED_COUNT, -1) + 1;
                next.ctx().removeAttr(HAS_RETRIED_COUNT);
                next.ctx().setAttr(HAS_RETRIED_COUNT, hasRetriedCount);

                // Judge whether the request has been handled successfully.
                boolean canRetry = predicate.canRetry(request, rsp, next.ctx(), th);
                if (!canRetry) {
                    if (rsp != null) {
                        response.complete(rsp);
                    } else {
                        response.completeExceptionally(Futures.unwrapped(th));
                    }
                    return;
                }

                if (hasRetriedCount < maxRetries) {
                    if (intervalMs != null) {
                        try {
                            backOff(request, hasRetriedCount + 1, intervalMs);
                        } catch (InterruptedException ex) {
                            response.completeExceptionally(new RetryException("Interrupted during retry interval",
                                    ex));
                        }
                    }

                    if (logger.isDebugEnabled()) {
                        logger.debug("Begin to retry request: {}, retryCount: {}",
                                request, hasRetriedCount + 1);
                    }

                    doRetry(response, request, next, maxRetries);
                } else {
                    response.completeExceptionally(new RetryException(String
                            .format("Failed to proceed request: " + request.uri().netURI().toString() +
                                    " after maxRetries: %d", maxRetries)));
                }
            } catch (Throwable ex) {
                response.completeExceptionally(new RetryException("Unexpected error while retrying", ex));
            }
        });
    }

    protected void backOff(HttpRequest request, int retryCount, IntToLongFunction intervalMs)
            throws InterruptedException {
        long interval = intervalMs.applyAsLong(retryCount);
        if (interval <= 0L) {
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Begin to back off before retrying request: {}, retryCount: {}", request, retryCount);
        }
        Thread.sleep(interval);
    }

}
