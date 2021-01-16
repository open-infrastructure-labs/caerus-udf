To test this function, do following steps:
1. build the project: 
```
mvn clean package
```

2. Start the app via IDE or command line:
```
java -jar target/thumbnail-0.0.1-SNAPSHOT.jar
```
3. Use post man or curl command to send GET request with following parameters:
```
localhost:8090/imagesbucket/sample.jpg?inputParameters=200&inputParameters=400
```
```
curl --location --request GET 'localhost:8090/imagesbucket/sample.jpg?inputParameters=200&inputParameters=400'
```
