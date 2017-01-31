/*
 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.proxy;

import java.util.Optional;

public interface DockerContainerInfo {
    /**
     * Converts a docker IP to a docker hostname if it exists.
     *
     * @param hostname The docker hostname to lookup
     * @return The docker IP for a docker hostname if it exists
     */
    Optional<String> getIpForHost(String hostname);

    /**
     * Converts a docker hostname to a docker IP if it exists.
     *
     * @param ip The docker ip to lookup
     * @return The docker hostname for a docker IP address if it exists
     */
    Optional<String> getHostForIp(String ip);

    /**
     * Returns the network name the proxy will connect to.
     *
     * @return The network for the proxy to connect to
     */
    String getNetworkName();
}
