/*
 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
 */
package com.palantir.docker.proxy;

import com.google.common.base.Throwables;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.configuration.ProjectName;
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
    public void canReachDockerContainerByHostnameWithProjectSpecified() {
        DockerProxyRule dockerProxyRule = new DockerProxyRule(
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
    public void canReachDockerContainerByHostnameWithNetworkSpecified() {
        DockerProxyRule dockerProxyRule = new DockerProxyRule(
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

    @Test(expected = IllegalStateException.class)
    public void runningProxyRuleBeforeDockerComposeRuleFails() {
        try {
            new DockerProxyRule(ProjectName.fromString("doesnotexist"), DockerProxyRuleTest.class).before();
        } catch (Throwable e) {
            throw Throwables.propagate(e);
        }
    }
}
