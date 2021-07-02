# Caerus Serverless UDF Auto Invocation via HDFS Event Notification 

Most of the Object Storage systems (such as Amazon S3, MinIO) support end-to-end event notification to allow user to monitor storage activities (read/write), and they allow to hook up many event bus targets like kafka, redis, webhooks etc. This feature allows us to support auto invocation of storage-side (NDP) serverless UDFs based on certain events.   

HDFS natively doesn't provide such features currently, however, HDFS supports iNotify/Event APIs to allow a custom program to monitor HDFS changes, and get notification. User also must implement different targets by themselves. The supported HDFS changes include append, close, create, metadata update, rename, truncate and unlink. 
See more details in: http://hadoop.apache.org/docs//r3.0.1/api/org/apache/hadoop/hdfs/inotify/Event.html

We have two options to use this HDFS feature:
1. Find an existing custom program to plug and play
2. Develop a custom program to support iNotify and add target systems (like redis, Kafka etc.)

Due to time constrain, and this part is not the core function of NDP/UDF support, it has been decided to use option #1.

Nifi is a widely used data pipeline framework that consists of many community-developed processors, two Nifi processors can be hooked up to accomplish this feature to HDFS, to be equivalent to object storage.
1. [GetHDFSEvents](https://nifi.apache.org/docs/nifi-docs/components/nifi-docs/components/org.apache.nifi/nifi-hadoop-nar/1.9.0/org.apache.nifi.processors.hadoop.inotify.GetHDFSEvents/index.html)
2. [PutDistributedMapCache](https://nifi.apache.org/docs/nifi-docs/components/org.apache.nifi/nifi-standard-nar/1.5.0/org.apache.nifi.processors.standard.PutDistributedMapCache/)

In the example, everytime a new file is uploaded (via any HDFS client) to a pre-registered HDFS folder, the Nifi will send notification to Redis, and Caerus UDF Event Listener Service will receive notification, a Caerus serverless UDF will be auto invoked, and the result is written back to another HDFS folder.
 
# Getting Started
The steps below show how to enable this feature:

## Step 1: Deploy HDFS 
Follow the below link: [HDFS](../ndpService)


## Step 2: Deploy Redis docker cluster and set event notification
- To deploy Redis, follow the below link: [registry](../registry)
- run following command using redis cli
```shell

root@ubuntu1804:/opt/hadoop-2.8.2# redis-cli -h localhost -a my_password
Warning: Using a password with '-a' or '-u' option on the command line interface may not be safe.
localhost:6379> CONFIG SET notify-keyspace-events K$
OK
localhost:6379> CONFIG get notify-keyspace-events 
1) "notify-keyspace-events"
2) "$K"
```

## Step 3: Deploy Nifi docker
Start docker compose, Nifi GUI can be access via port 8091: http://localhost:8091/nifi/:
```shell
> cd nifi
> docker-compose up -d
```

After docker is up, run following commands (these can be automated later):
```shell
root@ubuntu1804:/home/ubuntu/openinfralabs/caerus-udf/deployment/nifi# docker cp config/core-site.xml nifi2:/opt/nifi/nifi-current
root@ubuntu1804:/home/ubuntu/openinfralabs/caerus-udf/deployment/nifi# docker cp config/hdfs-site.xml nifi2:/opt/nifi/nifi-current
docker network connect docker-hadoop_default nifi2
```
## Step 4: Import prebuilt Nifi template
Using Nifi GUI (see above Step 2): 
- import and enable the template: [nifi/hdfs-events-to-redis.xml](nifi/hdfs-events-to-redis.xml)
- start both Nifi processors
- If needed, change HDFS folder to be monitored (currently set as "/input"),  and Redis connection String (currently set as 10.124.62.106:6379), credentials etc.
- When a new file is uploaded to the monitored HDFS folder, both Nifi processors should have data being transported

## Step 5: Set up OPENFAAS and deploy needed serverless functions (e.g. thumbnail function)
Follow the below link: [faas](../faas)

## Step 6 Start Caerus Event Listener Service
Follow the below link: [eventListenerService](../eventListenerService) 

## Step 7: Upload a (image) file to HDFS monitored folder
- Can use any HDFS client tools
- Simple one is to use HDFS GUI port 9870: http://localhost:9870/explorer.html#/
- After upload, check newly created thumbnail by serverless function: http://localhost:9870/explorer.html#/thumbnailsbucket
