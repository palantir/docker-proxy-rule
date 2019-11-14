/*
 * (c) Copyright 2017 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.docker.proxy;

import com.google.common.collect.ImmutableList;
import com.palantir.docker.compose.connection.Cluster;
import com.palantir.logsafe.Preconditions;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;

public final class DockerProxySelector extends ProxySelector {
    public static final String PROXY_CONTAINER_NAME = "proxy";
    public static final short PROXY_CONTAINER_PORT = 1080;

    private final InetSocketAddress proxyAddress;
    private final DockerContainerInfo containerInfo;
    private final ProxySelector delegate;

    public DockerProxySelector(Cluster containers, DockerContainerInfo containerInfo, ProxySelector delegate) {
        // We can't call InetSocketAddress.createUnresolved here as some downstream libraries cannot deal with
        // getAddress returning null.
        this.proxyAddress = new InetSocketAddress(
                containers.ip(),
                containers.container(PROXY_CONTAINER_NAME).port(PROXY_CONTAINER_PORT).getExternalPort());
        this.containerInfo = containerInfo;
        this.delegate = delegate;
    }

    @Override
    public List<Proxy> select(URI uri) {
        String host = uri.getHost();
        if (containerInfo.getIpForHost(host).isPresent() || containerInfo.getHostForIp(host).isPresent()) {
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
