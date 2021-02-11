# Caerus User Defined Function (UDF) Support

![Caerus UDF Architecture](https://github.com/futurewei-cloud/caerus/blob/master/images/Caerus%20UDF%20Architecture%20(small).jpg)

Caerus UDF allows user to define, register, upload, and invoke user define function that can directly operate on storage side. The UDF invocation can be achieved either via explicit call or via automatic event notification.

# Major Benefits of Caerus UDF Support
* Highly portable architecture that can be easily added to any storage system or cloud storage backend without the need to change storage systems
* First to support storage-side serverless architecture option that is easy to deploy UDFs, lower cost, better scalability, and improved latency
* First to support fully automated event driven UDF invocations  
* Work with any workflows, compute platforms, and programming languages in Big Data Analytics and AI
* Have the potential for further UDF acceleration (future TODO) by taking advantage storage-side hardware (CPU, GPU, FPGA, Smart SSD etc.) and software (caching and indexing)
* Has the same customer benefits as general Near Data Processing:   
  * Significantly reduce network traffic
  * Reduce storage I/O in most of the cases
  * Speed up overall processing time
  * Mitigate the “too big to eat” problem
  * Take full advantage of storage system resources 
  * Reduce cost
  * Improve in data privacy and regulation

# Major Features of Caerus UDF Support
* Support options to run UDF as serverless (using Openfaas framework, Q1) or standalone containers (Q3)
* Support both fully automated event-driven and direct invocation of UDFs
* Support UDF invocation upon any storage operations like Get/Access, Put, Copy, and Delete
* Support Spark UDF SQL integration (Q2)
* Support any storage systems (Integration of Minio for Q1 as an example, Ceph and HDFS in Q2 and beyond)
* Ability to support any programming language implementations of UDFs

# Major Components of Caerus UDF Support

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
15. Build Caerus HTTP Service (clientService) project:
```
> cd clientService
> mvn clean package
```
16. Run Caerus HTTP Service (clientService) project:
```
> java -jar target/clientService-0.0.1-SNAPSHOT.jar
``` 
17. Explicitly call a UDF while upload/download/delete object, use Postman or curl command. 
In following example, upon uploading an image file "sample.jpg" to storage, invoke a UDF "thumbnail" to create a thumbnail image 
with input parameters of width=400 and height=600, the thumbnail image is then upload to storage (default location bucket "thumbnailsbucket"):  
```
curl --location --request POST 'localhost:8000/upload' \
--form 'bucket=b100' \
--form 'uploadFile=@/home/ubuntu/images/new/sample.jpg' \
--form 'metadata={   "name": "thumbnail",
    "inputParameters": ["400", "600"]
}'
```
18. Check storage to see the metadata tag info:
```
root@ubuntu1804:/home/ubuntu# mc stat minio/b100/sample0.jpg
Name      : sample0.jpg
Date      : 2020-11-22 11:57:44 EST 
Size      : 2.4 MiB 
ETag      : d68938beae85b03344ec3b2fc56e2ea9 
Type      : file 
Metadata  :
  Content-Type              : application/octet-stream 
  X-Amz-Meta-Inputparameters: 400,600 
  X-Amz-Meta-Udfname        : thumbnail 

root@ubuntu1804:/home/ubuntu# 
```
