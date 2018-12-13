/*
 * (c) Copyright 2017 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.docker.proxy;

import static com.palantir.docker.proxy.DockerContainerInfoUtils.IP_FORMAT_STRING;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.palantir.docker.compose.execution.DockerExecutable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class DockerContainerInfoUtilsTest {
    private static final String CONTAINER_ID = "container-id";

    private final Process response = mock(Process.class);
    private final DockerExecutable dockerExecutable = mock(DockerExecutable.class);

    @Test
    public void getContainerIpFromIdDoesNotThrowWhenContainerIsStopped() throws IOException, InterruptedException {
        when(response.getInputStream()).thenReturn(getDockerOutputForStoppedContainer());
        when(response.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(response.exitValue()).thenReturn(0);
        when(dockerExecutable.execute("inspect", "--format", IP_FORMAT_STRING, CONTAINER_ID)).thenReturn(response);

        Optional<String> ip = DockerContainerInfoUtils.getContainerIpFromId(dockerExecutable, CONTAINER_ID);
        assertFalse(ip.isPresent());
    }

    private InputStream getDockerOutputForStoppedContainer() {
        return new ByteArrayInputStream("\n".getBytes(StandardCharsets.UTF_8));
    }
}
