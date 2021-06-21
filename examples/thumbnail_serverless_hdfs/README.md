# Preconditions
1. Openfaas is up running
2. Port forwarding
```
kubectl port-forward -n openfaas svc/gateway 8080:8080 &
```
3. HDFS cluster (can be dockers cluster) is up running by following this link:
[caerus-udf/ndpService/README.md](../../ndpService/README.md)



# Operations on Function
To build, push (to docker hub), deploy and invoke openfaas java(8) spring boot thumbnail app serverless function, do follwoings:
1. Build
```
faas build -f caerus-faas-spring-thumbnail-hdfs.yml
```
2. Push to docker hub
```
docker push ywang529/caerus-faas-spring-thumbnail-hdfs
```
or
```
docker push futureweibostonlab/caerus-faas-spring-thumbnail-hdfs
```

3. Deploy
```
faas deploy -f caerus-faas-spring-thumbnail-hdfs.yml
```
4. Invoke
```
echo '{ "bucketName": "imagesbucket", "objectKey": "sample.jpg", "inputParameters": ["200", "400"] }' | faas invoke caerus-faas-spring-thumbnail-hdfs
```
