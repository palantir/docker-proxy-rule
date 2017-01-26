/*
 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.proxy;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.palantir.docker.compose.connection.Cluster;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class DockerProxySelector extends ProxySelector {
    public static final String PROXY_CONTAINER_NAME = "proxy";
    public static final short PROXY_CONTAINER_PORT = 1080;

    private final InetSocketAddress proxyAddress;
    private final Supplier<ProjectInfoMappings> projectInfo;

    public DockerProxySelector(Cluster containers, Supplier<ProjectInfoMappings> projectInfo) {
        this.proxyAddress = InetSocketAddress.createUnresolved(
                containers.ip(),
                containers.container(PROXY_CONTAINER_NAME).port(PROXY_CONTAINER_PORT).getExternalPort());
        this.projectInfo = projectInfo;
    }

    @Override
    public List<Proxy> select(URI uri) {
        ProjectInfoMappings projectInfoMappings = projectInfo.get();
        Set<String> hosts = projectInfoMappings.getHostToIp().keySet();
        Set<String> ips = projectInfoMappings.getIpToHosts().keySet();

        if (hosts.contains(uri.getHost()) || ips.contains(uri.getHost())) {
            return ImmutableList.of(new Proxy(Proxy.Type.SOCKS, proxyAddress));
        } else {
            return ImmutableList.of(Proxy.NO_PROXY);
        }
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        Preconditions.checkArgument(uri != null && sa != null && ioe != null, "Invalid connectFailed call");
    }
}
