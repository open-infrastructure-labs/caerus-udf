# Caerus UDF Event Notification Service

Caerus Event Listener Service is a storage-side REST service that listens to registered streaming sources (Redis for now, can add other sources like Kafka, RMQ etc. if needed). Upon event, it reacts and automatically invokes related UDFs upon certain storage actions.


In modern storage systems and cloud storage backend, many of them started to implement a feature called “bucket notification”, vendors and open sources like Amazon S3, GCP Storage (Google Cloud Storage), IBM Cloud Object Storage, Ceph, MinIO etc. all have this support now. This notification feature enables you to receive notifications when certain events (such as PUT, GET, COPY, DELTE etc.) happen in your bucket. Currently people use this feature majorly for alerting etc. lightweight use cases, with Caerus storage-side UDF pushdown, now we can support use cases even they are very data intensive in an fully automatic fashion.


Related to “bucket notification”, there are two characteristics:
  1.	It is currently only supported in object storage systems/cloud object storage, we haven’t seen it in file system (like HDFS) or block systems, but this doesn’t say that we can’t add such support to file/block storage, in matter of fact, people do ask such feature and have some experimental implementation (for example, people hook up HDFS with Apache Nifi and event target like Redis/Kafka to support HDFS version of this feature). We can integrate this feature into HDFS as needed.
  2.	When people use bucket notification feature, they normally take advantage the rich feature of “bucket” configurations (normally it is called bucket policy, see examples of Amazon S3 bucket policy) to create specialized bucket. For example, a bucket normally only contains certain type of object types (images file only), or a bucket only belongs to certain group/user/organization etc. 
      

Amazon S3 bucket policy examples:
  1.	https://aws.amazon.com/premiumsupport/knowledge-center/s3-allow-certain-file-types/
  2.	https://docs.aws.amazon.com/AmazonS3/latest/userguide/example-bucket-policies.html
  
  
Upon registered event happening, the storage system automatically sends triggered events to the configured notification targets. Storage system can support notification targets like AMQP, Redis, Kafka, ElasticSearch, NATS, Webhooks, NSQ, MQTT, MySQL and PostgreSQL. In Caerus project, we use MinIO Bucket Notifications and Redis as notification target as an example for our first storage system integration example, more storage system integration and different notification targets can be added as needed.

# Getting Started
The steps below show how to use this bucket notification:

## Step 1: Start notification target, such as Redis and Caerus Event Notification Service
```
> cd $CAERUS_HOME/bitnami-docker-redis/
> docker-compose -f docker-compose-replicaset.yml up -d
> mvn clean package
> java -jar target/EventListenerService-0.0.1-SNAPSHOT.jar
```
## Step 2: Add nortification target to storage system
Follow the below link: https://docs.min.io/docs/minio-bucket-notification-guide.html, run commands:
```
> mc admin config set minio/ notify_redis:1 address="172.18.0.2:6379" format="namespace" key="bucketevents" password="my_password" queue_dir="/home/ubuntu/redisEvents" queue_limit="10000
> mc admin config get minio/ notify_redis
> mc mb minio/imagesbucket
> mc event add minio/imagesbucket arn:minio:sqs::1:redis --suffix .jpg
> mc event list minio/imagesbucket
  arn:minio:sqs::1:redis s3:ObjectCreated:*,s3:ObjectRemoved:* Filter: suffix=”.jpg”
```
## Step 3: Enable bucket notification to storage system
Set up redis notification by following this: https://redis.io/topics/notifications (note this step can be saved if we don't want to change config during runtime, we can just set in the redis docker config file.)
```
root@ubuntu1804:/home/ubuntu# redis-cli -h 172.18.0.2 -a my_password
Warning: Using a password with '-a' or '-u' option on the command line interface may not be safe.
172.18.0.2:6379> config set "notify-keysapce-events" "EKsglh"
(error) ERR Unsupported CONFIG parameter: notify-keysapce-events
172.18.0.2:6379> config set "notify-keyspace-events" "EKsglh"
OK
172.18.0.2:6379> config get "notify-keyspace-events"
1) "notify-keyspace-events"
2) "glshKE"
172.18.0.2:6379>
```
## Step 4: Test on notification target
Start the redis-cli Redis client program to inspect the contents in Redis. Run the monitor Redis command. This prints each operation performed on Redis as it occurs.
```
redis-cli -a yoursecret
127.0.0.1:6379> monitor
OK
```
Set up serverless function framework: see details in $CAERUS_UDF_HOME/faas
Copy an image file from local to storage (MinIO in this case)
```
mc cp /home/ubuntu/images/new/sample.jpg minio/imagesbucket/
/home/ubuntu/images/new/sample.jpg:  2.44 MiB / 2.44 MiB ▓▓▓▓┃ 76.35 MiB/s 0sroot@ubuntu1804:/home/ubuntu/caerus-udf/examples/java/thumbnail#
```
Check storage, the thumbnail file will be created in a bucket called thumbnailsbucket
```
root@ubuntu1804:/home/ubuntu/caerus-udf/examples/java/thumbnail# mc ls minio/thumbnailsbucket
[2020-11-10 13:53:53 EST]  6.2KiB sample_thumbnail.png
root@ubuntu1804:/home/ubuntu/caerus-udf/examples/java/thumbnail#
```


