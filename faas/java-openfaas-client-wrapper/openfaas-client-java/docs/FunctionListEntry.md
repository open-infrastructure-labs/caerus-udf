
# FunctionListEntry

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**name** | **String** | The name of the function | 
**image** | **String** | The fully qualified docker image name of the function | 
**invocationCount** | [**BigDecimal**](BigDecimal.md) | The amount of invocations for the specified function | 
**replicas** | [**BigDecimal**](BigDecimal.md) | The current minimal ammount of replicas | 
**availableReplicas** | [**BigDecimal**](BigDecimal.md) | The current available amount of replicas | 
**envProcess** | **String** | Process for watchdog to fork | 
**labels** | **Map&lt;String, String&gt;** |  | 
**annotations** | **Map&lt;String, String&gt;** |  |  [optional]



