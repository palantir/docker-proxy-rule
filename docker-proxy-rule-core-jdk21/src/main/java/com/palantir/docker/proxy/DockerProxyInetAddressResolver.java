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
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class DockerProxyInetAddressResolver implements InetAddressResolver {
    private final Supplier<DockerNameService> dockerNameService;

    public DockerProxyInetAddressResolver(Supplier<DockerNameService> dockerNameService) {
        this.dockerNameService = dockerNameService;
    }

    @Override
    public Stream<InetAddress> lookupByName(String host, LookupPolicy _lookupPolicy) throws UnknownHostException {
        return Arrays.stream(dockerNameService.get().lookupAllHostAddr(host));
    }

    @Override
    public String lookupByAddress(byte[] addr) throws UnknownHostException {
        return dockerNameService.get().getHostByAddr(addr);
    }
}
