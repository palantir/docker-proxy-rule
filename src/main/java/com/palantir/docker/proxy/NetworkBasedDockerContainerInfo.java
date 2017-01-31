/*
 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.proxy;

import com.palantir.docker.compose.execution.DockerExecutable;
import java.util.Optional;
import one.util.streamex.StreamEx;

public class NetworkBasedDockerContainerInfo implements DockerContainerInfo {
    private final DockerExecutable docker;
    private final String networkName;

    public NetworkBasedDockerContainerInfo(DockerExecutable docker, String networkName) {
        this.docker = docker;
        this.networkName = networkName;
    }

    @Override
    public Optional<String> getIpForHost(String hostname) {
        return StreamEx.of(DockerContainerInfoUtils.getContainerIdsOnNetwork(docker, networkName))
                .mapToEntry(containerId -> DockerContainerInfoUtils.getAllNamesForContainerId(docker, containerId))
                .filterValues(names -> names.contains(hostname))
                .mapToValue((containerId, names) -> DockerContainerInfoUtils.getContainerIpFromId(docker, containerId))
                .values()
                .findAny();
    }

    @Override
    public Optional<String> getHostForIp(String ip) {
        return StreamEx.of(DockerContainerInfoUtils.getContainerIdsOnNetwork(docker, networkName))
                .mapToEntry(containerId -> DockerContainerInfoUtils.getContainerIpFromId(docker, containerId))
                .filterValues(ip::equals)
                .keys()
                .findAny();
    }

    @Override
    public String getNetworkName() {
        return networkName;
    }
}
