/*
 * (c) Copyright 2017 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.docker.proxy;

import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.configuration.ProjectName;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.execution.DockerExecutable;
import com.palantir.docker.compose.execution.DockerExecutionException;
import com.palantir.docker.compose.logging.LogDirectory;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.ProxySelector;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import org.junit.rules.ExternalResource;

@SuppressWarnings("PreferSafeLoggableExceptions")
public final class DockerProxyRule extends ExternalResource {
    private final DockerContainerInfo dockerContainerInfo;
    private final DockerComposeRule dockerComposeRule;

    private ProxySelector originalProxySelector;
    private Object originalNameService;

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
        DockerContainerInfo builtDockerContainerInfo = dockerContainerInfoCreator.apply(DockerExecutable.builder()
                .dockerConfiguration(DockerMachine.localMachine().build())
                .build());
        String logDirectory = DockerProxyRule.class.getSimpleName() + "-" + classToLogFor.getSimpleName();
        this.dockerContainerInfo = new CachingDockerContainerInfo(builtDockerContainerInfo);
        this.dockerComposeRule = DockerComposeRule.builder()
                .file(getDockerComposeFile(this.dockerContainerInfo.getNetworkName()).getPath())
                .waitingForService("proxy", Container::areAllPortsOpen)
                .saveLogsTo(LogDirectory.circleAwareLogDirectory(logDirectory))
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
            setNameService(new DockerNameService(dockerContainerInfo));
            ProxySelector.setDefault(new DockerProxySelector(
                    dockerComposeRule.containers(),
                    dockerContainerInfo,
                    originalProxySelector));
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
        unsetNameService();
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

    private void setNameService(DockerNameService nameService) {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            getJava8NameServices().add(0, wrapNameService("sun.net.spi.nameservice.NameService", nameService, null));
        } else {
            originalNameService = getJava9NameService();
            setJava9NameService(wrapNameService("java.net.InetAddress$NameService", nameService, originalNameService));
        }
    }

    private void unsetNameService() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            getJava8NameServices().remove(0);
        } else {
            setJava9NameService(originalNameService);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Object> getJava8NameServices() {
        try {
            Field nameServices = InetAddress.class.getDeclaredField("nameServices");
            nameServices.setAccessible(true);
            return (List<Object>) nameServices.get(null);
        } catch (Throwable e) {
            throw new IllegalStateException("Unable to get Java 8 name services", e);
        }
    }

    private static Object getJava9NameService() {
        try {
            Field nameService = InetAddress.class.getDeclaredField("nameService");
            nameService.setAccessible(true);
            return nameService.get(null);
        } catch (Throwable e) {
            throw new IllegalStateException("Unable to get Java 9+ name service", e);
        }
    }

    private static void setJava9NameService(Object newNameService) {
        try {
            Field nameService = InetAddress.class.getDeclaredField("nameService");
            nameService.setAccessible(true);
            nameService.set(null, newNameService);
        } catch (Throwable e) {
            throw new IllegalStateException("Unable to set Java 9+ name service", e);
        }
    }

    private static Object wrapNameService(String className, Object delegate, Object fallback) {
        try {
            Class<?> clazz = Class.forName(className);
            return Proxy.newProxyInstance(
                    clazz.getClassLoader(),
                    new Class<?>[] { clazz },
                    new ForwardingNameServiceHandler(delegate, fallback));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to find class " + className, e);
        }
    }

    @SuppressWarnings("checkstyle:IllegalThrows")
    private static class ForwardingNameServiceHandler implements InvocationHandler {
        private final Object delegate;
        private final Object fallback;

        ForwardingNameServiceHandler(Object delegate, Object fallback) {
            this.delegate = delegate;
            this.fallback = fallback;
        }

        @Override
        public Object invoke(Object _proxy, Method method, Object[] args) throws Throwable {
            try {
                return callAndUnwrap(delegate, method, args);
            } catch (UnknownHostException e) {
                if (fallback != null) {
                    return callAndUnwrap(fallback, method, args);
                }
                throw e;
            }
        }

        private static Object callAndUnwrap(Object obj, Method method, Object[] args) throws Throwable {
            try {
                Method delegateMethod = obj.getClass().getMethod(method.getName(), method.getParameterTypes());
                delegateMethod.setAccessible(true);
                return delegateMethod.invoke(obj, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            } catch (IllegalAccessException | IllegalArgumentException e) {
                throw new IllegalStateException("Couldn't call method on underlying object", e);
            }
        }
    }
}
