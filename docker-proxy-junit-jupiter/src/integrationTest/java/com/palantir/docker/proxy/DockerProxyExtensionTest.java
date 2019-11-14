/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.docker.proxy;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.palantir.docker.compose.DockerComposeExtension;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.logging.LogDirectory;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class DockerProxyExtensionTest {
    @RegisterExtension
    static final DockerComposeExtension DOCKER_COMPOSE_EXTENSION = DockerComposeExtension.builder()
            .file("src/integrationTest/resources/DockerProxyExtensionTest-services.yml")
            .saveLogsTo(LogDirectory.circleAwareLogDirectory(DockerProxyExtensionTest.class))
            .waitingForService("webserver", Container::areAllPortsOpen)
            .build();

    @Test
    void canReachDockerContainerByContainerNameWithProjectSpecified() throws IOException, InterruptedException {
        DockerProxyExtension dockerProxyExtension = DockerProxyExtension.fromProjectName(
                DOCKER_COMPOSE_EXTENSION.projectName(),
                DockerProxyExtensionTest.class);
        try {
            dockerProxyExtension.before();
            URLConnection urlConnection = new URL("http://webserver").openConnection();
            urlConnection.connect();
        } finally {
            dockerProxyExtension.after();
        }
    }

    @Test
    void canReachDockerContainerByHostnameWithProjectSpecified() throws IOException, InterruptedException {
        DockerProxyExtension dockerProxyExtension = DockerProxyExtension.fromProjectName(
                DOCKER_COMPOSE_EXTENSION.projectName(),
                DockerProxyExtensionTest.class);
        try {
            dockerProxyExtension.before();
            URLConnection urlConnection = new URL("http://web").openConnection();
            urlConnection.connect();
        } finally {
            dockerProxyExtension.after();
        }
    }

    @Test
    void canReachDockerContainerByHostnameAndDomainNameWithProjectSpecified()
            throws IOException, InterruptedException {
        DockerProxyExtension dockerProxyExtension = DockerProxyExtension.fromProjectName(
                DOCKER_COMPOSE_EXTENSION.projectName(),
                DockerProxyExtensionTest.class);
        try {
            dockerProxyExtension.before();
            URLConnection urlConnection = new URL("http://web.server.here").openConnection();
            urlConnection.connect();
        } finally {
            dockerProxyExtension.after();
        }
    }

    @Test
    void canReachDockerContainerByContainerNameWithNetworkSpecified() throws IOException, InterruptedException {
        DockerProxyExtension dockerProxyExtension = DockerProxyExtension.fromNetworkName(
                DOCKER_COMPOSE_EXTENSION.projectName().asString() + "_default",
                DockerProxyExtensionTest.class);
        try {
            dockerProxyExtension.before();
            URLConnection urlConnection = new URL("http://webserver").openConnection();
            urlConnection.connect();
        } finally {
            dockerProxyExtension.after();
        }
    }

    @Test
    void canReachDockerContainerByHostnameWithNetworkSpecified() throws IOException, InterruptedException {
        DockerProxyExtension dockerProxyExtension = DockerProxyExtension.fromNetworkName(
                DOCKER_COMPOSE_EXTENSION.projectName().asString() + "_default",
                DockerProxyExtensionTest.class);
        try {
            dockerProxyExtension.before();
            URLConnection urlConnection = new URL("http://web").openConnection();
            urlConnection.connect();
        } finally {
            dockerProxyExtension.after();
        }
    }

    @Test
    void canReachDockerContainerByHostnameAndDomainNameWithNetworkSpecified()
            throws IOException, InterruptedException {
        DockerProxyExtension dockerProxyExtension = DockerProxyExtension.fromNetworkName(
                DOCKER_COMPOSE_EXTENSION.projectName().asString() + "_default",
                DockerProxyExtensionTest.class);
        try {
            dockerProxyExtension.before();
            URLConnection urlConnection = new URL("http://web.server.here").openConnection();
            urlConnection.connect();
        } finally {
            dockerProxyExtension.after();
        }
    }

    @Test
    void otherHostnamesStillResolve() throws IOException, InterruptedException {
        DockerProxyExtension dockerProxyExtension = DockerProxyExtension.fromProjectName(
                DOCKER_COMPOSE_EXTENSION.projectName(),
                DockerProxyExtensionTest.class);
        try {
            dockerProxyExtension.before();
            URLConnection urlConnection = new URL("http://www.palantir.com").openConnection();
            urlConnection.connect();
        } finally {
            dockerProxyExtension.after();
        }
    }

    @Test
    void runningProxyRuleBeforeDockerComposeRuleFails() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> DockerProxyExtension.fromNetworkName("doesnotexist", DockerProxyExtensionTest.class)
                        .before());
    }
}
