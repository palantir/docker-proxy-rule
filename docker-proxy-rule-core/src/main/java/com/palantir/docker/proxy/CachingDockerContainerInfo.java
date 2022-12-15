/*
 * (c) Copyright 2017 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.docker.proxy;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * CachingDockerContainerInfo will cache and refresh container info. If the refresh
 * fails four times in a row, the entry will be removed and the next call will return
 * the exception to you if it happens again.
 */
@SuppressWarnings("checkstyle:BanGuavaCaches")
public final class CachingDockerContainerInfo implements DockerContainerInfo {
    private final DockerContainerInfo delegate;
    private final LoadingCache<String, Optional<String>> ipForHostCache;
    private final LoadingCache<String, Optional<String>> hostForIpCache;

    public CachingDockerContainerInfo(DockerContainerInfo delegate) {
        // It takes up to 1s to query docker so we set this to be under a multiple of 5, 10, and 15 by at least 2s
        this(delegate, 53, TimeUnit.SECONDS);
    }

    @VisibleForTesting
    CachingDockerContainerInfo(DockerContainerInfo delegate, long refreshDuration, TimeUnit refreshUnit) {
        this.delegate = delegate;
        this.ipForHostCache = CacheBuilder.newBuilder()
                .expireAfterWrite(4 * refreshDuration, refreshUnit)
                .refreshAfterWrite(refreshDuration, refreshUnit)
                .build(CacheLoader.from(delegate::getIpForHost));
        this.hostForIpCache = CacheBuilder.newBuilder()
                .expireAfterWrite(4 * refreshDuration, refreshUnit)
                .refreshAfterWrite(refreshDuration / 4, refreshUnit)
                .build(CacheLoader.from(delegate::getHostForIp));
    }

    @Override
    public Optional<String> getIpForHost(String hostname) {
        Optional<String> ip = ipForHostCache.getUnchecked(hostname);
        if (!ip.isPresent()) {
            ipForHostCache.invalidate(hostname);
        }
        return ip;
    }

    @Override
    public Optional<String> getHostForIp(String ip) {
        Optional<String> host = hostForIpCache.getUnchecked(ip);
        if (!host.isPresent()) {
            hostForIpCache.invalidate(ip);
        }
        return host;
    }

    @Override
    public String getNetworkName() {
        return delegate.getNetworkName();
    }

    @Override
    public Optional<String> getImageNameOverride() {
        return delegate.getImageNameOverride();
    }
}
