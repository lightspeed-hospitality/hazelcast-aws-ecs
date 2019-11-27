# hazelcast-aws-ecs
Discovery strategy for AWS ECS.

This discovery strategy uses the AWS ECS API to enumerate address of containers in tasks for a given service and cluster.

Cluster and Services names can be filtered using `cluster-name-regexp` as well as `service-name-regexp` as you might 
want to deploy to different environments (dev, stage, prod).

If you specify `cluster-name` `cluster-name-regexp` is ignored, the same goes for `service-name` and `service-name-regexp`.  

Currently the task definition name cannot be filtered. You can however filter on the container name inside the task
by setting a regexp in `container-name-regexp`.

You should create an ECS task role and attach the permissions `ecs:ListTasks` and `ecs:DescribeTasks` to it through a
policy. If you want to use `cluster-name-regexp` and `service-name-regexp` the permissions `ecs:ListClusters` and 
`ecs:ListServices` are also required otherwise they are optional. Example:

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
                        "ecs:DescribeTasks",
                        "ecs:ListClusters",
                        "ecs:ListServices"
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
                        <property name="cluster-name-regexp">.*ikentoo-dev-cluster.*</property>
                        <property name="service-name-regexp">.*ikentoo-dev-service.*</property>
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
## Property configuration
```java
     
    /**
    * Setup Properties
    */
    Map<String, Comparable> properties = new HashMap<>();
    properties.put("cluster", "ikentoo-services-trial");
    properties.put("service", "ik-waitlist-trial");

    /**
    * Alternatively or in Combination with regexp for cluster / service name,
    * cluster / service properties if available will overwrite the regexps
    */
    properties.put("cluster-name-regexp", ".*-dev-.*");
    properties.put("service-name-regexp", ".*-backend-.*-dev$");
    
    properties.put("ports", "5701-5702");
    properties.put("container-name-regexp", ".*");
    properties.put("fail-fast", "true");
    
    properties.put("region", "us-east-1");
    properties.put("access-key", "<somekey>");
    properties.put("secret-key", "<somekey>");

    /**
    * Hazelcast Configuration
    */ 
    DiscoveryStrategyConfig discoveryStrategyConfig = new DiscoveryStrategyConfig(awsEcsDiscoveryStrategyFactory, properties);
    joinConfig.getDiscoveryConfig().addDiscoveryStrategyConfig(discoveryStrategyConfig);

``` 

## Integration / Behavior Testing

Please use provided integration test `AwsEcsDiscoveryStrategyIT` to test behavior of `discoveryNodes()` and resulting 
clusters and services names based on set parameters. You need to provide valid `AWS_ACCESS_KEY`, `AWS_SECRET_ACCESS_KEY` and `AWS_DEFAULT_REGION`
as environment variables as well as `CLUSTER_NAME`, `CLUSTER_NAME_REGEXP`, `SERVICE_NAME` and `SERVICE_NAME_REGEXP` for filtering.

To execute the test simply use the following command line. 
```bash 
export AWS_SECRET_ACCESS_KEY=rmxxxxx
export AWS_ACCESS_KEY=AKIxxxx
export AWS_DEFAULT_REGION=us-east-1

export CLUSTER_NAME=testing-xxxxx-cluster-ecs-stack-us-east-1-dev-EcsCluster-32XXXXXXXX
export CLUSTER_NAME_REGEXP=.*-dev-.*
export SERVICE_NAME=service/xxxxx-testing-backend-delivery-system-dev
export SERVICE_NAME_REGEXP=.*-backend-.*-dev$

mvn clean test -Dtest=AwsEcsDiscoveryStrategyIT


[main] DEBUG simple - SYSTEM_ENV={PATH=/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:/opt/X11/bin:/Library/Frameworks/Mono.framework/Versions/Current/Commands:/Applications/Wireshark.app/Contents/MacOS, ... }
[main] DEBUG simple - AWS_META={     "Cluster": "default",     "ContainerInstanceARN": ... }
[main] DEBUG simple - TaskARN=arn:aws:ecs:us-west-2:012345678910:task/d90675f8-1a98-444b-805b-3d9cabb6fcd4
[main] DEBUG simple - Discovering nodes in AWS ECS {cluster=testing-xxxx-cluster-ecs-stack-us-east-1-dev-EcsCluster-32XXXXXXXX, secret-key=rm..., service-name-regexp=.*-backend-.*-dev$, access-key=AK..., region=us-east-1}
[main] DEBUG simple - Using Cluster Name Regexp 'testing-xxxxx-cluster-ecs-stack-us-east-1-dev-EcsCluster-32XXXXXXXX'
[main] DEBUG simple - Using Service Name Regexp '.*-backend-.*-dev$'
[main] DEBUG simple - Found Cluster 'arn:aws:ecs:us-east-1:686xxxxxxxxx:cluster/testing-xxxx-cluster-ecs-stack-us-east-1-dev-EcsCluster-32XXXXXXXX'
[main] DEBUG simple - Found Service 'arn:aws:ecs:us-east-1:686xxxxxxxxx:service/xxxx-testing-backend-kv-dev'
[main] DEBUG simple - Found Service 'arn:aws:ecs:us-east-1:686xxxxxxxxx:service/xxxx-testing-backend-api-settings-cache-dev'
[main] DEBUG simple - Found Service 'arn:aws:ecs:us-east-1:686xxxxxxxxx:service/xxxx-testing-backend-delivery-system-dev'
[main] DEBUG simple - 3 Tasks Found : [arn:aws:ecs:us-east-1:686xxxxxxxxx:task/0376050b-d3f6-4f5d-9cc5-c3d9e0c13ef9, arn:aws:ecs:us-east-1:686xxxxxxxxx:task/f7d7982e-4734-4929-bc54-e0dc6f3f4eb5, ...]
[main] DEBUG simple - 2 Addresses Found : [[10.192.21.126]:5701, [10.192.20.28]:5701]
[main] INFO simple - Private / Public Address Found : [10.192.20.28]:5701 / [10.192.20.28]:5701
[main] INFO simple - Private / Public Address Found : [10.192.21.126]:5701 / [10.192.21.126]:5701
``