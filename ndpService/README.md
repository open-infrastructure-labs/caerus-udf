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

3.1 Use Caerus S3 client (see an example command below)
```
put -b testbucket -f "/home/ubuntu/images/new/sample3.jpg" --keyName sample3.jpg --udfName caerus-faas-spring-thumbnail --udfInputParameters "400, 600"
```
3.2 Use Caerus HDFS client (see an example command below)
```
put -s "/home/ubuntu/images/new/sample.jpg" -t input/sample.jpg -u caerus-faas-spring-thumbnail -i "400, 600"
```


Note: 
1. If the service is running in standing alone mode outside the general docker deployment for debugging/development, the environment variable needs to be set for Minio support:
```
export MINIO_URL="http://localhost:9000"
```
2. For HDFS support, sample HTTP request to NDP Service can be:
```shell
curl --location --request GET 'http://localhost:8000/input/' \
--header 'CaerusUDF: <?xml version="1.0" encoding="UTF-8" standalone="yes"?> <caerusudf id ="1"> <function_name>caerus-faas-spring-thumbnail</function_name> <function_inputParameters>400, 600 </function_inputParameters> </caerusudf> ' \
--data-raw ''
```
3. For HDFS testing, the docker deployment of HDFS (with webhdfs enabled) can be following this site (adding more data nodes if needed)
https://github.com/big-data-europe/docker-hadoop
  - Make small changes in docker-compose.yml
    ```shell

    root@ubuntu1804:/home/ubuntu/docker-hadoop# git diff
    diff --git a/docker-compose.yml b/docker-compose.yml
    ......
    container_name: datanode
    restart: always
    +    hostname: localhost
    +    ports:
    +      - 9864:9864
      volumes:
      - hadoop_datanode:/hadoop/dfs/data
      environment:
      root@ubuntu1804:/home/ubuntu/docker-hadoop#
    ```
  - Run docker-compose
    ```shell
    docker-compose up -d
    ```
