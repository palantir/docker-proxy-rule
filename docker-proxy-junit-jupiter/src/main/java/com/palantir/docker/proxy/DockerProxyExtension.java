/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
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

import com.palantir.docker.compose.DockerComposeExtension;
import com.palantir.docker.compose.configuration.ProjectName;
import com.palantir.docker.compose.execution.DockerExecutable;
import java.io.IOException;
import java.util.function.Function;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class DockerProxyExtension extends DockerProxyManager<DockerComposeExtension.Builder>
        implements BeforeAllCallback, AfterAllCallback {
    /**
     * Creates a {@link DockerProxyExtension} which will create a proxy and DNS so that tests can interface with docker
     * containers directly.
     *
     * @param dockerContainerInfoCreator A {@link Function} that creates the DockerContainerInfo to use
     * @param classToLogFor              The class using {@link DockerProxyExtension}
     */
    DockerProxyExtension(
            Function<DockerExecutable, DockerContainerInfo> dockerContainerInfoCreator,
            Class<?> classToLogFor) {
        super(customizer -> customizer.apply(DockerComposeExtension.builder()).build(),
                dockerContainerInfoCreator,
                classToLogFor);
    }

    /**
     * Creates a {@link DockerProxyExtension} using a {@link ProjectBasedDockerContainerInfo}.
     *
     * @param projectName The docker-compose-rule ProjectName to use to find the containers
     * @param classToLogFor The class using {@link DockerProxyExtension}
     */
    public static DockerProxyExtension fromProjectName(ProjectName projectName, Class<?> classToLogFor) {
        return new DockerProxyExtension(docker ->
                new ProjectBasedDockerContainerInfo(docker, projectName), classToLogFor);
    }

    /**
     * Creates a {@link DockerProxyExtension} using a {@link NetworkBasedDockerContainerInfo}.
     *
     * @param networkName The network name to use to find the containers
     * @param classToLogFor The class using {@link DockerProxyExtension}
     */
    public static DockerProxyExtension fromNetworkName(String networkName, Class<?> classToLogFor) {
        return new DockerProxyExtension(docker ->
                new NetworkBasedDockerContainerInfo(docker, networkName), classToLogFor);
    }

    @Override
    public void beforeAll(ExtensionContext _context) throws IOException, InterruptedException {
        before();
    }

    @Override
    public void afterAll(ExtensionContext _context) {
        after();
    }
}
