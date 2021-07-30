# Caerus UDF Deployment Automation - HDFS System Support

Please see [parent page for step 1 to 3](README.md)

## Step 4: Start dockers
```
root@ubuntu1804:/home/ubuntu/caerus-udf/deployment# docker-compose -f docker-compose-hdfs-all-in-one.yml up -d 
```

## Step 5: Enable bucket notification to HDFS
run following command using redis cli:
```
root@ubuntu1804:/opt/hadoop-2.8.2# redis-cli -h localhost -a my_password
Warning: Using a password with '-a' or '-u' option on the command line interface may not be safe.
localhost:6379> CONFIG SET notify-keyspace-events K$
OK
localhost:6379> CONFIG get notify-keyspace-events 
1) "notify-keyspace-events"
2) "$K"
```
## Step 6: Import pre-built Nifi template and run the processors
This step can be automated further by calling NIFI REST APIs in the future, for now, it is a manual step:
  1. The prebuilt Nifi template file path is: nifi/hdfs-events-to-redis.xml
  2. Use built-in Nifi GUI to import, supply HDFS password, and run: http://localhost:8091/nifi/ 

## Step 7: Test this deployment set up for HDFS: automated event-driven invocation of serverless function
Run following steps:
  1. Use built-in HDFS GUI (or caerus_hdfs_client) to upload an image file into a HDFS folder called "/input": http://localhost:9870/explorer.html#/input
  2. Then use built-in HDFS GUI to check newly created thumbnail by serverless function: http://localhost:9870/explorer.html#/thumbnailsbucket

## Step 8: Test this deployment set up for HDFS: explicit invocation of serverless function
Run following steps:
  1. run this cli command:
```
root@ubuntu1804:/home/ubuntu/openinfralabs/caerus-udf/hdfsclient# java -jar target/hdfsclient-0.0.1-SNAPSHOT.jar put -s "/home/ubuntu/images/new/sample5.jpg" -t images/sample5.jpg -u caerus-faas-spring-thumbnail-hdfs -i "400, 600"
Creating connection to webHDFS endpoint: http://localhost:9870/webhdfs/v1/images/sample5.jpg?op=CREATE&overwrite=true&noredirect=true
Creating connection to webHDFS endpoint: http://localhost:8000/webhdfs/v1/images/sample5.jpg?op=CREATE&namenoderpcaddress=namenode:9000&createflag=&createparent=true&overwrite=true
root@ubuntu1804:/home/ubuntu/openinfralabs/caerus-udf/hdfsclient#
```
  2. Then use built-in HDFS GUI to check newly created thumbnail by serverless function: http://localhost:9870/explorer.html#/thumbnailsbucket

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
