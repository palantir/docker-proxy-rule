/*
 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
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
import java.util.function.Supplier;
import org.junit.Test;

public class DockerNameServiceTest {
    private static final String HOST_NAME = "host";
    private static final String HOST_IP = "172.0.2.5";
    private static final InetAddress HOST_IP_INET = InetAddresses.forString("172.0.2.5");

    private final Supplier<ProjectInfoMappings> mappings = mock(Supplier.class);
    private final DockerNameService dockerNameService = new DockerNameService(mappings);

    @Test
    public void shouldReturnIpOfHost() throws UnknownHostException {
        when(mappings.get()).thenReturn(ImmutableProjectInfoMappings.builder()
                .putHostToIp(HOST_NAME, HOST_IP)
                .build());

        InetAddress[] hostAddresses = dockerNameService.lookupAllHostAddr(HOST_NAME);

        assertThat(hostAddresses).containsExactly(HOST_IP_INET);
    }

    @Test
    public void shouldOnlyQueryTheSupplierOncePerLookupCall() throws UnknownHostException {
        when(mappings.get()).thenReturn(ImmutableProjectInfoMappings.builder()
                .putHostToIp(HOST_NAME, HOST_IP)
                .build());

        dockerNameService.lookupAllHostAddr(HOST_NAME);

        verify(mappings, times(1)).get();
    }

    @Test
    public void shouldGetIpOfHostFromSupplierEveryTime() throws UnknownHostException {
        when(mappings.get()).thenReturn(ImmutableProjectInfoMappings.builder()
                .putHostToIp(HOST_NAME, HOST_IP)
                .build());

        dockerNameService.lookupAllHostAddr(HOST_NAME);
        dockerNameService.lookupAllHostAddr(HOST_NAME);

        verify(mappings, times(2)).get();
    }

    @Test(expected = UnknownHostException.class)
    public void shouldThrowUnknownHostExceptionWhenNoIpForHost() throws UnknownHostException {
        when(mappings.get()).thenReturn(ImmutableProjectInfoMappings.builder()
                .build());

        dockerNameService.lookupAllHostAddr(HOST_NAME);
    }

    @Test
    public void shouldGetHostFromIp() throws UnknownHostException {
        when(mappings.get()).thenReturn(ImmutableProjectInfoMappings.builder()
                .putIpToHosts(HOST_IP, HOST_NAME)
                .build());

        String host = dockerNameService.getHostByAddr(HOST_IP_INET.getAddress());

        assertThat(host).isEqualTo(HOST_NAME);
    }

    @Test
    public void shouldOnlyQueryTheSupplierOncePerHostByAddrCall() throws UnknownHostException {
        when(mappings.get()).thenReturn(ImmutableProjectInfoMappings.builder()
                .putIpToHosts(HOST_IP, HOST_NAME)
                .build());

        dockerNameService.getHostByAddr(HOST_IP_INET.getAddress());

        verify(mappings, times(1)).get();
    }

    @Test
    public void shouldGetHostOfIpFromSupplierEveryTime() throws UnknownHostException {
        when(mappings.get()).thenReturn(ImmutableProjectInfoMappings.builder()
                .putIpToHosts(HOST_IP, HOST_NAME)
                .build());

        dockerNameService.getHostByAddr(HOST_IP_INET.getAddress());
        dockerNameService.getHostByAddr(HOST_IP_INET.getAddress());

        verify(mappings, times(2)).get();
    }

    @Test(expected = UnknownHostException.class)
    public void shouldThrowUnknownHostExceptionWhenNoHostForIp() throws UnknownHostException {
        when(mappings.get()).thenReturn(ImmutableProjectInfoMappings.builder()
                .build());

        dockerNameService.getHostByAddr(HOST_IP_INET.getAddress());
    }
}
