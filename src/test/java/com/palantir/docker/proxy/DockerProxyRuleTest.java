/*
 * (c) Copyright 2017 Palantir Technologies Inc. All rights reserved.
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
            .file("src/test/resources/DockerProxyRuleTest-services.yml")
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


    @Test(expected = IllegalStateException.class)
    public void runningProxyRuleBeforeDockerComposeRuleFails() throws IOException, InterruptedException {
        DockerProxyRule.fromNetworkName("doesnotexist", DockerProxyRuleTest.class).before();
    }
}
