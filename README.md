# hazelcast-aws-ecs
Discovery strategy for AWS ECS.

This discovery strategy uses the AWS ECS API to enumerate address of containers in tasks for a given service and cluster.

Currently the task definition name cannot be filtered. You can however filter on the container name inside the task
by setting a regexp in `container-name-regexp`.

This service should work in both EC2 and fargate based clusters.
 
You will need to expose the hazelcast port in your docker file (`EXPOSE 5701`) 

Usually the credentials should be auto configured but can be overridden if needed.


```xml

<?xml version="1.0" encoding="UTF-8"?>
<hazelcast xmlns="http://www.hazelcast.com/schema/config"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://www.hazelcast.com/schema/config hazelcast-config-3.11.xsd">

    <import resource="hazelcast-common.xml" />      

    <properties>
        <property name="hazelcast.discovery.enabled">true</property>
    </properties>

    <network>
        <join>
            <multicast enabled="false"/>
            <aws enabled="false" />
            <tcp-ip enabled="false" />
            <discovery-strategies>
                <discovery-strategy enabled="true" class="com.ikentoo.hazelcast.AwsEcsDiscoveryStrategy">
                    <properties>
                        <property name="cluster">ikentoo-services-trial</property>
                        <property name="service">ik-waitlist-trial</property>
                        <!-- below are optional --> 
                        <property name="ports">5701-5702</property>
                        <property name="container-name-regexp">.*</property>
                        <property name="access_key">somekey</property>
                        <property name="secret_key">somekey</property>
                        <property name="region">us-east-1</property>                       
                    </properties>
                </discovery-strategy>
            </discovery-strategies>
        </join>
    </network>
</hazelcast>


```
