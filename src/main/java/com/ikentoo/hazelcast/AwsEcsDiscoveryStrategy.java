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

import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.*;
import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.Address;
import com.hazelcast.spi.discovery.AbstractDiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;

import java.net.UnknownHostException;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@SuppressWarnings("raw")
public class AwsEcsDiscoveryStrategy extends AbstractDiscoveryStrategy {

    private final AwsEcsProperties.Config config;

    public AwsEcsDiscoveryStrategy(ILogger logger, Map<String, Comparable> properties) {
        super(logger, properties);
        this.config = AwsEcsProperties.fromProps(properties);
    }

    @Override
    public Iterable<DiscoveryNode> discoverNodes() {
        getLogger().info("called discoverNodes");

        AmazonECSClientBuilder clientBuilder = AmazonECSClientBuilder.standard();
        config.getAwsCredentials().ifPresent(clientBuilder::withCredentials);
        config.getAwsRegion().ifPresent(clientBuilder::withRegion);
        AmazonECS client = clientBuilder.build();

        ListTasksRequest listTaskRequest = new ListTasksRequest();
        listTaskRequest.setCluster(config.getClusterName());
        listTaskRequest.setServiceName(config.getServiceName());
        listTaskRequest.setDesiredStatus(DesiredStatus.RUNNING);
        ListTasksResult taskIds = client.listTasks(listTaskRequest);

        DescribeTasksRequest describeTaskRequest = new DescribeTasksRequest();
        describeTaskRequest.setTasks(taskIds.getTaskArns());
        describeTaskRequest.setCluster(config.getClusterName());
        DescribeTasksResult tasks = client.describeTasks(describeTaskRequest);

        return tasks.getTasks()
                .stream()
                .flatMap(this::fromTask)
                .map(SimpleDiscoveryNode::new)
                .collect(Collectors.toList());
    }

    private Stream<Address> fromTask(Task task) {
        return task.getContainers()
                .stream()
                .filter(container -> container.getName().matches(config.getContainerNameFilter()))
                .flatMap(this::fromContainer);

    }
    private Stream<Address> fromContainer(Container container) {
        return container.getNetworkInterfaces()
                .stream()
                .flatMap(this::fromNetworkInterface);
    }

    private Stream<Address> fromNetworkInterface(NetworkInterface networkInterface) {
        return config.getPorts()
                .mapToObj(port -> {
                    try {
                        return new Address(networkInterface.getPrivateIpv4Address(), port);
                    } catch (UnknownHostException e) {
                        getLogger().severe(e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull);
    }
}