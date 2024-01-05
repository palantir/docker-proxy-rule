/*
 * (c) Copyright 2024 Palantir Technologies Inc. All rights reserved.
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.spi.InetAddressResolver;
import java.util.function.Supplier;
import java.util.stream.Stream;

class ForwardingInetAddressResolver implements InetAddressResolver {
    private final InetAddressResolver delegate;
    private final InetAddressResolver fallback;
    private final Supplier<Boolean> delegateEnabled;

    ForwardingInetAddressResolver(
            InetAddressResolver delegate, InetAddressResolver fallback, Supplier<Boolean> delegateEnabled) {
        this.delegate = delegate;
        this.fallback = fallback;
        this.delegateEnabled = delegateEnabled;
    }

    @Override
    public Stream<InetAddress> lookupByName(String host, LookupPolicy lookupPolicy) throws UnknownHostException {
        if (!delegateEnabled.get()) {
            return fallback.lookupByName(host, lookupPolicy);
        }

        try {
            return delegate.lookupByName(host, lookupPolicy);
        } catch (UnknownHostException e) {
            if (fallback != null) {
                return fallback.lookupByName(host, lookupPolicy);
            }
            throw e;
        }
    }

    @Override
    public String lookupByAddress(byte[] addr) throws UnknownHostException {
        if (!delegateEnabled.get()) {
            return fallback.lookupByAddress(addr);
        }

        try {
            return delegate.lookupByAddress(addr);
        } catch (UnknownHostException e) {
            if (fallback != null) {
                return fallback.lookupByAddress(addr);
            }
            throw e;
        }
    }
}
