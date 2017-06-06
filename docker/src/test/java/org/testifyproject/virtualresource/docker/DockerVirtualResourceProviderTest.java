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

import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import org.testifyproject.MethodDescriptor;
import org.testifyproject.MockProvider;
import org.testifyproject.StartStrategy;
import org.testifyproject.TestConfigurer;
import org.testifyproject.TestContext;
import org.testifyproject.TestDescriptor;
import org.testifyproject.VirtualResourceInstance;
import org.testifyproject.annotation.VirtualResource;
import org.testifyproject.core.DefaultTestContextBuilder;
import org.testifyproject.core.util.ReflectionUtil;
import org.testifyproject.core.util.SettingUtil;
import org.testifyproject.spotify.docker.client.DefaultDockerClient;
import org.testifyproject.spotify.docker.client.exceptions.DockerCertificateException;
import org.testifyproject.spotify.docker.client.messages.RegistryAuth;
import org.testifyproject.spotify.docker.client.messages.RegistryAuthSupplier;
import org.testifyproject.trait.DefaultPropertiesReader;
import org.testifyproject.trait.PropertiesReader;

/**
 *
 * @author saden
 */
public class DockerVirtualResourceProviderTest {

    DockerVirtualResourceProvider sut;
    TestContext testContext;
    VirtualResource virtualResource;

    @Before
    public void init() {
        testContext = mock(TestContext.class);
        VirtualResource delegate = ReflectionUtil.INSTANCE.newInstance(VirtualResource.class);
        virtualResource = mock(VirtualResource.class, delegatesTo(delegate));

        sut = new DockerVirtualResourceProvider();
    }

    @After
    public void destroy() {
        sut.stop(testContext, virtualResource);
    }

    @Test
    public void callToConfigureShouldReturnBuilder() {
        PropertiesReader reader = mock(PropertiesReader.class);
        PropertiesReader configReader = mock(PropertiesReader.class);

        given(configReader.isEmpty()).willReturn(true);
        given(testContext.getPropertiesReader("docker")).willReturn(reader);
        given(reader.isEmpty()).willReturn(false);

        DefaultDockerClient.Builder result = sut.configure(testContext, virtualResource, configReader);
        assertThat(result).isNotNull();
    }

    @Test
    public void givenValidParametersCallToStartAndStopContainerShouldSucceed() throws DockerCertificateException {
        StartStrategy resourceStartStrategy = StartStrategy.EAGER;
        Object testInstance = new Object();
        MethodDescriptor methodDescriptor = mock(MethodDescriptor.class);
        TestDescriptor testDescriptor = mock(TestDescriptor.class);
        TestConfigurer testConfigurer = mock(TestConfigurer.class);
        MockProvider mockProvider = mock(MockProvider.class);
        Map<String, Object> properties = SettingUtil.INSTANCE.getSettings();
        Map<String, String> dependencies = mock(Map.class);

        testContext = new DefaultTestContextBuilder()
                .resourceStartStrategy(resourceStartStrategy)
                .testInstance(testInstance)
                .testDescriptor(testDescriptor)
                .testMethodDescriptor(methodDescriptor)
                .testConfigurer(testConfigurer)
                .mockProvider(mockProvider)
                .properties(properties)
                .dependencies(dependencies)
                .build();

        given(virtualResource.value()).willReturn("postgres");
        given(virtualResource.version()).willReturn("9.4.12");
        given(testContext.getTestName()).willReturn("TestClass");
        given(testContext.getMethodName()).willReturn("testMethod");

        PropertiesReader reader = new DefaultPropertiesReader(properties);
        PropertiesReader dockerReader = reader.getPropertiesReader("docker");

        RegistryAuth registryAuth = RegistryAuth.builder()
                .email(dockerReader.getProperty("email"))
                .username(dockerReader.getProperty("username"))
                .password(dockerReader.getProperty("password"))
                .serverAddress(dockerReader.getProperty("uri"))
                .build();

        RegistryAuthSupplier dockerHubAuthSupplier = new DockerHubRegistryAuthSupplier(registryAuth);
        DefaultDockerClient.Builder builder = DefaultDockerClient.fromEnv()
                .registryAuthSupplier(dockerHubAuthSupplier);
        VirtualResourceInstance result = sut.start(testContext, virtualResource, builder);

        assertThat(result).isNotNull();
    }

}
