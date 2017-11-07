/*
 * Copyright 2016-2017 Testify Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.testifyproject.virtualresource.docker;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import static org.testifyproject.virtualresource.docker.DockerProperties.DOCKER_CONTAINERS;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.testifyproject.TestContext;
import org.testifyproject.VirtualResourceInstance;
import org.testifyproject.VirtualResourceProvider;
import org.testifyproject.annotation.Discoverable;
import org.testifyproject.annotation.VirtualResource;
import org.testifyproject.core.VirtualResourceInstanceBuilder;
import org.testifyproject.core.util.ExceptionUtil;
import org.testifyproject.core.util.ExpressionUtil;
import org.testifyproject.core.util.LoggingUtil;
import org.testifyproject.failsafe.Failsafe;
import org.testifyproject.failsafe.RetryPolicy;
import org.testifyproject.guava.common.net.InetAddresses;
import org.testifyproject.spotify.docker.client.AnsiProgressHandler;
import org.testifyproject.spotify.docker.client.DefaultDockerClient;
import org.testifyproject.spotify.docker.client.exceptions.DockerCertificateException;
import org.testifyproject.spotify.docker.client.exceptions.DockerException;
import org.testifyproject.spotify.docker.client.messages.ContainerConfig;
import org.testifyproject.spotify.docker.client.messages.ContainerCreation;
import org.testifyproject.spotify.docker.client.messages.ContainerInfo;
import org.testifyproject.spotify.docker.client.messages.HostConfig;
import org.testifyproject.spotify.docker.client.messages.PortBinding;
import org.testifyproject.spotify.docker.client.messages.RegistryAuth;
import org.testifyproject.spotify.docker.client.messages.RegistryAuthSupplier;
import org.testifyproject.trait.PropertiesReader;

/**
 * A Docker implementation of {@link VirtualResourceProvider SPI Contract}.
 *
 * @author saden
 */
