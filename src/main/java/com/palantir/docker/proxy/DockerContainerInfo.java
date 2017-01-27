/*
 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.proxy;

import java.util.Optional;

public interface DockerContainerInfo {
    Optional<String> getIpForHost(String hostname);
    Optional<String> getHostForIp(String ip);
}
