# openfaas-client

## Credit to this site: https://github.com/cloudatio/openfaas-client-java
Our contributions are to: 
  - Add basic authentication support
  - Updatie code and depend libraries (e.g. from okhttp to okhttp3 etc.).

## Requirements

Building the API client library requires [Maven](https://maven.apache.org/) to be installed.

## Installation

To install the API client library to your local Maven repository, simply execute:

```shell
mvn install
```

To deploy it to a remote Maven repository instead, configure the settings of the repository and execute:

```shell
mvn deploy
```

Refer to the [official documentation](https://maven.apache.org/plugins/maven-deploy-plugin/usage.html) for more information.

### Maven users

Add this dependency to your project's POM:

```xml
<dependency>
    <groupId>com.cloudatio.openfaas</groupId>
    <artifactId>openfaas-client</artifactId>
    <version>0.0.1</version>
    <scope>compile</scope>
</dependency>
```

### Gradle users

Add this dependency to your project's build file:

```groovy
compile "com.cloudatio.openfaas:openfaas-client:0.0.1"
```

### Others

At first generate the JAR by executing:

    mvn package

Then manually install the following JARs:

* target/openfaas-client-0.0.1.jar
* target/lib/*.jar

## Getting Started

Please follow the [installation](#installation) instruction and execute the following Java code:

```java

import io.swagger.client.*;
import io.swagger.client.auth.*;
import io.swagger.client.model.*;
import io.swagger.client.api.DefaultApi;

import java.io.File;
import java.util.*;

public class DefaultApiExample {

    public static void main(String[] args) {
        
        DefaultApi apiInstance = new DefaultApi();
        String functionName = "functionName_example"; // String | Function name
        byte[] input = BINARY_DATA_HERE; // byte[] | (Optional) data to pass to function
        try {
            apiInstance.asyncFunctionFunctionNamePost(functionName, input);
        } catch (ApiException e) {
            System.err.println("Exception when calling DefaultApi#asyncFunctionFunctionNamePost");
            e.printStackTrace();
        }
    }
}

```

## Documentation for API Endpoints

All URIs are relative to *http://localhost*

Class | Method | HTTP request | Description
------------ | ------------- | ------------- | -------------
*DefaultApi* | [**asyncFunctionFunctionNamePost**](docs/DefaultApi.md#asyncFunctionFunctionNamePost) | **POST** /async-function/{functionName} | Invoke a function asynchronously in OpenFaaS
*DefaultApi* | [**functionFunctionNamePost**](docs/DefaultApi.md#functionFunctionNamePost) | **POST** /function/{functionName} | Invoke a function defined in OpenFaaS
*DefaultApi* | [**healthzGet**](docs/DefaultApi.md#healthzGet) | **GET** /healthz | Healthcheck
*DefaultApi* | [**systemAlertPost**](docs/DefaultApi.md#systemAlertPost) | **POST** /system/alert | Event-sink for AlertManager, for auto-scaling
*DefaultApi* | [**systemFunctionFunctionNameGet**](docs/DefaultApi.md#systemFunctionFunctionNameGet) | **GET** /system/function/{functionName} | Get a summary of an OpenFaaS function
*DefaultApi* | [**systemFunctionsDelete**](docs/DefaultApi.md#systemFunctionsDelete) | **DELETE** /system/functions | Remove a deployed function.
*DefaultApi* | [**systemFunctionsGet**](docs/DefaultApi.md#systemFunctionsGet) | **GET** /system/functions | Get a list of deployed functions with: stats and image digest
*DefaultApi* | [**systemFunctionsPost**](docs/DefaultApi.md#systemFunctionsPost) | **POST** /system/functions | Deploy a new function.
*DefaultApi* | [**systemFunctionsPut**](docs/DefaultApi.md#systemFunctionsPut) | **PUT** /system/functions | Update a function.
*DefaultApi* | [**systemInfoGet**](docs/DefaultApi.md#systemInfoGet) | **GET** /system/info | Get info such as provider version number and provider orchestrator
*DefaultApi* | [**systemScaleFunctionFunctionNamePost**](docs/DefaultApi.md#systemScaleFunctionFunctionNamePost) | **POST** /system/scale-function/{functionName} | Scale a function


## Documentation for Models

 - [DeleteFunctionRequest](docs/DeleteFunctionRequest.md)
 - [FunctionDefintion](docs/FunctionDefintion.md)
 - [FunctionDefintionLimits](docs/FunctionDefintionLimits.md)
 - [FunctionListEntry](docs/FunctionListEntry.md)
 - [Info](docs/Info.md)
 - [InfoProvider](docs/InfoProvider.md)
 - [InfoProviderVersion](docs/InfoProviderVersion.md)
 - [InfoVersion](docs/InfoVersion.md)


## Documentation for Authorization

Authentication schemes defined for the API:
### basicAuth

- **Type**: HTTP basic authentication


## Recommendation

It's recommended to create an instance of `ApiClient` per thread in a multithreaded environment to avoid any potential issues.

## Author



