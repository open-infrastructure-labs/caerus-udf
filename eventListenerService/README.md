# Caerus UDF Registry Server
Caerus UDF Registry Server is a HTTP server that serves requests to store/create, retrieve, modify and delete UDF configurations and its app code (e.g. jar file). It uses Redis (mount on any storage) as backend.  

There are three portions in this project:
* Rest API portion: defines APIs for common actions such as POST/GET/Delete/PUT etc., it uses Swagger editor etc. to define, test and generate code (see notes) 
* Redis configuration: contains Redis docker configurations for Redis cluster setup
* UDF Registry Server: serves micro-service type requests, based on Spring Boot framework, and code for Redis operations  

Notes: For Swagger code generation, currently it has limitations on Spring Boot server side, for example, it restricts to Java 7 and older version of Spring Boot, significant changes and tests need to be added before we can use it. Set this as TODO list for future development. We are currently using tools like SpringInitialzr (https://start.spring.io/) to generate server code skeleton and maven pom file.

# Getting Started
1. Start Redis docker cluster:
```
> cd bitnami-docker-redis/
> docker-compose -f docker-compose-replicaset.yml up -d
``` 
2. Build server project:
```
> mvn clean package
```
3. Run server project:
```
> java -jar target/Udf-Registry-0.0.1-SNAPSHOT.jar
``` 
4. Run tools like postman or curl to submit request:
```
> curl --location --request GET 'http://localhost:8080/udf/'
> [{"id":"f5b53821-1fe4-46da-8a94-d2be18411bfe","name":"udf1","pkg":0,"language":"Java","interfaceVersion":1.0,"languageVersion":1.8,"main":"main_class_name","fileName":"Udf1.jar"}]
``` 
5. confirm with Redis client tools like RDM GUI (https://snapcraft.io/redis-desktop-manager) or Redis cli command to see if the records returned are correct

# API Documentation - Via Swagger
* To see and try out the APIs provided by this service, simply type in following in a browser (localhost can be replaced by the IP address of the server):
``` 
http://localhost:8080/swagger-ui.html  
``` 
* To see the APIs document in a Json format, simply issue a GET request via curl or tools like postman:
``` 
curl --location --request GET 'http://localhost:8080/v2/api-docs' 
``` 
```
{"swagger":"2.0","info":{"description":"Rest APIs for managing UDF registry backed by redis","version":"1.0","title":"Caerus UDF Registry APIs","termsOfService":"Free to use","contact":{"name":"Yong Wang","url":"https://openinfralabs.org/","email":"yong.wang@futurewei.com"},"license":{"name":"Apache License 2.0","url":"https://openinfralabs.org/"}},"host":"localhost:8080","basePath":"/","tags":[{"name":"udf-controller","description":"Udf Controller"}],"paths":{"/api/udf":{"get":{"tags":["udf-controller"],"summary":"Fetch all UDfs' configuration information from the registry.","description":"Fetch all Udfs configuration in json format.","operationId":"fetchAllUdfUsingGET","produces":["*/*"],"responses":{"200":.....
```
