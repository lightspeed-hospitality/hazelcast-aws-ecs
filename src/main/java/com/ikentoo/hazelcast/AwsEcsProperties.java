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

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.hazelcast.config.properties.*;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.hazelcast.config.properties.PropertyTypeConverter.STRING;

@SuppressWarnings("raw")
public enum AwsEcsProperties {

    cluster(false, STRING, null),
    service(false, STRING, null),
    ports(true, STRING, value -> new PortRange((String) value)),
    container_name_regexp(true, STRING, null),
    access_key(true, STRING, null),
    secret_key(true, STRING, null),
    region(true, STRING, null);

    public static final Collection<PropertyDefinition> PROPERTY_DEFINITIONS =
            Collections.unmodifiableList(
                    Arrays.stream(AwsEcsProperties.values())
                            .map(e -> e.propertyDefinition)
                            .collect(Collectors.toList()));

    private final SimplePropertyDefinition propertyDefinition;

    String key() {
        return this.name().replaceAll("_", "-");
    }

    AwsEcsProperties(boolean optional, PropertyTypeConverter typeConverter, ValueValidator validator) {
        this.propertyDefinition = new SimplePropertyDefinition(this.key(), optional, typeConverter, validator);
    }

    public static Config fromProps(Map<String, Comparable> props) {
        return new Config(props);
    }

    public static class PortRange {
        private final Supplier<IntStream> ports;

        PortRange(String portRange) {
            if (!portRange.matches("\\d+(-\\d+)?")) {
                throw new ValidationException("portRange invalid: " + portRange);
            }
            String[] portRanges = portRange.split("-");
            int lower = Integer.valueOf(portRanges[0]);
            int upper = portRanges.length == 2 ? Integer.valueOf(portRanges[1]) : lower;
            validatePort(lower);
            validatePort(upper);
            this.ports = () -> IntStream.rangeClosed(lower, upper);
        }

        private void validatePort(int port) {
            if (!(port > 0 && port < 65536)) {
                throw new ValidationException("port is not valid " + port);
            }
        }

        IntStream ports() {
            return ports.get();
        }
    }


    public static class Config {
        private final Map<String, Comparable> properties;

        Config(Map<String, Comparable> properties) {
            this.properties = properties;
        }

        String getClusterName() {
            return (String) properties.get(cluster.key());
        }

        String getServiceName() {
            return (String) properties.get(service.key());
        }

        IntStream getPorts() {
            String portSpec = (String) properties.get(ports.key());
            if (portSpec == null) {
                return IntStream.of(5701);
            }
            return new PortRange(portSpec).ports();
        }

        public Optional<AWSCredentialsProvider> getAwsCredentials() {
            AWSCredentialsProvider provider = null;
            String awsKey = (String) properties.get(access_key.key());
            String secretKey = (String) properties.get(secret_key.key());
            if (awsKey != null && secretKey != null) {
                provider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsKey, secretKey));
            }
            return Optional.ofNullable(provider);
        }

        public Optional<String> getAwsRegion() {
            return Optional.ofNullable((String) properties.get(region.key()));
        }

        public String getContainerNameFilter() {
            String containerFilter = (String) properties.get(container_name_regexp.key());
            if (containerFilter==null || containerFilter.trim().isEmpty()) return ".*";
            return containerFilter.trim();
        }
    }
}
