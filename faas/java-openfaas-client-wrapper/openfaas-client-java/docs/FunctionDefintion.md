
# FunctionDefintion

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**service** | **String** | Name of deployed function | 
**network** | **String** | Docker swarm network, usually func_functions |  [optional]
**image** | **String** | Docker image in accessible registry | 
**envProcess** | **String** | Process for watchdog to fork | 
**envVars** | **Map&lt;String, String&gt;** | Overrides to environmental variables |  [optional]
**constraints** | **List&lt;String&gt;** |  |  [optional]
**labels** | **List&lt;String&gt;** | An array of labels used by the back-end for making scheduling or routing decisions |  [optional]
**annotations** | **List&lt;String&gt;** | An array of annotations used by the back-end for management, orchestration, events and build tasks |  [optional]
**secrets** | **List&lt;String&gt;** |  |  [optional]
**registryAuth** | **String** | Private registry base64-encoded basic auth (as present in ~/.docker/config.json) |  [optional]
**limits** | [**FunctionDefintionLimits**](FunctionDefintionLimits.md) |  |  [optional]
**requests** | [**FunctionDefintionLimits**](FunctionDefintionLimits.md) |  |  [optional]



