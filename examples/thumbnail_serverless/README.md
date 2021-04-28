# Preconditions
1. Openfaas is up running
2. Port forwarding
```
kubectl port-forward -n openfaas svc/gateway 8080:8080 &
```
3. Minio server is up running 

**Note**: minio server address is defined in this file:

[application.properties](examples/thumbnail_serverless/template/springboot/src/main/resources/application.properties)
```
	server.contextPath=/
	server.port=3001
	logging.path=/tmp
	logging.file=${logging.path}/thumbnailApplication.log
	minio.access.name=minioadmin
	minio.access.secret=minioadmin
	minio.url=http://192.168.1.118:9000
```
Make sure to adjust your Minio server IP, port and credentials based on your Minio actual settings, fail to do so it might cause function invocation error: 
```
root@ubuntu1804:/home/ubuntu/caerus-udf/examples/thumbnail_serverless# echo '{ "bucketName": "imagesbucket", "objectKey": "sample.jpg", "inputParameters": ["200", "400"] }' | faas invoke caerus-faas-spring-thumbnail
Server returned unexpected status code: 400 - Unexpected exception: java.net.NoRouteToHostException: Host is unreachable (Host unreachable)
root@ubuntu1804:/home/ubuntu/caerus-udf/examples/thumbnail_serverless#
```




# Operations on Function
To build, push (to docker hub), deploy and invoke openfaas java(8) spring boot thumbnail app serverless function, do follwoings:
1. Build
```
faas build -f caerus-faas-spring-thumbnail.yml
```
2. Push to docker hub
```
docker push ywang529/caerus-faas-spring-thumbnail
```
or
```
docker push futureweibostonlab//caerus-faas-spring-thumbnail
```

3. Deploy
```
faas deploy -f caerus-faas-spring-thumbnail.yml
```
4. Invoke
```
echo '{ "bucketName": "imagesbucket", "objectKey": "sample.jpg", "inputParameters": ["200", "400"] }' | faas invoke caerus-faas-spring-thumbnail
```
