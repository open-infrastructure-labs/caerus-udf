# Caerus UDF Support

Caerus UDF allows user to define, register, upload, and invoke user define function that can directly operate on storage. The UDF invocation can be achieved either via explicit call or via automatic event notification.

There are three portions in this support:
* UDF Service: allows dynamic registration of event notification, and provides REST APIs to allow explict invoking UDF.   
* UDF Registry Service: provides REST APIs to manage UDF Registry which is implemented based on Redis and underlining stroage
* UDF Image (example): a docker service (will more to serverless next) that compile and deploy UDF docker that combines user defined function and common boilerplate code. It will read/write to storage directly via storage client (e.g. minion client)  

# Getting Started
1. Start Redis docker cluster:
```
> cd bitnami-docker-redis/
> docker-compose -f docker-compose-replicaset.yml up -d
``` 
2. Build UDF Registry Service project:
```
> cd registry
> mvn clean package
```
3. Run UDF Registry Service:
```
> java -jar target/Udf-Registry-0.0.1-SNAPSHOT.jar
``` 
4. Build UDF Service project:
```
> cd service
> mvn clean package
```
5. Run UDF Service:
```
> java -jar target/UdfService-0.0.1-SNAPSHOT.jar
```
6. Build UDF example (java for now, other language in the future) project:
```
> cd examples/java/thumbnail
> mvn clean package
```
7. Install docker registry: this is needed by UDF docker generation (local for now, can be external docker registry later):
https://www.digitalocean.com/community/tutorials/how-to-set-up-a-private-docker-registry-on-ubuntu-18-04

8. Build and run UDF example docker using GOOGLE JIB (https://cloud.google.com/blog/products/gcp/introducing-jib-build-java-docker-images-better):
```
> mvn jib:dockerBuild
> [INFO] Scanning for projects...
  [INFO] 
  [INFO] -----------------------< com.example:thumbnail >------------------------
  [INFO] Building thumbnail 0.0.1-SNAPSHOT
  [INFO] --------------------------------[ jar ]---------------------------------
  [INFO] 
  [INFO] --- jib-maven-plugin:2.1.0:dockerBuild (default-cli) @ thumbnail ---
  [INFO] 
  [INFO] Containerizing application to Docker daemon as localhost:5000/thumbnail:0.0.1-SNAPSHOT...
  [WARNING] Base image 'openjdk:8u171-alpine' does not use a specific image digest - build may not be reproducible
  [INFO] The base image requires auth. Trying again for openjdk:8u171-alpine...
  [INFO] Using base image with digest: sha256:e10cd2553b2a7247b9bf03543e33c4daed53ee508751930794a4a560b622e20a
  [INFO] 
  [INFO] Container entrypoint set to [java, -Xms512m, -Xmx512m, -cp, /app/resources:/app/classes:/app/libs/*, com.example.thumbnail.ThumbnailApplication]
  [INFO] 
  [INFO] Built image to Docker daemon as localhost:5000/thumbnail:0.0.1-SNAPSHOT
  [INFO] Executing tasks:
  [INFO] [==============================] 100.0% complete
  [INFO] 
  [INFO] ------------------------------------------------------------------------
  [INFO] BUILD SUCCESS
  [INFO] ------------------------------------------------------------------------
  [INFO] Total time:  12.590 s
  [INFO] Finished at: 2020-11-10T16:12:34-05:00
  [INFO] ------------------------------------------------------------------------
  root@ubuntu1804:/home/ubuntu/caerus/caerus/ndp/udf/examples/java/thumbnail# 
> docker run -d --network host  localhost:5000/thumbnail:0.0.1-SNAPSHOT
```
9. (steps 9-10 are related to storage setup, current example is using Minio, but could change to other storage systems if needed)
Start up a minio server:
```
root@ubuntu1804:/home/ubuntu/minio/minio# ./minio server /data

┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃ You are running an older version of MinIO released 1 month ago ┃
┃ Update: Run `mc admin update`                                  ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛

Endpoint:  http://192.168.6.129:9000  http://172.17.0.1:9000  http://172.18.0.1:9000  http://172.19.0.1:9000  http://172.20.0.1:9000  http://127.0.0.1:9000          
AccessKey: minioadmin 
SecretKey: minioadmin 
SQS ARNs:  arn:minio:sqs::1:redis
....
```
10. Follow the below link: https://docs.min.io/docs/minio-bucket-notification-guide.html, run commands:
```
> mc admin config set minio/ notify_redis:1 address="172.18.0.2:6379" format="namespace" key="bucketevents" password="my_password" queue_dir="/home/ubuntu/redisEvents" queue_limit="10000
> mc admin config get minio/ notify_redis
> mc mb minio/imagesbucket
> mc event add minio/imagesbucket arn:minio:sqs::1:redis --suffix .jpg
> mc event list minio/imagesbucket
  arn:minio:sqs::1:redis s3:ObjectCreated:*,s3:ObjectRemoved:* Filter: suffix=”.jpg”
``` 
11. Set up redis notification by following this: https://redis.io/topics/notifications (note this step can be saved if we don't want to change config during runtime, we can just set in the redis docker config file.)
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
12. Copy an image file from local to storage (minio in this case) 
```
mc cp /home/ubuntu/images/new/sample.jpg minio/imagesbucket/
/home/ubuntu/images/new/sample.jpg:  2.44 MiB / 2.44 MiB ▓▓▓▓┃ 76.35 MiB/s 0sroot@ubuntu1804:/home/ubuntu/caerus/caerus/ndp/udf/examples/java/thumbnail#  
``` 

13. Check storage, the thumbnail file will be created in a bucket called thumbnailsbucket 
```
root@ubuntu1804:/home/ubuntu/caerus/caerus/ndp/udf/examples/java/thumbnail# mc ls minio/thumbnailsbucket
[2020-11-10 13:53:53 EST]  6.2KiB sample_thumbnail.png
root@ubuntu1804:/home/ubuntu/caerus/caerus/ndp/udf/examples/java/thumbnail# 
``` 

14. Can also view web portal of storage using a web browser 
```
http://localhost:9000/minio/thumbnailsbucket/
``` 
