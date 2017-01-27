/*
 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
 */
package com.palantir.docker.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Throwables;
import com.palantir.docker.compose.connection.Cluster;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.ContainerCache;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.ImmutableCluster;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public class DockerProxySelectorTest {
    private static final String CLUSTER_IP = "172.17.0.1";
    private static final int PROXY_EXTERNAL_PORT = 12345;
    private static final InetSocketAddress PROXY_ADDRESS = InetSocketAddress
            .createUnresolved(CLUSTER_IP, PROXY_EXTERNAL_PORT);

    private static final String TEST_IP = "172.17.0.5";
    private static final String TEST_HOSTNAME = "some-address";
    private static final URI TEST_IP_URI = createUriUnsafe("http://172.17.0.5");
    private static final URI TEST_HOSTNAME_URI = createUriUnsafe("http://some-address");

    private final DockerContainerInfo containerInfo = mock(DockerContainerInfo.class);
    private final DockerProxySelector dockerProxySelector = new DockerProxySelector(setupProxyContainer(),
            containerInfo);

    @Test
    public void nonDockerAddressesShouldNotGoThroughAProxy() {
        when(containerInfo.getIpForHost(TEST_HOSTNAME)).thenReturn(Optional.empty());
        when(containerInfo.getHostForIp(TEST_HOSTNAME)).thenReturn(Optional.empty());

        List<Proxy> selectedProxy = dockerProxySelector.select(TEST_HOSTNAME_URI);

        assertThat(selectedProxy).containsExactly(Proxy.NO_PROXY);
    }

    @Test
    public void dockerAddressesShouldGoThroughAProxy() throws URISyntaxException {
        when(containerInfo.getIpForHost(TEST_HOSTNAME)).thenReturn(Optional.of(TEST_IP));

        List<Proxy> selectedProxy = dockerProxySelector.select(TEST_HOSTNAME_URI);

        assertThat(selectedProxy).containsExactly(new Proxy(Proxy.Type.SOCKS, PROXY_ADDRESS));
    }

    @Test
    public void dockerIpsShouldGoThroughAProxy() {
        when(containerInfo.getHostForIp(TEST_IP)).thenReturn(Optional.of(TEST_HOSTNAME));

        List<Proxy> selectedProxy = dockerProxySelector.select(TEST_IP_URI);

        assertThat(selectedProxy).containsExactly(new Proxy(Proxy.Type.SOCKS, PROXY_ADDRESS));
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectionFailedShouldThrowOnNullUri() {
        dockerProxySelector.connectFailed(
                null,
                PROXY_ADDRESS,
                new IOException());
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectionFailedShouldThrowOnNullAddress() {
        dockerProxySelector.connectFailed(
                TEST_HOSTNAME_URI,
                null,
                new IOException());
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectionFailedShouldThrowOnNullException() {
        dockerProxySelector.connectFailed(
                TEST_HOSTNAME_URI,
                PROXY_ADDRESS,
                null);
    }

    @Test
    public void connectionFailedShouldNotThrowOnValidArguments() {
        dockerProxySelector.connectFailed(
                TEST_HOSTNAME_URI,
                PROXY_ADDRESS,
                new IOException());
    }

    private static Cluster setupProxyContainer() {
        Container proxyContainer = mock(Container.class);
        when(proxyContainer.port(DockerProxySelector.PROXY_CONTAINER_PORT))
                .thenReturn(new DockerPort(CLUSTER_IP, PROXY_EXTERNAL_PORT, DockerProxySelector.PROXY_CONTAINER_PORT));

        ContainerCache containerCache = mock(ContainerCache.class);
        when(containerCache.container(DockerProxySelector.PROXY_CONTAINER_NAME))
                .thenReturn(proxyContainer);

        return ImmutableCluster.builder()
                .ip(CLUSTER_IP)
                .containerCache(containerCache)
                .build();
    }

    private static URI createUriUnsafe(String uriString) {
        try {
            return new URI(uriString);
        } catch (URISyntaxException e) {
            throw Throwables.propagate(e);
        }
    }
}
