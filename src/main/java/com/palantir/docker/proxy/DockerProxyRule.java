/*
 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.proxy;

import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.configuration.ProjectName;
import com.palantir.docker.compose.configuration.ShutdownStrategy;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.execution.DockerExecutionException;
import com.palantir.docker.compose.logging.LogDirectory;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ProxySelector;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.amygdalum.xrayinterface.XRayInterface;
import org.junit.rules.ExternalResource;

public class DockerProxyRule extends ExternalResource {
    private final DockerComposeRule dockerComposeRule;
    private final ProjectName projectName;

    private ProxySelector originalProxySelector;

    public DockerProxyRule(ProjectName projectName, Class<?> classToLogFor) {
        String logDirectory = DockerProxyRule.class.getSimpleName() + "-" + classToLogFor.getSimpleName();
        this.dockerComposeRule = DockerComposeRule.builder()
                .file(getDockerComposeFile(projectName).getPath())
                .waitingForService("proxy", Container::areAllPortsOpen)
                .saveLogsTo(LogDirectory.circleAwareLogDirectory(logDirectory))
                .shutdownStrategy(ShutdownStrategy.AGGRESSIVE_WITH_NETWORK_CLEANUP)
                .build();
        this.projectName = projectName;
    }

    @Override
    public void before() throws Throwable {
        try {
            originalProxySelector = ProxySelector.getDefault();
            dockerComposeRule.before();
            ProjectInfo projectInfo = new ProjectInfo(dockerComposeRule.dockerExecutable(), projectName);
            getNameServices().add(0, new DockerNameService(projectInfo));
            ProxySelector.setDefault(new DockerProxySelector(dockerComposeRule.containers(), projectInfo));
        } catch (DockerExecutionException e) {
            if (e.getMessage().contains("declared as external")) {
                throw new IllegalStateException(
                        "DockerComposeRule must run before DockerProxyRule. Please use a RuleChain.", e);
            } else {
                throw e;
            }
        }
    }

    @Override
    public void after() {
        ProxySelector.setDefault(originalProxySelector);
        getNameServices().remove(0);
        dockerComposeRule.after();
    }

    private static File getDockerComposeFile(ProjectName projectName) {
        try {
            File proxyFile = File.createTempFile("proxy", ".yml");
            String proxyConfig = Resources.toString(
                    Resources.getResource("docker-compose.proxy.yml"),
                    StandardCharsets.UTF_8);
            Files.write(
                    proxyConfig.replace("{{PROJECT_NAME}}", projectName.asString()),
                    proxyFile,
                    StandardCharsets.UTF_8);
            return proxyFile;
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private static List<sun.net.spi.nameservice.NameService> getNameServices() {
        return XRayInterface
                .xray(InetAddress.class)
                .to(OpenInetAddress.class)
                .getNameServices();
    }

    private interface OpenInetAddress {
        List<sun.net.spi.nameservice.NameService> getNameServices();
    }
}
