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

import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

public final class DockerNameService {
    private final DockerContainerInfo containerInfo;

    public DockerNameService(DockerContainerInfo containerInfo) {
        this.containerInfo = containerInfo;
    }

    public InetAddress[] lookupAllHostAddr(String hostname) throws UnknownHostException {
        Optional<String> containerIp = containerInfo.getIpForHost(hostname);

        if (containerIp.isPresent()) {
            return new InetAddress[] {InetAddresses.forString(containerIp.get())};
        }
        throw new UnknownHostException(hostname);
    }

    public String getHostByAddr(byte[] bytes) throws UnknownHostException {
        String ipAddress = InetAddress.getByAddress(bytes).getHostAddress();
        Optional<String> containerHost = containerInfo.getHostForIp(ipAddress);

        if (containerHost.isPresent()) {
            return containerHost.get();
        }
        throw new UnknownHostException(ipAddress);
    }
}
