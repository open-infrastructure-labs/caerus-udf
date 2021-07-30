# Caerus UDF Deployment Automation - Object Storage System Support

Please see [parent page for step 1 to 3](README.md)

## Step 4: Set up storage system
Current example is using Minio, but could change to other storage systems if needed. 

Start up a minio storage server:
```
root@ubuntu1804:/home/ubuntu/minio/minio# ./minio server /data

┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃ You are running an older version of MinIO released 1 month ago ┃
┃ Update: Run `mc admin update`                                  ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛

Endpoint:  http://192.168.6.129:9000  http://172.17.0.1:9000  http://172.18.0.1:9000  http://172.19.0.1:9000  http://172.20.0.1:9000  http://127.0.0.1:9000
AccessKey: minioadmin
SecretKey: minioadmin
```
## Step 5: Start dockers
```
root@ubuntu1804:/home/ubuntu/caerus-udf/deployment# docker-compose up -d 
```

## Step 6: Enable bucket notification to storage system
Follow the below link: https://docs.min.io/docs/minio-bucket-notification-guide.html, run commands:
```
> mc admin config set minio/ notify_redis:1 address="localhost:6379" format="namespace" key="bucketevents" password="my_password" queue_dir="/home/ubuntu/redisEvents" queue_limit="10000
> mc admin config get minio/ notify_redis
> mc mb minio/imagesbucket
> mc event add minio/imagesbucket arn:minio:sqs::1:redis --suffix .jpg
> mc event list minio/imagesbucket
  arn:minio:sqs::1:redis s3:ObjectCreated:*,s3:ObjectRemoved:* Filter: suffix=”.jpg”
```
## Step 7: Test this deployment set up: using Caerus S3 CLI for general storage operations and direct invocation of serverless function
Start the redis-cli Redis client program to inspect the contents in Redis. Run the monitor Redis command. This prints each operation performed on Redis as it occurs.
```
root@ubuntu1804:/home/ubuntu/caerus-udf/s3client# java -jar target/s3client-0.0.1-SNAPSHOT.jar put -b testbucket -f "/home/ubuntu/images/new/sample3.jpg" -k sample3.jpg -u caerus-faas-spring-thumbnail -i "400, 600"
Uploading a new object to S3 from a file

root@ubuntu1804:/home/ubuntu/caerus-udf/s3client# java -jar target/s3client-0.0.1-SNAPSHOT.jar list -b thumbnailsbucket
...
* sample_thumbnail.png
    Last Modified: Mon Feb 15 10:48:03 EST 2021
    Size:          97042
    Etag:          b012df12a3e771ae9af1f4e0c40f88bc
    Storage Class: STANDARD
```

## Step 8: Test this deployment set up: automated event-driven invocation of serverless function
Copy an image file from local to storage, then check newly created thumbnail by serverless function:
```
root@ubuntu1804:/home/ubuntu/caerus-udf/eventListenerService# mc cp /home/ubuntu/images/new/sample.jpg minio/imagesbucket/
/home/ubuntu/images/new/sample.jpg:  2.44 MiB / 2.44 MiB ┃▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┃ 64.30 MiB/s 0s
root@ubuntu1804:/home/ubuntu/caerus-udf/s3client# java -jar target/s3client-0.0.1-SNAPSHOT.jar list -b thumbnailsbucket
...
* sample_thumbnail.png
    Last Modified: Mon Feb 15 10:55:12 EST 2021
    Size:          97042
    Etag:          b012df12a3e771ae9af1f4e0c40f88bc
    Storage Class: STANDARD
```
# Troubleshooting
All Caerus UDF related services have log files created in the backend container:
```
root@ubuntu1804:/home/ubuntu/caerus-udf/deployment# docker exec -it deployment_caerus-ndp-udf-backend_1 bash
root@ubuntu1804:/home/jars# ls -la /tmp/
..
-rw------- 1 root root    0 Mar 26 12:33 eventListenerService-stderr---supervisor-V3xute.log
-rw------- 1 root root 6181 Mar 26 12:33 eventListenerService-stdout---supervisor-bhsttH.log
-rw------- 1 root root    0 Mar 26 12:33 ndpService-stderr---supervisor-29rInA.log
-rw------- 1 root root 2773 Mar 26 12:33 ndpService-stdout---supervisor-BY39L3.log
-rw------- 1 root root    0 Mar 26 12:33 registryService-stderr---supervisor-abevyV.log
-rw------- 1 root root 3204 Mar 26 12:33 registryService-stdout---supervisor-oo4GN2.log
-rw------- 1 root root    0 Mar 26 12:33 udfService-stderr---supervisor-X0lgO4.log
-rw------- 1 root root 2140 Mar 26 12:33 udfService-stdout---supervisor-tUlIbG.log
...
root@ubuntu1804:/home/jars# 
```
