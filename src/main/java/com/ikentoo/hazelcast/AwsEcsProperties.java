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

import static com.hazelcast.config.properties.PropertyTypeConverter.BOOLEAN;
import static com.hazelcast.config.properties.PropertyTypeConverter.STRING;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.hazelcast.config.properties.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("raw")
public enum AwsEcsProperties {
    cluster(true, STRING, null),
    cluster_name_regexp(true, STRING, null),
    service(true, STRING, null),
    service_name_regexp(true, STRING, null),

    ports(true, STRING, value -> new PortRange((String) value)),
    container_name_regexp(true, STRING, null),
    access_key(true, STRING, null),
    secret_key(true, STRING, null),
    fail_fast(true, BOOLEAN, null),
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

    AwsEcsProperties(
            boolean optional, PropertyTypeConverter typeConverter, ValueValidator validator) {
        this.propertyDefinition =
                new SimplePropertyDefinition(this.key(), optional, typeConverter, validator);
    }

    public static Config fromProps(Map<String, Comparable> props) {
        return new Config(props);
    }

    public static class PortRange {
        private final int lower;
        private final int upper;

        PortRange(String portRange) {
            if (!portRange.matches("\\d+(-\\d+)?")) {
                throw new ValidationException("portRange invalid: " + portRange);
            }
            String[] portRanges = portRange.split("-");
            this.lower = Integer.valueOf(portRanges[0]);
            this.upper = portRanges.length == 2 ? Integer.valueOf(portRanges[1]) : lower;
            validatePort(lower);
            validatePort(upper);
        }

        private void validatePort(int port) {
            if (!(port > 0 && port < 65536)) {
                throw new ValidationException("port is not valid " + port);
            }
        }

        IntStream ports() {
            return IntStream.rangeClosed(lower, upper);
        }

        @Override
        public String toString() {
            return format("%d-%d", lower, upper);
        }
    }

    public static class Config {
        private final Map<String, Comparable> properties;
        private final Pattern clusterNamePattern;
        private final Pattern serviceNamePattern;

        Config(Map<String, Comparable> properties) {
            this.properties = properties;
            this.clusterNamePattern = initPattern(cluster_name_regexp, getClusterName());
            this.serviceNamePattern = initPattern(service_name_regexp, getServiceName());
        }

        String getClusterName() {
            return (String) properties.get(cluster.key());
        }

        String getServiceName() {
            return (String) properties.get(service.key());
        }

        Pattern getClusterNameRegexp() {
            return clusterNamePattern;
        }

        Pattern getServiceNameRegexp() {
            return serviceNamePattern;
        }

        private Pattern initPattern(AwsEcsProperties prop, String exactFallBack) {
            String fallback =
                    "^" + ofNullable(exactFallBack).map(Pattern::quote).orElse(".*") + "$";

            return Pattern.compile((String) properties.getOrDefault(prop.key(), fallback));
        }

        IntStream getPorts() {
            String portSpec = (String) properties.get(ports.key());
            if (portSpec == null) {
                return IntStream.of(5701);
            }
            return new PortRange(portSpec).ports();
        }

        public Optional<AWSCredentialsProvider> getAwsCredentialsProvider() {
            AWSCredentialsProvider provider = null;
            String awsKey = (String) properties.get(access_key.key());
            String secretKey = (String) properties.get(secret_key.key());
            if (awsKey != null && secretKey != null) {
                provider =
                        new AWSStaticCredentialsProvider(
                                new BasicAWSCredentials(awsKey, secretKey));
            }
            return ofNullable(provider);
        }

        public Optional<String> getAwsRegion() {
            return ofNullable((String) properties.get(region.key()));
        }

        public String getContainerNameFilter() {
            String containerFilter = (String) properties.get(container_name_regexp.key());
            if (containerFilter == null || containerFilter.trim().isEmpty()) return ".*";
            return containerFilter.trim();
        }

        @Override
        public String toString() {
            Map<String, Comparable> copy = new LinkedHashMap<>(properties);
            hide(copy, access_key);
            hide(copy, secret_key);
            return copy.toString();
        }

        private void hide(Map<String, Comparable> props, AwsEcsProperties prop) {
            String secret = (String) props.get(prop.key());
            if (secret == null || secret.isEmpty()) {
                return;
            }
            props.put(prop.key(), secret.substring(0, Math.min(secret.length(), 2)) + "...");
        }

        public boolean isFailFast() {
            Boolean failFast = (Boolean) properties.get(fail_fast.key());
            return failFast == null || failFast;
        }
    }
}
