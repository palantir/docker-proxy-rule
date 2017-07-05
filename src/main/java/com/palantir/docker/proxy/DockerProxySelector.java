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
import java.util.Optional;

public class DockerProxySelector extends ProxySelector {
    public static final String PROXY_CONTAINER_NAME = "proxy";
    public static final short PROXY_CONTAINER_PORT = 1080;

    private final InetSocketAddress proxyAddress;
    private final DockerContainerInfo containerInfo;
    private final ProxySelector delegate;

    public DockerProxySelector(Cluster containers, DockerContainerInfo containerInfo, ProxySelector delegate) {
        this.proxyAddress = InetSocketAddress.createUnresolved(
                containers.ip(),
                containers.container(PROXY_CONTAINER_NAME).port(PROXY_CONTAINER_PORT).getExternalPort());
        this.containerInfo = containerInfo;
        this.delegate = delegate;
    }

    @Override
    public List<Proxy> select(URI uri) {
        Optional<String> containerIpForUriHost = containerInfo.getIpForHost(uri.getHost());
        Optional<String> containerHostForUriHost = containerInfo.getHostForIp(uri.getHost());
        if (containerIpForUriHost.isPresent() || containerHostForUriHost.isPresent()) {
            return ImmutableList.of(new Proxy(Proxy.Type.SOCKS, proxyAddress));
        } else {
            return delegate.select(uri);
        }
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        Preconditions.checkArgument(uri != null && sa != null && ioe != null, "Invalid connectFailed call");
        delegate.connectFailed(uri, sa, ioe);
    }
}
