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

    /**
     * Returns an override for the image name to use for the docker container,
     * otherwise `vimagick/dante:latest` will get used.
     */
    Optional<String> getImageNameOverride();
}
