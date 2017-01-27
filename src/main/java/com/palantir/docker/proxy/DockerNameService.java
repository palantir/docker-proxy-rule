/*
 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.proxy;

import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

public class DockerNameService implements sun.net.spi.nameservice.NameService {
    private final DockerContainerInfo containerInfo;

    public DockerNameService(DockerContainerInfo containerInfo) {
        this.containerInfo = containerInfo;
    }

    @Override
    public InetAddress[] lookupAllHostAddr(String hostname) throws UnknownHostException {
        Optional<String> containerIp = containerInfo.getIpForHost(hostname);

        if (containerIp.isPresent()) {
            return new InetAddress[] { InetAddresses.forString(containerIp.get()) };
        }
        throw new UnknownHostException(hostname);
    }

    @Override
    public String getHostByAddr(byte[] bytes) throws UnknownHostException {
        String ipAddress = InetAddress.getByAddress(bytes).getHostAddress();
        Optional<String> containerHost = containerInfo.getHostForIp(ipAddress);

        if (containerHost.isPresent()) {
            return containerHost.get();
        }
        throw new UnknownHostException(ipAddress);
    }
}
