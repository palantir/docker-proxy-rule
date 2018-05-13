/*
 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.proxy;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.io.CharStreams;
import com.google.common.net.InetAddresses;
import com.palantir.docker.compose.configuration.ProjectName;
import com.palantir.docker.compose.execution.DockerExecutable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import one.util.streamex.StreamEx;

public class DockerContainerInfoUtils {
    private static final List<String> DOCKER_NAME_TAGS = ImmutableList.of(
            "{{ .Name }}",
            "{{ .Config.Hostname }}",
            "{{ .Config.Hostname }}.{{ .Config.Domainname }}");
    private static final List<String> DOCKER_NAME_LABELS = ImmutableList.of(
            "com.docker.compose.service",
            "hostname");

    private DockerContainerInfoUtils() {
        // Utility class
    }

    public static List<String> getAllNamesForContainerId(DockerExecutable docker, String containerId) {
        try {
            String labelsFormat = StreamEx.of(DOCKER_NAME_LABELS)
                    .map(label -> String.format("{{ index .Config.Labels \"%s\" }}", label))
                    .append(DOCKER_NAME_TAGS)
                    .collect(Collectors.joining(","));
            String labelsString = Iterables.getOnlyElement(runDockerProcess(
                    docker,
                    "inspect",
                    "--format", labelsFormat,
                    containerId));
            return Splitter.on(CharMatcher.anyOf(",/"))
                    .omitEmptyStrings()
                    .splitToList(labelsString);
        } catch (IOException | InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }

    public static Optional<String> getContainerIpFromId(DockerExecutable docker, String containerId) {
        try {
            String ip = Iterables.getOnlyElement(runDockerProcess(
                    docker,
                    "inspect",
                    "--format",
                    "{{ range .NetworkSettings.Networks }}{{ .IPAddress }}{{ end }}",
                    containerId));

            // stopped containers don't return IPs
            if (ip.trim().isEmpty()) {
                return Optional.empty();
            }

            Preconditions.checkState(InetAddresses.isInetAddress(ip), "IP address is not valid: " + ip);
            return Optional.of(ip);
        } catch (InterruptedException | IOException | RuntimeException e) {
            throw new IllegalStateException("Couldn't get IP for container ID " + containerId, e);
        }
    }

    public static List<String> getContainerIdsOnNetwork(DockerExecutable docker, String networkName) {
        try {
            String containersOnNetworkString = Iterables.getOnlyElement(DockerContainerInfoUtils.runDockerProcess(
                    docker,
                    "network",
                    "inspect",
                    "--format", "{{ range $container, $_ := .Containers }}{{ $container }},{{ end }}",
                    networkName));

            return Splitter.on(',')
                    .omitEmptyStrings()
                    .splitToList(containersOnNetworkString);
        } catch (InterruptedException | IOException | RuntimeException e) {
            throw new IllegalStateException("Unable to find the container IDs on the network " + networkName, e);
        }
    }

    public static List<String> getContainerIdsInDockerComposeProject(
            DockerExecutable docker,
            ProjectName projectName) {
        try {
            return DockerContainerInfoUtils.runDockerProcess(
                    docker,
                    "ps",
                    "--filter", "label=com.docker.compose.project=" + projectName.asString(),
                    "--format", "{{ .ID }}");
        } catch (IOException | InterruptedException | RuntimeException e) {
            throw new IllegalStateException(
                    "Unable to get container IDs in the docker compose project " + projectName.asString(),
                    e);
        }
    }

    private static List<String> runDockerProcess(DockerExecutable docker, String... args)
            throws IOException, InterruptedException {
        Process process = docker.execute(args);
        if (!process.waitFor(5, TimeUnit.SECONDS) || process.exitValue() != 0) {
            throw new IllegalStateException("Unable to execute docker command: " + ImmutableList.copyOf(args));
        }
        return getLinesFromInputStream(process.getInputStream());
    }

    private static List<String> getLinesFromInputStream(InputStream inputStream) throws IOException {
        try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return CharStreams.readLines(inputStreamReader);
        }
    }
}
