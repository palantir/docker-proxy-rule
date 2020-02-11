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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import org.junit.Test;

public class DockerNameServiceTest {
    private static final String HOST_NAME = "host";
    private static final String HOST_IP = "172.0.2.5";
    private static final InetAddress HOST_IP_INET = InetAddresses.forString("172.0.2.5");

    private final DockerContainerInfo containerInfo = mock(DockerContainerInfo.class);
    private final DockerNameService dockerNameService = new DockerNameService(containerInfo);

    @Test
    public void shouldReturnIpOfHost() throws UnknownHostException {
        when(containerInfo.getIpForHost(HOST_NAME)).thenReturn(Optional.of(HOST_IP));

        InetAddress[] hostAddresses = dockerNameService.lookupAllHostAddr(HOST_NAME);

        assertThat(hostAddresses).containsExactly(HOST_IP_INET);
    }

    @Test
    public void shouldOnlyQueryTheSupplierOncePerLookupCall() throws UnknownHostException {
        when(containerInfo.getIpForHost(HOST_NAME)).thenReturn(Optional.of(HOST_IP));

        dockerNameService.lookupAllHostAddr(HOST_NAME);

        verify(containerInfo, times(1)).getIpForHost(HOST_NAME);
    }

    @Test
    public void shouldGetIpOfHostFromSupplierEveryTime() throws UnknownHostException {
        when(containerInfo.getIpForHost(HOST_NAME)).thenReturn(Optional.of(HOST_IP));

        dockerNameService.lookupAllHostAddr(HOST_NAME);
        dockerNameService.lookupAllHostAddr(HOST_NAME);

        verify(containerInfo, times(2)).getIpForHost(HOST_NAME);
    }

    @Test(expected = UnknownHostException.class)
    public void shouldThrowUnknownHostExceptionWhenNoIpForHost() throws UnknownHostException {
        when(containerInfo.getIpForHost(HOST_NAME)).thenReturn(Optional.empty());

        dockerNameService.lookupAllHostAddr(HOST_NAME);
    }

    @Test
    public void shouldGetHostFromIp() throws UnknownHostException {
        when(containerInfo.getHostForIp(HOST_IP)).thenReturn(Optional.of(HOST_NAME));

        String host = dockerNameService.getHostByAddr(HOST_IP_INET.getAddress());

        assertThat(host).isEqualTo(HOST_NAME);
    }

    @Test
    public void shouldOnlyQueryTheSupplierOncePerHostByAddrCall() throws UnknownHostException {
        when(containerInfo.getHostForIp(HOST_IP)).thenReturn(Optional.of(HOST_NAME));

        dockerNameService.getHostByAddr(HOST_IP_INET.getAddress());

        verify(containerInfo, times(1)).getHostForIp(HOST_IP);
    }

    @Test
    public void shouldGetHostOfIpFromSupplierEveryTime() throws UnknownHostException {
        when(containerInfo.getHostForIp(HOST_IP)).thenReturn(Optional.of(HOST_NAME));

        dockerNameService.getHostByAddr(HOST_IP_INET.getAddress());
        dockerNameService.getHostByAddr(HOST_IP_INET.getAddress());

        verify(containerInfo, times(2)).getHostForIp(HOST_IP);
    }

    @Test(expected = UnknownHostException.class)
    public void shouldThrowUnknownHostExceptionWhenNoHostForIp() throws UnknownHostException {
        when(containerInfo.getHostForIp(HOST_IP)).thenReturn(Optional.empty());

        dockerNameService.getHostByAddr(HOST_IP_INET.getAddress());
    }
}
