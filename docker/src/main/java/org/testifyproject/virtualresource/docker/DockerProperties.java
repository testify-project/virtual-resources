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

/**
 * A class that defines a list keys for properties added by
 * {@link DockerVirtualResourceProvider}.
 *
 * @author saden
 */
public class DockerProperties {

    /**
     * Docker client property key.
     */
    public static final String DOCKER_CLIENT = "dockerClient";

    /**
     * Docker container property key.
     */
    public static final String DOCKER_CONTAINER = "dockerContainer";

    /**
     * Docker containers property key.
     */
    public static final String DOCKER_CONTAINERS = "dockerContainers";

    private DockerProperties() {
    }

}
