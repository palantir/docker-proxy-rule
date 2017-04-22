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
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.execution.DockerExecutable;
import com.palantir.docker.compose.execution.DockerExecutionException;
import com.palantir.docker.compose.logging.LogDirectory;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ProxySelector;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import net.amygdalum.xrayinterface.XRayInterface;
import org.junit.rules.ExternalResource;

public class DockerProxyRule extends ExternalResource {
    private final DockerContainerInfo dockerContainerInfo;
    private final DockerComposeRule dockerComposeRule;

    private ProxySelector originalProxySelector;

    /**
     * Creates a {@link DockerProxyRule} which will create a proxy and DNS so that
     * tests can interface with docker containers directly.
     *
     * @param dockerContainerInfoCreator A {@link Function} that creates the DockerContainerInfo to use
     * @param classToLogFor The class using {@link DockerProxyRule}
     */
    public DockerProxyRule(
            Function<DockerExecutable, DockerContainerInfo> dockerContainerInfoCreator,
            Class<?> classToLogFor) {
        this.dockerContainerInfo = dockerContainerInfoCreator.apply(DockerExecutable.builder()
                .dockerConfiguration(DockerMachine.localMachine().build())
                .build());
        String logDirectory = DockerProxyRule.class.getSimpleName() + "-" + classToLogFor.getSimpleName();
        this.dockerComposeRule = DockerComposeRule.builder()
                .file(getDockerComposeFile(this.dockerContainerInfo.getNetworkName()).getPath())
                .waitingForService("proxy", Container::areAllPortsOpen)
                .saveLogsTo(LogDirectory.circleAwareLogDirectory(logDirectory))
                .shutdownStrategy(ShutdownStrategy.AGGRESSIVE_WITH_NETWORK_CLEANUP)
                .retryAttempts(0)
                .build();
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
    public void before() throws IOException, InterruptedException {
        try {
            originalProxySelector = ProxySelector.getDefault();
            dockerComposeRule.before();
            getNameServices().add(0, new DockerNameService(dockerContainerInfo));
            ProxySelector.setDefault(new DockerProxySelector(dockerComposeRule.containers(), dockerContainerInfo));
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

    private static File getDockerComposeFile(String networkName) {
        try {
            File proxyFile = File.createTempFile("proxy", ".yml");
            String proxyConfig = Resources.toString(
                    Resources.getResource("docker-compose.proxy.yml"),
                    StandardCharsets.UTF_8);
            Files.write(
                    proxyConfig.replace("{{NETWORK_NAME}}", networkName),
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
