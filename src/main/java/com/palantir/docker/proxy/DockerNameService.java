/*
 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.proxy;

import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.function.Supplier;

public class DockerNameService implements sun.net.spi.nameservice.NameService {
    private final Supplier<ProjectInfoMappings> projectInfo;

    public DockerNameService(Supplier<ProjectInfoMappings> projectInfo) {
        this.projectInfo = projectInfo;
    }

    @Override
    public InetAddress[] lookupAllHostAddr(String hostname) throws UnknownHostException {
        Map<String, String> hostToIp = projectInfo.get().getHostToIp();

        if (hostToIp.containsKey(hostname)) {
            return new InetAddress[] { InetAddresses.forString(hostToIp.get(hostname)) };
        }
        throw new UnknownHostException(hostname);
    }

    @Override
    public String getHostByAddr(byte[] bytes) throws UnknownHostException {
        Multimap<String, String> ipToHosts = projectInfo.get().getIpToHosts();
        String ipAddress = InetAddress.getByAddress(bytes).getHostAddress();

        if (ipToHosts.containsKey(ipAddress)) {
            return Iterables.getFirst(ipToHosts.get(ipAddress), null);
        }
        throw new UnknownHostException(ipAddress);
    }
}
