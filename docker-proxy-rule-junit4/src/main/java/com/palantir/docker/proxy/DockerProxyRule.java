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

import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.configuration.ProjectName;
import com.palantir.docker.compose.execution.DockerExecutable;
import java.util.function.Function;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public final class DockerProxyRule extends DockerProxyManager<DockerComposeRule.Builder> implements TestRule {
    /**
     * Creates a {@link DockerProxyRule} which will create a proxy and DNS so that
     * tests can interface with docker containers directly.
     *
     * @param dockerContainerInfoCreator A {@link Function} that creates the DockerContainerInfo to use
     * @param classToLogFor The class using {@link DockerProxyRule}
     */
    DockerProxyRule(
            Function<DockerExecutable, DockerContainerInfo> dockerContainerInfoCreator,
            Class<?> classToLogFor) {
        super(customizer -> customizer.apply(DockerComposeRule.builder().retryAttempts(0)).build(),
                dockerContainerInfoCreator,
                classToLogFor);
    }

    /**
     * Creates a {@link DockerProxyRule} using a {@link ProjectBasedDockerContainerInfo}.
     *
     * @param projectName The docker-compose-rule ProjectName to use to find the containers
     * @param classToLogFor The class using {@link DockerProxyRule}
     */
    public static DockerProxyRule fromProjectName(ProjectName projectName, Class<?> classToLogFor) {
        return new DockerProxyRule(docker -> new ProjectBasedDockerContainerInfo(docker, projectName), classToLogFor);
    }

    /**
     * Creates a {@link DockerProxyRule} using a {@link NetworkBasedDockerContainerInfo}.
     *
     * @param networkName The network name to use to find the containers
     * @param classToLogFor The class using {@link DockerProxyRule}
     */
    public static DockerProxyRule fromNetworkName(String networkName, Class<?> classToLogFor) {
        return new DockerProxyRule(docker -> new NetworkBasedDockerContainerInfo(docker, networkName), classToLogFor);
    }

    @Override
    public Statement apply(Statement base, Description _description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    before();
                    base.evaluate();
                } finally {
                    after();
                }
            }
        };
    }
}
