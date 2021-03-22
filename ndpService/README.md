# Caerus NDP Service

Caerus NDP Service is a storage-side HTTP service that can accept and process common storage requests by complying standard protocols (e.g. AWS S3 storage protocol), 
the major difference of this service comparing with other similar service is that we have the ability to process UDF request as part of storage requests for direct invocation
of UDFs.

# Getting Started
1. Build project:
```
> mvn clean package
```
2. Run project:
```
> java -jar target/ndpService-0.0.1-SNAPSHOT.jar
```
3. Test project:

Use Caerus S3 client (see an example command below)
```
put -b testbucket -f "/home/ubuntu/images/new/sample3.jpg" --keyName sample3.jpg --udfName caerus-faas-spring-thumbnail --udfInputParameters "400, 600"
```
