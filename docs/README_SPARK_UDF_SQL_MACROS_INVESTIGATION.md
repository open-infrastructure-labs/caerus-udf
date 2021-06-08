# Investigation on Usability of Spark SQL Macros for Spark Scala UDF Pushdown   

## Background
Currently Spark doesn't support any UDF pushdown with the exception of JDBC/database datascource case, and Spark UDF is run in a black box on compute-sdie,
and Spark Catalyst, Spark SQL Optimizer, can't optimize UDF.  

[Spark SQL Macros](https://github.com/hbutani/spark-sql-macros) is a github project to take advantage Scala Macros feature, it adopts similar user syntax of regular Spark UDF registration and invocation, underneath it, the macros will auto translate the **function body** to an equivalent Spark Catalyst Expression with holes (MarcroArg Catalyst expressions) during compiling time, then register the function name and its serialized object into Spark function registry, so that during runtime, any function invocation is replaced by the Catalyst Expression with the holes replaced by the calling site arguments.  

There are 2 potential performance benefits for replacing function calls with native catalyst expressions:
- evaluation performance. Since we **avoid the SerDe cost** at the function boundary.
- More importantly, since the plan has native catalyst expressions more optimizations are possible.
  - For example you can get null check etc. for free
  - **Pushdown** of operations to Datsources has a huge impact. 

In case of failure to translate due to unsupported functions etc cases such as argument functions provided to DataFrame map and flatMap high-order functions, it will just fall back 
to normal client side UDF, so the correctness is still there. 

Although Spark SQL Macros has performance advantages, it has few usability issues. It feels like the author stopped short, and we didn't know why:
- Cannot support UDF as a variable for dataframe API (like df.filter() et.) without UDF registration. User must register it.
  - Spark allows user to do any of following related to use UDF
      - define UDF as a variable, then use that variable in dataframe API. This kind of UDF can not be used in Spark SQL context
        ```shell
            > val plusOne = udf((x: Int) => x + 1)
            > df.filter(plusOne(age))
            this will fail => > spark.sql("Select plusOne(5)").show()              
        ```
      - define UDF (in **flexible form**, such as below "val a" is defined outside **function body**) and register UDF, then UDF can be used in either dataframe API or Spark SQL context
        ```shell
            > val a = Array(2)
            > spark.udf.register("intUDF", (i: Int) => {
                  val j = a(0)
                  i + j
            })
           > spark.sql("Select * from people where intUDF(age) > 21").show()
           > df.filter(intUDF(age) > 21)    
        ```
- Cannot support **flexible form** UDF in UDF registration case (above Array case will fail to pushdown)
- Cannot support existing UDF implementation, user might have to rewrite
  - user needs to change existing Scala program to introduce udm (macros replacement for udf), then recompile
    
In summary, Spark SQL Macros out-of-box product might be only good for users who are willing write new UDF/UDMs to gain performance. 

In order to solve these problems, we have started the investigation on if there is a way to solve all these. 

## Things we can do so far
1. Have a Catalyst extension code module working to capture function binary and its metadata including name, input parameters, output parameters etc.
   - Intercept the call of udf() in spark by setting spark config for Spark Session Extensions
   - Calling Spark SQL Macros APIs (udm) as an external libary by following its test examples
   - Support UDF variable and registration in one scoop, because both call udf()   
2. Have a working Scala macros setup including build configurations, and did many experiments on some of the macros contents including quasiquatation, def macros etc.
3. Have a working setup from compile time to runtime except that had to hard code the **function body**

## Thing we can't do
1. Can't get the raw **function body** (refers to everything inside udm function below) from existing Spark APIs (Spark don't need to keep the raw **function body** around once it has function binary)
```shell
spark.registerMacro("intUDM", spark.udm((i: Int) => {
   val j = 2
   i + j
  }))
```
## Recommendations? 
1. It might need a lot of efforts to "reverse engineer" from the function binary and its metadata to the raw **function body**. The deserialization might have to be called again
2. Scala Macros becomes more and more important in Scala 3.0 (is scheduled for release in early-mid 2021), code name Dotty. The existing Macro syntax will go away in Dotty,
but it becomes more user-friendly and has more features with meta-programming. A lot of Spark performance gains might be delegated to Scala Macros.
3. We might want to continue to pursue this, but in a longer timeframe.