@Discoverable
public class DockerVirtualResourceProvider
        implements VirtualResourceProvider<DefaultDockerClient.Builder> {

    public static final String DEFAULT_CONFIG_KEY = "docker";
    public static final String DEFAULT_VERSION = "latest";

    private DefaultDockerClient client;
    private final AtomicBoolean started = new AtomicBoolean(false);

    @Override
    public DefaultDockerClient.Builder configure(TestContext testContext,
            VirtualResource virtualResource, PropertiesReader configReader) {
        try {
            DefaultDockerClient.Builder builder = DefaultDockerClient.fromEnv();
            PropertiesReader reader = configReader;

            if (reader.isEmpty()) {
                reader = testContext.getPropertiesReader(DEFAULT_CONFIG_KEY);
            }

            if (!reader.isEmpty()) {
                RegistryAuth registryAuth = RegistryAuth.builder()
                        .serverAddress(reader.getProperty("uri"))
                        .email(reader.getProperty("email"))
                        .username(reader.getProperty("username"))
                        .password(reader.getProperty("password"))
                        .build();

                RegistryAuthSupplier dockerHubAuthSupplier =
                        new DockerHubRegistryAuthSupplier(registryAuth);

                //TODO: explore making these configuration configurable via .testify.yml file
                builder.registryAuthSupplier(dockerHubAuthSupplier)
                        .connectTimeoutMillis(10000)
                        .connectionPoolSize(16);
            }

            return builder;
        } catch (DockerCertificateException e) {
            throw ExceptionUtil.INSTANCE.propagate(e);
        }
    }

    @Override
    @SuppressWarnings("UseSpecificCatch")
    public VirtualResourceInstance start(TestContext testContext,
            VirtualResource virtualResource, DefaultDockerClient.Builder clientBuilder) {
        if (started.compareAndSet(false, true)) {
            VirtualResourceInstance virtualResourceInstance = null;
            int nodes = virtualResource.nodes();

            try {
                for (int i = 1; i <= nodes; i++) {
                    LoggingUtil.INSTANCE.info("Connecting to {}", clientBuilder.uri());
                    client = clientBuilder.build();

                    String imageName = virtualResource.value();
                    String imageTag = getImageTag(virtualResource.version());

                    String image = imageName + ":" + imageTag;
                    boolean imagePulled = isImagePulled(image, imageTag);

                    if (virtualResource.pull() && !imagePulled) {
                        pullImage(virtualResource, image);
                    }

                    ContainerConfig.Builder containerConfigBuilder =
                            ContainerConfig.builder().image(image);

                    if (!virtualResource.cmd().isEmpty()) {
                        containerConfigBuilder.cmd(virtualResource.cmd());
                    }

                    String containerName;

                    if (nodes == 1) {
                        containerName = virtualResource.name().isEmpty()
                                ? String.format("%s-%s", testContext.getName(), UUID
                                        .randomUUID().toString())
                                : String.format("%s-%s", virtualResource.name(), UUID
                                        .randomUUID().toString());
                    } else {
                        containerName = virtualResource.name().isEmpty()
                                ? String.format("%s-%d-%s", testContext.getName(), i, UUID
                                        .randomUUID().toString())
                                : String.format("%s-%d-%s", virtualResource.name(), i,
                                        UUID.randomUUID().toString());
                    }

                    HostConfig.Builder hostConfigBuilder = HostConfig.builder();

                    if (virtualResource.link()) {
                        List<String> containerNames =
                                testContext.<ContainerInfo>findCollection(
                                        DOCKER_CONTAINERS)
                                        .stream()
                                        .map(p -> p.name().replace("/", ""))
                                        .collect(toList());

                        hostConfigBuilder.links(containerNames);
                    }

                    for (String env : virtualResource.env()) {
                        try {
                            Map<String, Object> templateContext =
                                    testContext.<ContainerInfo>findCollection(
                                            DOCKER_CONTAINERS)
                                            .stream()
                                            .collect(toMap(p -> p.name()
                                                    .replace("/", ""), p -> p));

                            String evaluation = ExpressionUtil.INSTANCE.evaluateTemplate(
                                    env, templateContext);
                            containerConfigBuilder.env(evaluation);
                        } catch (Exception e) {
                            LoggingUtil.INSTANCE.debug(
                                    "Could not evaluate env '{}' as an expression ", env);
                        }
                    }

                    HostConfig hostConfig = hostConfigBuilder
                            .publishAllPorts(true)
                            .build();

                    ContainerConfig containerConfig = containerConfigBuilder
                            .hostConfig(hostConfig)
                            .build();

                    ContainerCreation containerCreation = client.createContainer(
                            containerConfig, containerName);
                    String containerId = containerCreation.id();
                    client.startContainer(containerId);

                    ContainerInfo containerInfo = client.inspectContainer(containerId);
                    InetAddress containerAddress = InetAddresses.forString(containerInfo
                            .networkSettings().ipAddress());
                    Map<String, List<PortBinding>> containerPorts = containerInfo
                            .networkSettings().ports();

                    testContext.addCollectionElement(DOCKER_CONTAINERS, containerInfo);

                    if (containerPorts != null) {
                        Map<Integer, Integer> mappedPorts = containerPorts.entrySet()
                                .stream()
                                .collect(collectingAndThen(toMap(
                                        k -> Integer.valueOf(k.getKey().split("/")[0]),
                                        v -> Integer.valueOf(v.getValue().get(0)
                                                .hostPort())),
                                        Collections::unmodifiableMap));

                        if (virtualResource.await()) {
                            waitForPorts(virtualResource, mappedPorts, containerAddress);
                        }
                    }

                    //return the first node
                    if (i == 1) {
                        virtualResourceInstance = VirtualResourceInstanceBuilder.builder()
                                .resource(containerAddress, InetAddress.class)
                                .property(DockerProperties.DOCKER_CLIENT, client)
                                .property(DockerProperties.DOCKER_CONTAINER, containerInfo)
                                .build(image, virtualResource);
                    }
                }

                return virtualResourceInstance;
            } catch (Exception e) {
                stop(testContext, virtualResource, virtualResourceInstance);
                throw ExceptionUtil.INSTANCE.propagate(e);
            }
        }

        throw ExceptionUtil.INSTANCE.propagate("Docker containers already started.");
    }

    @Override
    public void stop(TestContext testContext, VirtualResource virtualResource,
            VirtualResourceInstance instance) {
        try {
            if (started.compareAndSet(true, false)) {
                testContext.<ContainerInfo>findCollection(DOCKER_CONTAINERS).stream().map(
                        p -> p.id())
                        .forEachOrdered(containerId -> {
                            LoggingUtil.INSTANCE.info(
                                    "Stopping and Removing Docker Container {}",
                                    containerId);
                            RetryPolicy retryPolicy = new RetryPolicy()
                                    .retryOn(Throwable.class)
                                    .withBackoff(virtualResource.delay(), virtualResource
                                            .maxDelay(), virtualResource.unit());

                            stopContainer(containerId, retryPolicy);
                        });
            }
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    /**
     * Get the image tag based on the given version. If version is not specified then use
     * {@link #DEFAULT_VERSION}
     *
     * @param version the version defined in the VirtualResource annotation.
     * @return the docker image tag
     */
    String getImageTag(String version) {
        String imageTag;
        //if version is not specified then use latest as the image tag
        if (version.isEmpty()) {
            imageTag = DEFAULT_VERSION;
        } else {
            imageTag = version;
        }

        return imageTag;
    }

    /**
     * Determine if the image is already pulled.
     *
     * @param image the image name
     * @param imageTag the image tag
     * @return true if the image is already pulled, false otherwise
     */
    boolean isImagePulled(String image, String imageTag) {
        boolean imagePulled = false;
        //determine if the image has already been pulled
        try {
            client.inspectImage(image);

            //if the tag is not the latest then that means we can look to see if
            //the image has been pulled. if it is then that means we always go and
            //pull the latest image by setting leaving imagePulled as false
            if (!DEFAULT_VERSION.equals(imageTag)) {
                imagePulled = true;
            }
        } catch (InterruptedException | DockerException e) {
            LoggingUtil.INSTANCE.info("Image '{}' not found", image);
        }

        return imagePulled;
    }

    /**
     * Pull the given virtual resource.
     *
     * @param virtualResource the virtual resource
     * @param image the image
     */
    void pullImage(VirtualResource virtualResource, String image) {
        RetryPolicy retryPolicy = new RetryPolicy()
                .retryOn(Throwable.class)
                .withDelay(virtualResource.delay(), virtualResource.unit())
                .withMaxRetries(virtualResource.maxRetries());

        Failsafe.with(retryPolicy)
                .onRetry(throwable -> LoggingUtil.INSTANCE.warn(
                        "Retrying pull request of image '{}'", image, throwable))
                .onFailure(throwable -> LoggingUtil.INSTANCE.error(
                        "Image image '{}' could not be pulled: ", image, throwable))
                .run(() -> client.pull(image, new AnsiProgressHandler()));
    }

    /**
     * Wait for all the container exposed ports to be available.
     *
     * @param virtualResource the virtual resource
     * @param mappedPorts the container mapped ports
     * @param host the container address
     */
    void waitForPorts(VirtualResource virtualResource, Map<Integer, Integer> mappedPorts,
            InetAddress host) {
        RetryPolicy retryPolicy = new RetryPolicy()
                .retryOn(IOException.class)
                .withBackoff(virtualResource.delay(), virtualResource.maxDelay(),
                        virtualResource.unit());

        //if ports are explicitly defined then wait for those ports
        int[] ports = virtualResource.ports();
        if (ports.length != 0) {
            for (int port : ports) {
                Failsafe.with(retryPolicy).run(() -> {
                    LoggingUtil.INSTANCE.info("Waiting for '{}:{}' to be reachable", host
                            .getHostAddress(), port);
                    new Socket(host, port).close();
                });
            }
        } else {
            mappedPorts.entrySet().forEach(entry ->
                    Failsafe.with(retryPolicy).run(() -> {
                        LoggingUtil.INSTANCE.info(
                                "Waiting for '{}:{}' to be reachable",
                                host.getHostAddress(), entry.getKey());
                        new Socket(host, entry.getKey()).close();
                    }));
        }
    }

    /**
     * Stop the given container using the given retry policy.
     *
     * @param retryPolicy the retry policy in the event of failure
     * @param containerId the container id
     */
    void stopContainer(String containerId, RetryPolicy retryPolicy) {
        Failsafe.with(retryPolicy)
                .onRetry(throwable -> LoggingUtil.INSTANCE.info(
                        "Trying to stop Docker Container '{}'", containerId))
                .onSuccess(result -> {
                    LoggingUtil.INSTANCE
                            .info("Docker Container '{}' stopped", containerId);
                    removeContainer(containerId, retryPolicy);
                })
                .onFailure(throwable -> LoggingUtil.INSTANCE.error(
                        "Docker Container '{}' could not be stopped", containerId,
                        throwable))
                .run(() -> client.stopContainer(containerId, 8));
    }

    /**
     * Remove the given container using the given retry policy.
     *
     * @param retryPolicy the retry policy in the event of failure
     * @param containerId the container id
     */
    void removeContainer(String containerId, RetryPolicy retryPolicy) {
        Failsafe.with(retryPolicy)
                .onRetry(throwable -> LoggingUtil.INSTANCE.info(
                        "Trying to remove Docker Container '{}'", containerId))
                .onSuccess(result -> LoggingUtil.INSTANCE.info(
                        "Docker Container '{}' removed", containerId))
                .onFailure(throwable -> LoggingUtil.INSTANCE.error(
                        "Docker Container '{}' could not be removed", containerId,
                        throwable))
                .run(() -> client.removeContainer(containerId));
    }

}
