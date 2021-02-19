/*
 * Copyright (c) 2019, iKentoo SA. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.ikentoo.hazelcast;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Slf4jFactory;
import com.hazelcast.spi.discovery.DiscoveryNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

public class AwsEcsDiscoveryStrategyIT {
    @Rule public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    Map<String, Comparable> properties = new HashMap<>();

    ILogger ilogger;

    static {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
    }

    /**
     * Use statics to define your cluster / service name as well as the regexps for them to test
     * against
     */
    String cluster_name;

    String cluster_name_regexp;

    String service_name;
    String service_name_regexp;

    @Before
    public void setup() {

        /** Configure logging */
        ilogger = new Slf4jFactory().getLogger("simple");

        /** Check for existing aws access parameters */
        if (!System.getenv().containsKey("AWS_DEFAULT_REGION")
                || !System.getenv().containsKey("AWS_ACCESS_KEY")
                || !System.getenv().containsKey("AWS_SECRET_ACCESS_KEY")) {
            fail(
                    "Please provide AWS_ACCESS_KEY, AWS_SECRET_ACCESS_KEY and AWS_DEFAULT_REGION environment variables.");
        }

        /** extract cluster, service names as well as regexps */
        cluster_name = System.getenv("CLUSTER_NAME");
        cluster_name_regexp = System.getenv("CLUSTER_NAME_REGEXP");

        service_name = System.getenv("SERVICE_NAME");
        service_name_regexp = System.getenv("SERVICE_NAME_REGEXP");

        /** Show current name / regexp config */
        ilogger.info("CLUSTER_NAME = " + cluster_name);
        ilogger.info("CLUSTER_NAME_REGEXP = " + cluster_name_regexp);
        ilogger.info("SERVICE_NAME = " + service_name);
        ilogger.info("SERVICE_NAME_REGEXP = " + service_name_regexp);

        /** Setup Properties */
        properties.put("region", System.getenv("AWS_DEFAULT_REGION"));
        properties.put("access-key", System.getenv("AWS_ACCESS_KEY"));
        properties.put("secret-key", System.getenv("AWS_SECRET_ACCESS_KEY"));
    }

    @Test
    public void testDiscoveryStrategyClusterRegExpServiceRegexp() throws IOException {

        /** Set Task ARN through test file */
        Path tempFile = makeTaskFile();
        environmentVariables.set(
                "ECS_CONTAINER_METADATA_FILE", tempFile.toAbsolutePath().toString());

        /** Configure logging */
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
        ILogger ilogger = new Slf4jFactory().getLogger("simple");

        /** Configure AwsEcsDiscoveryStrategy */
        AwsEcsDiscoveryStrategyFactory awsEcsDiscoveryStrategyFactory =
                new AwsEcsDiscoveryStrategyFactory();
        properties.put("cluster-name-regexp", cluster_name_regexp);
        properties.put("service-name-regexp", service_name_regexp);

        /** Try to run discoversNodes() method */
        try {
            AwsEcsDiscoveryStrategy strategy = new AwsEcsDiscoveryStrategy(ilogger, properties);
            Iterator<DiscoveryNode> iterator = strategy.discoverNodes().iterator();

            assertTrue(iterator.hasNext());

            while (iterator.hasNext()) {
                DiscoveryNode discoveryNode = iterator.next();
                ilogger.info(
                        "Private / Public Address Found : "
                                + discoveryNode.getPrivateAddress()
                                + " / "
                                + discoveryNode.getPublicAddress());
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testDiscoveryStrategyClusterNameServiceRegExp() throws IOException {

        /** Set Task ARN through test file */
        Path tempFile = makeTaskFile();
        environmentVariables.set(
                "ECS_CONTAINER_METADATA_FILE", tempFile.toAbsolutePath().toString());

        /** Configure logging */
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
        ILogger ilogger = new Slf4jFactory().getLogger("simple");

        /** Configure AwsEcsDiscoveryStrategy */
        AwsEcsDiscoveryStrategyFactory awsEcsDiscoveryStrategyFactory =
                new AwsEcsDiscoveryStrategyFactory();
        properties.put("cluster", cluster_name);
        properties.put("service-name-regexp", service_name_regexp);

        /** Try to run discoversNodes() method */
        try {
            AwsEcsDiscoveryStrategy strategy = new AwsEcsDiscoveryStrategy(ilogger, properties);
            Iterator<DiscoveryNode> iterator = strategy.discoverNodes().iterator();

            assertTrue(iterator.hasNext());

            while (iterator.hasNext()) {
                DiscoveryNode discoveryNode = iterator.next();
                ilogger.info(
                        "Private / Public Address Found : "
                                + discoveryNode.getPrivateAddress()
                                + " / "
                                + discoveryNode.getPublicAddress());
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testDiscoveryStrategyClusterNameServiceName() throws IOException {

        /** Set Task ARN through test file */
        Path tempFile = makeTaskFile();
        environmentVariables.set(
                "ECS_CONTAINER_METADATA_FILE", tempFile.toAbsolutePath().toString());

        /** Configure AwsEcsDiscoveryStrategy */
        AwsEcsDiscoveryStrategyFactory awsEcsDiscoveryStrategyFactory =
                new AwsEcsDiscoveryStrategyFactory();
        properties.put("cluster", cluster_name);
        properties.put("service", service_name);

        /** Try to run discoversNodes() method */
        try {
            AwsEcsDiscoveryStrategy strategy = new AwsEcsDiscoveryStrategy(ilogger, properties);
            Iterator<DiscoveryNode> iterator = strategy.discoverNodes().iterator();

            assertTrue(iterator.hasNext());

            while (iterator.hasNext()) {
                DiscoveryNode discoveryNode = iterator.next();
                ilogger.info(
                        "Private / Public Address Found : "
                                + discoveryNode.getPrivateAddress()
                                + " / "
                                + discoveryNode.getPublicAddress());
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private Path makeTaskFile() throws IOException {

        String example =
                "{\n"
                        + "    \"Cluster\": \"default\",\n"
                        + "    \"ContainerInstanceARN\": \"arn:aws:ecs:us-west-2:012345678910:container-instance/1f73d099-b914-411c-a9ff-81633b7741dd\",\n"
                        + "    \"TaskARN\": \"arn:aws:ecs:us-west-2:012345678910:task/d90675f8-1a98-444b-805b-3d9cabb6fcd4\",\n"
                        + "    \"ContainerName\": \"metadata\"\n"
                        + "}";

        Path tempFile = Files.createTempFile("meta", "json");
        Files.write(tempFile, example.getBytes(StandardCharsets.UTF_8));
        return tempFile;
    }
}
