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


import com.hazelcast.logging.Slf4jFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.Assert.assertEquals;

public class AwsEcsDiscoveryStrategyTest {
    @Rule
    public final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();

    @Test
    public void getOwnTaskArnFromFile() throws IOException {
        Path tempFile = makeTaskFile();
        environmentVariables.set("ECS_CONTAINER_METADATA_FILE", tempFile.toAbsolutePath().toString());
        String arn = AwsEcsDiscoveryStrategy.getOwnTaskArn(new Slf4jFactory().getLogger(""));
        assertEquals("arn:aws:ecs:us-west-2:012345678910:task/d90675f8-1a98-444b-805b-3d9cabb6fcd4", arn);
    }

    @Test
    public void getOwnTaskArnFromURI() throws IOException {
        Path tempFile = makeTaskFile();
        Path parent = tempFile.getParent();
        Files.copy(tempFile, parent.resolve("task"), REPLACE_EXISTING);

        environmentVariables.set("ECS_CONTAINER_METADATA_URI", parent.toAbsolutePath().toUri().toString());
        String arn = AwsEcsDiscoveryStrategy.getOwnTaskArn(new Slf4jFactory().getLogger(""));
        assertEquals("arn:aws:ecs:us-west-2:012345678910:task/d90675f8-1a98-444b-805b-3d9cabb6fcd4", arn);
    }


    private Path makeTaskFile() throws IOException {

        String example = "{\n" +
                "    \"Cluster\": \"default\",\n" +
                "    \"ContainerInstanceARN\": \"arn:aws:ecs:us-west-2:012345678910:container-instance/1f73d099-b914-411c-a9ff-81633b7741dd\",\n" +
                "    \"TaskARN\": \"arn:aws:ecs:us-west-2:012345678910:task/d90675f8-1a98-444b-805b-3d9cabb6fcd4\",\n" +
                "    \"ContainerName\": \"metadata\"\n" +
                "}";

        Path tempFile = Files.createTempFile("meta", "json");
        Files.write(tempFile, example.getBytes(StandardCharsets.UTF_8));
        return tempFile;
    }
}
