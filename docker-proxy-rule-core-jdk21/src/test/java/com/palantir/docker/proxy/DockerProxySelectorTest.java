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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.net.InetAddresses;
import com.palantir.docker.compose.connection.Cluster;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.ContainerCache;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.ImmutableCluster;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

public class DockerProxySelectorTest {
    private static final String CLUSTER_IP = "172.17.0.1";
    private static final int PROXY_EXTERNAL_PORT = 12345;
    private static final InetSocketAddress PROXY_ADDRESS =
            new InetSocketAddress(InetAddresses.forString(CLUSTER_IP), PROXY_EXTERNAL_PORT);

    private static final String TEST_IP = "172.17.0.5";
    private static final String TEST_HOSTNAME = "some-address";
    private static final URI TEST_IP_URI = createUriUnsafe("http://172.17.0.5");
    private static final URI TEST_HOSTNAME_URI = createUriUnsafe("http://some-address");

    private final DockerContainerInfo containerInfo = mock(DockerContainerInfo.class);
    private final ProxySelector originalProxySelector = mock(ProxySelector.class);
    private final ProxySelector dockerProxySelector =
            new DockerProxySelector(setupProxyContainer(), containerInfo, originalProxySelector);

    @Before
    public void originalProxySelectorIsNoProxy() {
        when(originalProxySelector.select(any())).thenReturn(ImmutableList.of(Proxy.NO_PROXY));
    }

    @Test
    public void nonDockerAddressesShouldDelegateToPassedInSelector() {
        when(containerInfo.getIpForHost(TEST_HOSTNAME)).thenReturn(Optional.empty());
        when(containerInfo.getHostForIp(TEST_HOSTNAME)).thenReturn(Optional.empty());

        List<Proxy> selectedProxy = dockerProxySelector.select(TEST_HOSTNAME_URI);

        assertThat(selectedProxy).containsExactly(Proxy.NO_PROXY);

        verify(originalProxySelector, times(1)).select(TEST_HOSTNAME_URI);
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
        dockerProxySelector.connectFailed(null, PROXY_ADDRESS, new IOException());
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectionFailedShouldThrowOnNullAddress() {
        dockerProxySelector.connectFailed(TEST_HOSTNAME_URI, null, new IOException());
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectionFailedShouldThrowOnNullException() {
        dockerProxySelector.connectFailed(TEST_HOSTNAME_URI, PROXY_ADDRESS, null);
    }

    @Test
    public void connectionFailedShouldNotThrowOnValidArguments() {
        dockerProxySelector.connectFailed(TEST_HOSTNAME_URI, PROXY_ADDRESS, new IOException());
    }

    @Test
    public void connectionFailedShouldDelegateToPassedInSelector() {
        IOException exception = new IOException();
        dockerProxySelector.connectFailed(TEST_HOSTNAME_URI, PROXY_ADDRESS, exception);

        verify(originalProxySelector, times(1)).connectFailed(TEST_HOSTNAME_URI, PROXY_ADDRESS, exception);
    }

    private static Cluster setupProxyContainer() {
        Container proxyContainer = mock(Container.class);
        when(proxyContainer.port(DockerProxySelector.PROXY_CONTAINER_PORT))
                .thenReturn(new DockerPort(CLUSTER_IP, PROXY_EXTERNAL_PORT, DockerProxySelector.PROXY_CONTAINER_PORT));

        ContainerCache containerCache = mock(ContainerCache.class);
        when(containerCache.container(DockerProxySelector.PROXY_CONTAINER_NAME)).thenReturn(proxyContainer);

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
