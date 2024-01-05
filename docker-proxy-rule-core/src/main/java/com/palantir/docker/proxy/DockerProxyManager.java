/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.docker.proxy;

import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.palantir.docker.compose.DockerComposeManager;
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
import java.util.function.UnaryOperator;

@SuppressWarnings("PreferSafeLoggableExceptions")
abstract class DockerProxyManager<SelfT extends DockerComposeManager.BuilderExtensions<SelfT>> {
    private final DockerContainerInfo dockerContainerInfo;
    private final DockerComposeManager dockerComposeRule;

    private ProxySelector originalProxySelector;
    private Object originalNameService;
    private static DockerNameService dockerNameService;

    /**
     * Creates a {@link DockerProxyManager} which will create a proxy and DNS so that
     * tests can interface with docker containers directly.
     *
     * @param dockerContainerInfoCreator A {@link Function} that creates the DockerContainerInfo to use
     * @param classToLogFor The class using {@link DockerProxyManager}
     */
    DockerProxyManager(
            Customizer<SelfT> builderSupplier,
            Function<DockerExecutable, DockerContainerInfo> dockerContainerInfoCreator,
            Class<?> classToLogFor) {
        DockerContainerInfo builtDockerContainerInfo = dockerContainerInfoCreator.apply(DockerExecutable.builder()
                .dockerConfiguration(DockerMachine.localMachine().build())
                .build());
        String logDirectory = DockerProxyManager.class.getSimpleName() + "-" + classToLogFor.getSimpleName();
        this.dockerContainerInfo = new CachingDockerContainerInfo(builtDockerContainerInfo);
        this.dockerComposeRule = builderSupplier.customize(builder -> builder.file(getDockerComposeFile(
                                this.dockerContainerInfo.getNetworkName(),
                                this.dockerContainerInfo.getImageNameOverride().orElse("vimagick/dante:latest"))
                        .getPath())
                .waitingForService("proxy", Container::areAllPortsOpen)
                .saveLogsTo(LogDirectory.circleAwareLogDirectory(logDirectory)));
    }

    public interface Customizer<T> {
        DockerComposeManager customize(UnaryOperator<T> customizeFunction);
    }

    public void before() throws IOException, InterruptedException {
        try {
            originalProxySelector = ProxySelector.getDefault();
            dockerComposeRule.before();
            setNameService(new DockerNameService(dockerContainerInfo));
            ProxySelector.setDefault(new DockerProxySelector(
                    dockerComposeRule.containers(), dockerContainerInfo, originalProxySelector));
        } catch (DockerExecutionException e) {
            if (e.getMessage().contains("declared as external")) {
                throw new IllegalStateException(
                        "DockerComposeRule must run before DockerProxyRule. Please use a RuleChain.", e);
            } else {
                throw e;
            }
        }
    }

    public void after() {
        ProxySelector.setDefault(originalProxySelector);
        unsetNameService();
        dockerComposeRule.after();
    }

    private static File getDockerComposeFile(String networkName, String imageName) {
        try {
            File proxyFile = File.createTempFile("proxy", ".yml");
            String proxyConfig =
                    Resources.toString(Resources.getResource("docker-compose.proxy.yml"), StandardCharsets.UTF_8);
            Files.write(
                    proxyConfig.replace("{{NETWORK_NAME}}", networkName).replace("{{IMAGE_NAME}}", imageName),
                    proxyFile,
                    StandardCharsets.UTF_8);
            return proxyFile;
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private void setNameService(DockerNameService nameService) {
        int featureVersion = Runtime.version().feature();
        if (featureVersion < 9) {
            getJava8NameServices().add(0, wrapNameService("sun.net.spi.nameservice.NameService", nameService, null));
        } else if (featureVersion < 21) {
            originalNameService = getJava9NameService();
            setJava9NameService(wrapNameService("java.net.InetAddress$NameService", nameService, originalNameService));
        } else {
            dockerNameService = nameService;
        }
    }

    private void unsetNameService() {
        int featureVersion = Runtime.version().feature();
        if (featureVersion < 9) {
            getJava8NameServices().remove(0);
        } else if (featureVersion < 21) {
            setJava9NameService(originalNameService);
        } else {
            dockerNameService = null;
        }
    }

    public static DockerNameService getDockerNameService() {
        return dockerNameService;
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

    @SuppressWarnings("ProxyNonConstantType")
    private static Object wrapNameService(String className, Object delegate, Object fallback) {
        try {
            Class<?> clazz = Class.forName(className);
            return Proxy.newProxyInstance(
                    clazz.getClassLoader(),
                    new Class<?>[] {clazz},
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
