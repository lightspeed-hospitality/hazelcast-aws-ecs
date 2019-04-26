# hazelcast-aws-ecs
Discovery strategy for AWS ECS.

This discovery strategy uses the AWS ECS API to enumerate address of containers in tasks for a given service and cluster.

Currently the task definition name cannot be filtered. You can however filter on the container name inside the task
by setting a regexp in `container-name-regexp`.

You should create an ECS task role and attach the permissions `ecs:ListTasks` and `ecs:DescribeTasks` to it through a
policy. Example:

```bash
$ aws iam list-attached-role-policies  --role-name some-ecs-task-role
{
    "AttachedPolicies": [
        {
            "PolicyName": "EcsTaskRole-allow-ecs-read-and-list",
            "PolicyArn": "arn:aws:iam::...:policy/EcsTaskRole-allow-ecs-read-and-list"
        }
        ...
    ]
}
$ aws iam get-policy-version --policy-arn arn:aws:iam::...:policy/EcsTaskRole-allow-ecs-read-and-list --version-id v2
{
    "PolicyVersion": {
        "CreateDate": "2019-04-24T07:13:35Z",
        "VersionId": "v2",
        "Document": {
            "Version": "2012-10-17",
            "Statement": [
                {
                    "Action": [
                        "ecs:ListTasks",
                        "ecs:DescribeTasks"
                    ],
                    "Resource": "*",
                    "Effect": "Allow",
                    "Sid": "VisualEditor0"
                }
            ]
        },
        "IsDefaultVersion": true
    }
}
```

This service should work in both EC2 and Fargate based clusters.
 
You will need to expose the hazelcast port in your docker file (`EXPOSE 5701`) .

The task definition must contain a port mapping to port 5701.

The ECS-service network security group must contain itself as a source in a TCP inbound rule to the port 5701, such
that cluster members can connect to each other on port 5701.

In your hazelcast config it is important to specify the interface that is visible between cluster members,
since ECS containers have internal interfaces which will otherwise potentially be picked up as public address.

Usually the AWS credentials are not necessary if you create a task role with the right permission, but can
be overriden if required.


## Xml configuration
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
        <interfaces enabled="true">
            <!-- this most likely corresponds to your vpc range -->
            <interface>10.10.*.*</interface>
        </interfaces>
        <join>
            <multicast enabled="false"/>
            <aws enabled="false" />
            <tcp-ip enabled="false" />
            <discovery-strategies>
                <discovery-strategy enabled="true" class="com.ikentoo.hazelcast.AwsEcsDiscoveryStrategy">
                    <properties>
                        <property name="cluster">ikentoo-services-trial</property>
                        <property name="service">ik-waitlist-trial</property>
                        <!-- below are optional, prefer using a ecs task role with right policy/permission--> 
                        <property name="ports">5701-5702</property>
                        <property name="container-name-regexp">.*</property>
                        <property name="access-key">somekey</property>
                        <property name="secret-key">somekey</property>
                        <property name="region">us-east-1</property>
                        <property name="fail-fast">true</property>                       
                    </properties>
                </discovery-strategy>
            </discovery-strategies>
        </join>
    </network>
</hazelcast>


```
