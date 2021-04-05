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
  * Significantly reduce network traffic between compute and storage layers
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
The software components of Caerus UDF support are listed as follows:
1.	**Caerus NDP Service**: a storage-side HTTP service that can accept and process common storage requests by complying standard protocols (e.g. AWS S3 storage protocol), the major difference of this service comparing with other similar service is that we have the ability to process UDF request as part of storage requests for direct invocation of UDFs.  
1.	**Caerus UDF Service**: a storage-side REST service that allows validate and invoke UDFs with the option of using serverless or standalone containers.
1.	**Caerus UDF Registry Service**: (for standalone container option only) a storage-side REST service that provides REST APIs to manage UDF Registry which is implemented based on Redis and underlining storage.
1.	**Caerus Event Listener Service**: a storage-side REST service that listens to registered streaming sources (Redis for now, can add other sources like Kafka, RMQ etc. if needed). Upon event, it reacts and automatically invokes related UDFs upon certain storage actions.
1.	**Caerus Registry**: 
    1.	**Redis Cluster**: it is a storage-side service (dockers cluster) that plays two roles. First, it acts as a streaming source for storage events, this is the common part for both serverless and standalone options. Second, in standalone mode, it acts as a repository for UDFs (this can be migrated to Docker Hub is needed in the future).
    1.	**Docker Hub**: In serverless mode, we will use Openfaas scheme which uses Docker Hub (public and private) as UDFs repository 
1.	**Caerus Faas**: 
     1. **Caerus Faas Client**: A modified version of Openfaas client library (from a public github source) that is part of the Caerus UDF Service, allow it to send request to Openfaas framework in serverless mode. Our major contributions are adding authentication support, updating code and depend libraries (e.g. from okhttp to okhttp3 etc.).
     1. **Openfaas Server-side Framework**: A set of commands, configurations and instrcutions to set up Openfaas platform for Caerus UDF support.  
1.	**Caerus S3 CLI (with UDF support)**: A CLI built based on AWS S3 SDK that can support standard storage operations by using standard AWS S3 protocols, PUT, GET, DELETE, COPY and LIST with UDF support. The major difference of this CLI comparing with other similar product is that we have the ability to process UDF request as part of storage requests for direct invocation of UDFs.  
1.	**Caerus UDF Functions**: 
    1.	A complete **serverless UDF** example that compiles, publishes and deploys UDF as an Openfaas serverless function that combines user defined function and common boilerplate code. It will read/write to storage directly via storage client. 
    1. A complete **standalone UDF** example that compiles, publishes and deploys UDF docker that combines user defined function and common boilerplate code. It will read/write to storage directly via storage client 

 

# Getting Started

## Serverless option (Q1 item):
See detail instructions here: https://github.com/futurewei-cloud/caerus/tree/master/ndp/udf/deployment

## Standalone option (Q3 item)


1. Run step 1-8 in above (serverless option)

2. Build and run UDF example docker using GOOGLE JIB (https://cloud.google.com/blog/products/gcp/introducing-jib-build-java-docker-images-better):
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
3. run above step 10-15
