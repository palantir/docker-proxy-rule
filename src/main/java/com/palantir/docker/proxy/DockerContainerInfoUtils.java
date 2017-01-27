/*
 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.proxy;

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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DockerContainerInfoUtils {
    private static final List<String> DOCKER_NAME_LABELS = ImmutableList.of(
            "com.docker.compose.service",
            "hostname");

    private DockerContainerInfoUtils() {
        // Utility class
    }

    public static List<String> getAllNamesForContainerId(DockerExecutable docker, String containerId) {
        try {
            // If the docker version is 1.13.0, then .Label doesn't work.
            // See https://github.com/docker/docker/pull/30291
            String dockerVersion = getDockerVersion(docker);

            String namesFormat = "{{ .Names }}";
            if (!dockerVersion.equals("1.13.0")) {
                namesFormat += DOCKER_NAME_LABELS.stream()
                        .map(label -> String.format(",{{ .Label \"%s\" }}", label))
                        .collect(Collectors.joining());
            }
            String namesString = Iterables.getOnlyElement(runDockerProcess(
                    docker,
                    "ps",
                    "--filter", "id=" + containerId,
                    "--format", namesFormat));
            List<String> names = Splitter.on(',')
                    .omitEmptyStrings()
                    .splitToList(namesString);
            ImmutableList.Builder<String> allContainerNames = ImmutableList.<String>builder()
                    .add(containerId)
                    .addAll(names);
            if (dockerVersion.equals("1.13.0")) {
                String labelsFormat = DOCKER_NAME_LABELS.stream()
                        .map(label -> String.format("{{ index .Config.Labels \"%s\" }}", label))
                        .collect(Collectors.joining(","));
                String labelsString = Iterables.getOnlyElement(runDockerProcess(
                        docker,
                        "inspect",
                        containerId,
                        "--format", labelsFormat));
                List<String> labels = Splitter.on(',')
                        .omitEmptyStrings()
                        .splitToList(labelsString);
                allContainerNames.addAll(labels);
            }
            return allContainerNames.build();
        } catch (IOException | InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }

    public static String getContainerIpFromId(DockerExecutable docker, String containerId) {
        try {
            String ip = Iterables.getOnlyElement(runDockerProcess(
                    docker,
                    "inspect",
                    containerId,
                    "--format",
                    "{{ range .NetworkSettings.Networks }}{{ .IPAddress }}{{ end }}"));
            Preconditions.checkState(InetAddresses.isInetAddress(ip), "IP address is not valid: " + ip);
            return ip;
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
                    networkName,
                    "--format", "{{ range $container, $_ := .Containers }}{{ $container }},{{ end }}"));

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

    private static String getOnlyLineFromInputStream(InputStream inputStream) throws IOException {
        return Iterables.getOnlyElement(getLinesFromInputStream(inputStream));
    }

    private static List<String> getLinesFromInputStream(InputStream inputStream) throws IOException {
        try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return CharStreams.readLines(inputStreamReader);
        }
    }

    private static String getDockerVersion(DockerExecutable docker) throws IOException, InterruptedException {
        Process process = docker.execute("version", "--format", "{{ .Client.Version }}");
        if (!process.waitFor(5, TimeUnit.SECONDS) || process.exitValue() != 0) {
            throw new IllegalStateException("Couldn't get docker version");
        }
        return getOnlyLineFromInputStream(process.getInputStream());
    }
}
