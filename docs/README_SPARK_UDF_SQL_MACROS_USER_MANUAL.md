# How to Use Spark SQL Macros for Spark Scala UDF Pushdown   

## Background
Currently Spark doesn't support any UDF pushdown with the exception of JDBC/database datascource case, and Spark UDF is run in a black box on compute-sdie, and Spark Catalyst, Spark SQL Optimizer, can't optimize UDF.  

[Spark SQL Macros](https://github.com/hbutani/spark-sql-macros) is a github project to take advantage Scala Macros feature, it adopts similar user syntax of regular Spark UDF registration and invocation, underneath it, the macros will auto translate the **function body** to an equivalent Spark Catalyst Expression with holes (MacroArg Catalyst expressions) during compiling time, then register the function name and its serialized object into Spark function registry, so that during runtime, any function invocation is replaced by the Catalyst Expression with the holes replaced by the calling site arguments.  

There are 2 potential performance benefits for replacing function calls with native catalyst expressions:
- Evaluation performance. Since it **avoids the Serialization and DeSerialization cost** at the function boundary.
- More importantly, since the plan has native catalyst expressions more optimizations are possible.
  - For example, you can get null check etc. for free
  - **Pushdown** of operations to Data Sources has a huge impact. 

In case of failure to translate due to unsupported functions etc cases such as argument functions provided to DataFrame map and flatMap high-order functions, it will just fall back 
to normal client side UDF, so the correctness is still there. 

## Performance gain
According to the author, this solution can make the query **2-3 times faster** than the native Spark UDF function based query:
["In our testing we found the task times for the macro based query to be around 2-3 faster than the function based query"](https://github.com/hbutani/spark-sql-macros)

## How to use this functionality
Although the original github repo provides some basic steps on how to use this functionality, more details are added here from our testing results: 
### Build
- Install Spark 3.1.0 or above (tested with Spark 3.1.1), Scala 2.12.10 or above (tested with Scala 2.12.10), and sbt 1.4.6 or above (tested with sbt 1.4.6)
- Clone the repo
  ```
  # git clone https://github.com/hbutani/spark-sql-macros.git
  # cd spark-sql-macros
  ```
- Build jar by issuing the following sbt command:
  ```shell
  root@ubuntu1804:/opt/spark-sql-macros# sbt sql/assembly
  root@ubuntu1804:/opt/spark-sql-macros# ls -la sql/target/scala-2.12/
  -rw-r--r-- 1 root root 630446 Apr  7 11:30 spark-sql-macros_2.12.10_0.1.0-SNAPSHOT.jar
  ```
- Add the built jar into any Spark environment. 
  - For Spark-Shell, The jar file need to be loaded like this (using –jar might cause problem: ) spark-shell --driver-class-path sql/target/scala-2.12/spark-sql-macros_2.12.10_0.1.0-SNAPSHOT.jar
  - For Spark-submit, details will be added later
### Running an example
1. Create a '/data/source/' folder on your local machine, copy people.json file from Spark installation example folder (.../examples/src/main/resources/people.json) to /data/source/
2. Run:
```SLF4J: Class path contains multiple SLF4J bindings.
spark-shell --driver-class-path sql/target/scala-2.12/spark-sql-macros_2.12.10_0.1.0-SNAPSHOT.jar
      Welcome to
      ____              __
     / __/__  ___ _____/ /__
    _\ \/ _ \/ _ `/ __/  '_/
/___/ .__/\_,_/_/ /_/\_\   version 3.1.1
/_/

Using Scala version 2.12.10 (OpenJDK 64-Bit Server VM, Java 1.8.0_282)
Type in expressions to have them evaluated.
Type :help for more information.

scala> import org.apache.spark.sql.types._
scala> val schema = new StructType().add("name", StringType, true).add("age", IntegerType, true)
scala> val df_with_schema = spark.read.schema(schema).json("file:///data/source/people.json")
scala> df_with_schema.printSchema()
scala> df_with_schema.show(false)
scala> df_with_schema.createOrReplaceTempView("people_with_schema")
scala> import org.apache.spark.sql.defineMacros._
scala >
spark.registerMacro("intUDM", spark.udm((i: Int) => {
   val j = 2
   i + j
  }))
scala> val udmResult = spark.sql("SELECT * FROM people_with_schema WHERE intUDM(age) > 15")
scala> udmResult.explain(true)
scala >
  spark.udf.register("intUDF", (i: Int) => {
       val j = 2
       i + j
      })
scala> val udfResult = spark.sql("SELECT * FROM people_with_schema WHERE intUDF(age) > 15")
scala> udfResult.explain(true)
scala> val sql_with_schema = spark.sql("select * from people_with_schema where age+2 > 15")
scala> sql_with_schema.show()
scala> sql_with_schema.explain(true)
       == Parsed Logical Plan ==
       'Project [*]
       +- 'Filter ('age > 15)
       +- 'UnresolvedRelation [people_with_schema], [], false

== Analyzed Logical Plan ==
name: string, age: int
Project [name#53, age#54]
+- Filter (age#54 > 15)
+- SubqueryAlias people_with_schema
+- Relation[name#53,age#54] json

== Optimized Logical Plan ==
Filter (isnotnull(age#54) AND (age#54 > 15))
+- Relation[name#53,age#54] json

== Physical Plan ==
*(1) Filter (isnotnull(age#54) AND (age#54 > 15))
+- FileScan json [name#53,age#54] Batched: false, DataFilters: [isnotnull(age#54), (age#54 > 15)], Format: JSON, Location: InMemoryFileIndex[file:/data/source/people.json], PartitionFilters: [], PushedFilters: [IsNotNull(age), GreaterThan(age,15)], ReadSchema: struct<name:string,age:int>
```
## Spark Macro: initial Scala translation support

The following Scala constructs are provided for the translation support:

- Primitive data types, Case Classes in Scala, Tuples, Arrays, Collections and Maps
  - For Tuples and Case Classes we will translate field access
- Value Definitions
- Arbitrary References at the macro call-site that can be evaluated at macro definition time
- Arithmetic, Math, Datetime, String, and Decimal functions supported in Catalyst
- Recursive macro invocations
- Scala Case and If statements
See [Spark SQL Macro examples page.](https://github.com/hbutani/spark-sql-macros/wiki/Spark_SQL_Macro_examples) 

The things are not supported yet (more will be filled upon our ongoing testing):
- Translation of DataFrame map and flatMap high-order functions to equivalent catalyst expressions

## Restrictions and limitations

Although Spark SQL Macros has performance advantages, it has few usability issues, a project has been started to solve some of the issues:
- Cannot support UDF as a variable for DataFrame API (like df.filter() et.) without UDF registration. User must register it.
  - Spark allows user to do any of following related to use UDF
      - define UDF as a variable, then use that variable in DataFrame API. This kind of UDF can not be used in Spark SQL context
        ```shell
            > val plusOne = udf((x: Int) => x + 1)
            > df.filter(plusOne(age))
            this will fail => > spark.sql("Select plusOne(5)").show()              
        ```
      - define UDF (in **flexible form**, such as below "val a" is defined outside **function body**) and register UDF, then UDF can be used in either DataFrame API or Spark SQL context
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
- Cannot support existing UDF implementation, user might have to rewrite code
  - user needs to change existing Scala program to introduce udm (macros replacement for udf), then recompile: 
    ```shell
      import org.apache.spark.sql.defineMacros._

      spark.registerMacro("intUDM", spark.udm((i: Int) => {
      val j = 2
      i + j
      }))
    ```
    
