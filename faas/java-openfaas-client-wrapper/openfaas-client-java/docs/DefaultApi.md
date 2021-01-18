# DefaultApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**asyncFunctionFunctionNamePost**](DefaultApi.md#asyncFunctionFunctionNamePost) | **POST** /async-function/{functionName} | Invoke a function asynchronously in OpenFaaS
[**functionFunctionNamePost**](DefaultApi.md#functionFunctionNamePost) | **POST** /function/{functionName} | Invoke a function defined in OpenFaaS
[**healthzGet**](DefaultApi.md#healthzGet) | **GET** /healthz | Healthcheck
[**systemAlertPost**](DefaultApi.md#systemAlertPost) | **POST** /system/alert | Event-sink for AlertManager, for auto-scaling
[**systemFunctionFunctionNameGet**](DefaultApi.md#systemFunctionFunctionNameGet) | **GET** /system/function/{functionName} | Get a summary of an OpenFaaS function
[**systemFunctionsDelete**](DefaultApi.md#systemFunctionsDelete) | **DELETE** /system/functions | Remove a deployed function.
[**systemFunctionsGet**](DefaultApi.md#systemFunctionsGet) | **GET** /system/functions | Get a list of deployed functions with: stats and image digest
[**systemFunctionsPost**](DefaultApi.md#systemFunctionsPost) | **POST** /system/functions | Deploy a new function.
[**systemFunctionsPut**](DefaultApi.md#systemFunctionsPut) | **PUT** /system/functions | Update a function.
[**systemInfoGet**](DefaultApi.md#systemInfoGet) | **GET** /system/info | Get info such as provider version number and provider orchestrator
[**systemScaleFunctionFunctionNamePost**](DefaultApi.md#systemScaleFunctionFunctionNamePost) | **POST** /system/scale-function/{functionName} | Scale a function


<a name="asyncFunctionFunctionNamePost"></a>
# **asyncFunctionFunctionNamePost**
> asyncFunctionFunctionNamePost(functionName, input)

Invoke a function asynchronously in OpenFaaS

See https://github.com/openfaas/faas/blob/master/guide/asynchronous.md.

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
String functionName = "functionName_example"; // String | Function name
byte[] input = BINARY_DATA_HERE; // byte[] | (Optional) data to pass to function
try {
    apiInstance.asyncFunctionFunctionNamePost(functionName, input);
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#asyncFunctionFunctionNamePost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **functionName** | **String**| Function name |
 **input** | **byte[]**| (Optional) data to pass to function | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

<a name="functionFunctionNamePost"></a>
# **functionFunctionNamePost**
> functionFunctionNamePost(functionName, input)

Invoke a function defined in OpenFaaS

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
String functionName = "functionName_example"; // String | Function name
byte[] input = BINARY_DATA_HERE; // byte[] | (Optional) data to pass to function
try {
    apiInstance.functionFunctionNamePost(functionName, input);
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#functionFunctionNamePost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **functionName** | **String**| Function name |
 **input** | **byte[]**| (Optional) data to pass to function | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

<a name="healthzGet"></a>
# **healthzGet**
> healthzGet()

Healthcheck

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
try {
    apiInstance.healthzGet();
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#healthzGet");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

<a name="systemAlertPost"></a>
# **systemAlertPost**
> systemAlertPost(body)

Event-sink for AlertManager, for auto-scaling

Internal use for AlertManager, requires valid AlertManager alert JSON

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
Object body = null; // Object | Function to delete
try {
    apiInstance.systemAlertPost(body);
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#systemAlertPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | **Object**| Function to delete | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="systemFunctionFunctionNameGet"></a>
# **systemFunctionFunctionNameGet**
> FunctionListEntry systemFunctionFunctionNameGet(functionName)

Get a summary of an OpenFaaS function

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
String functionName = "functionName_example"; // String | Function name
try {
    FunctionListEntry result = apiInstance.systemFunctionFunctionNameGet(functionName);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#systemFunctionFunctionNameGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **functionName** | **String**| Function name |

### Return type

[**FunctionListEntry**](FunctionListEntry.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

<a name="systemFunctionsDelete"></a>
# **systemFunctionsDelete**
> systemFunctionsDelete(body)

Remove a deployed function.



### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
DeleteFunctionRequest body = new DeleteFunctionRequest(); // DeleteFunctionRequest | Function to delete
try {
    apiInstance.systemFunctionsDelete(body);
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#systemFunctionsDelete");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**DeleteFunctionRequest**](DeleteFunctionRequest.md)| Function to delete |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="systemFunctionsGet"></a>
# **systemFunctionsGet**
> List&lt;FunctionListEntry&gt; systemFunctionsGet()

Get a list of deployed functions with: stats and image digest

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
try {
    List<FunctionListEntry> result = apiInstance.systemFunctionsGet();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#systemFunctionsGet");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**List&lt;FunctionListEntry&gt;**](FunctionListEntry.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="systemFunctionsPost"></a>
# **systemFunctionsPost**
> systemFunctionsPost(body)

Deploy a new function.



### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
FunctionDefintion body = new FunctionDefintion(); // FunctionDefintion | Function to deploy
try {
    apiInstance.systemFunctionsPost(body);
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#systemFunctionsPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**FunctionDefintion**](FunctionDefintion.md)| Function to deploy |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="systemFunctionsPut"></a>
# **systemFunctionsPut**
> systemFunctionsPut(body)

Update a function.



### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
FunctionDefintion body = new FunctionDefintion(); // FunctionDefintion | Function to update
try {
    apiInstance.systemFunctionsPut(body);
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#systemFunctionsPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**FunctionDefintion**](FunctionDefintion.md)| Function to update |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="systemInfoGet"></a>
# **systemInfoGet**
> Info systemInfoGet()

Get info such as provider version number and provider orchestrator

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
try {
    Info result = apiInstance.systemInfoGet();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#systemInfoGet");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**Info**](Info.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="systemScaleFunctionFunctionNamePost"></a>
# **systemScaleFunctionFunctionNamePost**
> systemScaleFunctionFunctionNamePost(functionName, input)

Scale a function

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
String functionName = "functionName_example"; // String | Function name
byte[] input = BINARY_DATA_HERE; // byte[] | Function to scale plus replica count
try {
    apiInstance.systemScaleFunctionFunctionNamePost(functionName, input);
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#systemScaleFunctionFunctionNamePost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **functionName** | **String**| Function name |
 **input** | **byte[]**| Function to scale plus replica count | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

