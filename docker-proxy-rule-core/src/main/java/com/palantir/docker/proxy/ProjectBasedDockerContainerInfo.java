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

import com.palantir.docker.compose.configuration.ProjectName;
import com.palantir.docker.compose.execution.DockerExecutable;
import java.util.Optional;
import one.util.streamex.StreamEx;

public final class ProjectBasedDockerContainerInfo implements DockerContainerInfo {
    private final DockerExecutable docker;
    private final ProjectName projectName;
    private final Optional<String> imageNameOverride;

    public ProjectBasedDockerContainerInfo(
            DockerExecutable docker, ProjectName projectName, Optional<String> imageNameOverride) {
        this.docker = docker;
        this.projectName = projectName;
        this.imageNameOverride = imageNameOverride;
    }

    public ProjectBasedDockerContainerInfo(DockerExecutable docker, ProjectName projectName) {
        this(docker, projectName, Optional.empty());
    }

    @Override
    public Optional<String> getIpForHost(String hostname) {
        return StreamEx.of(DockerContainerInfoUtils.getContainerIdsInDockerComposeProject(docker, projectName))
                .mapToEntry(containerId -> DockerContainerInfoUtils.getAllNamesForContainerId(docker, containerId))
                .filterValues(names -> names.contains(hostname))
                .mapToValue((containerId, _names) -> DockerContainerInfoUtils.getContainerIpFromId(docker, containerId))
                .filterValues(Optional::isPresent)
                .mapValues(Optional::get)
                .values()
                .findAny();
    }

    @Override
    public Optional<String> getHostForIp(String ip) {
        return StreamEx.of(DockerContainerInfoUtils.getContainerIdsInDockerComposeProject(docker, projectName))
                .mapToEntry(containerId -> DockerContainerInfoUtils.getContainerIpFromId(docker, containerId))
                .filterValues(Optional::isPresent)
                .mapValues(Optional::get)
                .filterValues(ip::equals)
                .keys()
                .findAny();
    }

    @Override
    public String getNetworkName() {
        return projectName.asString() + "_default";
    }

    @Override
    public Optional<String> getImageNameOverride() {
        return imageNameOverride;
    }
}
