/*
 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
 */
package com.palantir.docker.proxy;

import com.google.common.base.Throwables;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.configuration.ShutdownStrategy;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.logging.LogDirectory;
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
            .shutdownStrategy(ShutdownStrategy.AGGRESSIVE_WITH_NETWORK_CLEANUP)
            .build();

    @Test
    public void canReachDockerContainerByContainerNameWithProjectSpecified() {
        DockerProxyRule dockerProxyRule = DockerProxyRule.fromProjectName(
                DOCKER_COMPOSE_RULE.projectName(),
                DockerProxyRuleTest.class);
        try {
            dockerProxyRule.before();
            URLConnection urlConnection = new URL("http://webserver").openConnection();
            urlConnection.connect();
        } catch (Throwable e) {
            throw Throwables.propagate(e);
        } finally {
            dockerProxyRule.after();
        }
    }

    @Test
    public void canReachDockerContainerByHostnameWithProjectSpecified() {
        DockerProxyRule dockerProxyRule = DockerProxyRule.fromProjectName(
                DOCKER_COMPOSE_RULE.projectName(),
                DockerProxyRuleTest.class);
        try {
            dockerProxyRule.before();
            URLConnection urlConnection = new URL("http://web").openConnection();
            urlConnection.connect();
        } catch (Throwable e) {
            throw Throwables.propagate(e);
        } finally {
            dockerProxyRule.after();
        }
    }

    @Test
    public void canReachDockerContainerByHostnameAndDomainNameWithProjectSpecified() {
        DockerProxyRule dockerProxyRule = DockerProxyRule.fromProjectName(
                DOCKER_COMPOSE_RULE.projectName(),
                DockerProxyRuleTest.class);
        try {
            dockerProxyRule.before();
            URLConnection urlConnection = new URL("http://web.server.here").openConnection();
            urlConnection.connect();
        } catch (Throwable e) {
            throw Throwables.propagate(e);
        } finally {
            dockerProxyRule.after();
        }
    }

    @Test
    public void canReachDockerContainerByContainerNameWithNetworkSpecified() {
        DockerProxyRule dockerProxyRule = DockerProxyRule.fromNetworkName(
                DOCKER_COMPOSE_RULE.projectName().asString() + "_default",
                DockerProxyRuleTest.class);
        try {
            dockerProxyRule.before();
            URLConnection urlConnection = new URL("http://webserver").openConnection();
            urlConnection.connect();
        } catch (Throwable e) {
            throw Throwables.propagate(e);
        } finally {
            dockerProxyRule.after();
        }
    }

    @Test
    public void canReachDockerContainerByHostnameWithNetworkSpecified() {
        DockerProxyRule dockerProxyRule = DockerProxyRule.fromNetworkName(
                DOCKER_COMPOSE_RULE.projectName().asString() + "_default",
                DockerProxyRuleTest.class);
        try {
            dockerProxyRule.before();
            URLConnection urlConnection = new URL("http://web").openConnection();
            urlConnection.connect();
        } catch (Throwable e) {
            throw Throwables.propagate(e);
        } finally {
            dockerProxyRule.after();
        }
    }

    @Test
    public void canReachDockerContainerByHostnameAndDomainNameWithNetworkSpecified() {
        DockerProxyRule dockerProxyRule = DockerProxyRule.fromNetworkName(
                DOCKER_COMPOSE_RULE.projectName().asString() + "_default",
                DockerProxyRuleTest.class);
        try {
            dockerProxyRule.before();
            URLConnection urlConnection = new URL("http://web.server.here").openConnection();
            urlConnection.connect();
        } catch (Throwable e) {
            throw Throwables.propagate(e);
        } finally {
            dockerProxyRule.after();
        }
    }


    @Test(expected = IllegalStateException.class)
    public void runningProxyRuleBeforeDockerComposeRuleFails() {
        try {
            DockerProxyRule.fromNetworkName("doesnotexist", DockerProxyRuleTest.class).before();
        } catch (Throwable e) {
            throw Throwables.propagate(e);
        }
    }
}
