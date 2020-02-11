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
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.logging.LogDirectory;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import org.junit.ClassRule;
import org.junit.Test;

public class DockerProxyRuleTest {
    @ClassRule
    public static final DockerComposeRule DOCKER_COMPOSE_RULE = DockerComposeRule.builder()
            .file("src/integrationTest/resources/DockerProxyRuleTest-services.yml")
            .saveLogsTo(LogDirectory.circleAwareLogDirectory(DockerProxyRuleTest.class))
            .waitingForService("webserver", Container::areAllPortsOpen)
            .build();

    @Test
    public void canReachDockerContainerByContainerNameWithProjectSpecified() throws IOException, InterruptedException {
        DockerProxyRule dockerProxyRule = DockerProxyRule.fromProjectName(
                DOCKER_COMPOSE_RULE.projectName(),
                DockerProxyRuleTest.class);
        try {
            dockerProxyRule.before();
            URLConnection urlConnection = new URL("http://webserver").openConnection();
            urlConnection.connect();
        } finally {
            dockerProxyRule.after();
        }
    }

    @Test
    public void canReachDockerContainerByHostnameWithProjectSpecified() throws IOException, InterruptedException {
        DockerProxyRule dockerProxyRule = DockerProxyRule.fromProjectName(
                DOCKER_COMPOSE_RULE.projectName(),
                DockerProxyRuleTest.class);
        try {
            dockerProxyRule.before();
            URLConnection urlConnection = new URL("http://web").openConnection();
            urlConnection.connect();
        } finally {
            dockerProxyRule.after();
        }
    }

    @Test
    public void canReachDockerContainerByHostnameAndDomainNameWithProjectSpecified()
            throws IOException, InterruptedException {
        DockerProxyRule dockerProxyRule = DockerProxyRule.fromProjectName(
                DOCKER_COMPOSE_RULE.projectName(),
                DockerProxyRuleTest.class);
        try {
            dockerProxyRule.before();
            URLConnection urlConnection = new URL("http://web.server.here").openConnection();
            urlConnection.connect();
        } finally {
            dockerProxyRule.after();
        }
    }

    @Test
    public void canReachDockerContainerByContainerNameWithNetworkSpecified() throws IOException, InterruptedException {
        DockerProxyRule dockerProxyRule = DockerProxyRule.fromNetworkName(
                DOCKER_COMPOSE_RULE.projectName().asString() + "_default",
                DockerProxyRuleTest.class);
        try {
            dockerProxyRule.before();
            URLConnection urlConnection = new URL("http://webserver").openConnection();
            urlConnection.connect();
        } finally {
            dockerProxyRule.after();
        }
    }

    @Test
    public void canReachDockerContainerByHostnameWithNetworkSpecified() throws IOException, InterruptedException {
        DockerProxyRule dockerProxyRule = DockerProxyRule.fromNetworkName(
                DOCKER_COMPOSE_RULE.projectName().asString() + "_default",
                DockerProxyRuleTest.class);
        try {
            dockerProxyRule.before();
            URLConnection urlConnection = new URL("http://web").openConnection();
            urlConnection.connect();
        } finally {
            dockerProxyRule.after();
        }
    }

    @Test
    public void canReachDockerContainerByHostnameAndDomainNameWithNetworkSpecified()
            throws IOException, InterruptedException {
        DockerProxyRule dockerProxyRule = DockerProxyRule.fromNetworkName(
                DOCKER_COMPOSE_RULE.projectName().asString() + "_default",
                DockerProxyRuleTest.class);
        try {
            dockerProxyRule.before();
            URLConnection urlConnection = new URL("http://web.server.here").openConnection();
            urlConnection.connect();
        } finally {
            dockerProxyRule.after();
        }
    }

    @Test
    public void otherHostnamesStillResolve() throws IOException, InterruptedException {
        DockerProxyRule dockerProxyRule = DockerProxyRule.fromProjectName(
                DOCKER_COMPOSE_RULE.projectName(),
                DockerProxyRuleTest.class);
        try {
            dockerProxyRule.before();
            URLConnection urlConnection = new URL("http://www.palantir.com").openConnection();
            urlConnection.connect();
        } finally {
            dockerProxyRule.after();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void runningProxyRuleBeforeDockerComposeRuleFails() throws IOException, InterruptedException {
        DockerProxyRule.fromNetworkName("doesnotexist", DockerProxyRuleTest.class).before();
    }
}
