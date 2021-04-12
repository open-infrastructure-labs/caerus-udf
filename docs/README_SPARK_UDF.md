# Spark UDF Support

More and more upper level applications like AI field (via Spark Mllib) are using Spark UDFs, while they are notorious in performance. In Facebook production environment, small amount of the UDFs can take up to 70% of the CPU time of the entire company’s queries.

Spark UDFs currently have these characteristics:
- The format is primarily lambda functions that major goal is to operate data row by row. The other complex format like UDAF/UDTF (back ported from Hive), and free form (programming language like Java friendly) are afterthought. This Apache Jira item spells out the need to expand Spark UDF support: https://issues.apache.org/jira/browse/SPARK-23818
- The UDF in its native form cannot be optimized, the optimization here refers to removing the need of unnecessary serialization/de-serialization, null checking, and allow UDFs being pushdown etc.
- The UDF performance in general is 2-3 times worse than the native Spark functions

There is strong need to improve performance and usability for Spark UDFs (see more details in this Apache Jira item: https://issues.apache.org/jira/browse/SPARK-27658 ).

We made investigations on Spark UDF support, trying to improve performance and usability of the Spark UDF, here are the findings:
## 1.	Changing Spark UDF into native Spark expression
The details are described here: https://databricks.com/session_eu20/optimizing-apache-spark-udfs

Our Caerus implementation example can be found here: https://metis.atlassian.net/browse/CAERUS-225


The expressions need include implementation of codeGen, isNull etc. to allow Spark UDF to be operated just as native Spark functions.
- The performance of such UDFs will be equivalent to the native Spark functions and support optimization like pushdown etc.
- The major disadvantage of this approach is the need to recompile the Spark core SQL module, thus results in custom-built Spark

## 2.	Spark SQL Macros
The details are described here: https://github.com/hbutani/spark-sql-macros
      
Spark SQL Macros provide a capability to register custom functions into a Spark Session that is similar to custom UDF Registration capability of Spark. The difference being that the SQL Macros registration mechanism attempts to translate the function body to an equivalent Spark catalyst Expression with holes(MarcroArg catalyst expressions). A FunctionBuilder that encapsulates this expression is registered in Spark's FunctionRegistry. Then any function invocation is replaced by the equivalent catalyst Expression with the holes replaced by the calling site arguments.

There are 2 potential performance benefits for replacing function calls with native catalyst expressions:
- Evaluation performance. Since we avoid the SerDe cost at the function boundary.
- More importantly, since the plan has native catalyst expressions more optimizations are possible.
      - For example in the taxRate example below discount calculation can be eliminated.
      - Pushdown of operations to Datsources has a huge impact. For example see below for the Oracle SQL generated and pushed when a macro is defined instead of a UDF.

### How To Start:
Follow this link, but need some minor tweaks:
- The version we used to test are Spark 3.1.1 and Scala 2.12.10
- The jar file need to be loaded like this (using –jar might cause problem: )
      spark-shell --driver-class-path sql/target/scala-2.12/spark-sql-macros_2.12.10_0.1.0-SNAPSHOT.jar

Running an Example:
    
1. Copy people.json file from Spark installation example folder to /data/source/	
2. spark-shell --driver-class-path sql/target/scala-2.12/spark-sql-macros_2.12.10_0.1.0-SNAPSHOT.jar
```SLF4J: Class path contains multiple SLF4J bindings.
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
scala> val df_with_schema = spark.read.schema(schema).json("/data/source/people.json")
scala> df_with_schema.printSchema()
scala> df_with_schema.show(false)
scala> df_with_schema.createOrReplaceTempView("people_with_schema")
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

## 3.	Transport (UDF)
The details are described here: https://github.com/linkedin/transport
      
Transport is a framework for writing performant user-defined functions (UDFs) that are portable across a variety of engines including Apache Spark, Apache Hive, and Presto. Transport UDFs are also capable of directly processing data stored in serialization formats such as Apache Avro. With Transport, developers only need to implement their UDF logic once using the Transport API. Transport then takes care of translating the UDF to native UDF version targeted at various engines or formats. Currently, Transport is capable of generating engine-artifacts for Spark, Hive, and Presto, and format-artifacts for Avro. 

There are 2 potential benefits for Transport:
- Unification: one UDF, multiple compute engine translation
- Performance: the Spark translation will create native function (without the implementation of codeGen etc.), which will remove the need of ser


There are 2 potential benefits for Transport:
- Unification: one UDF, multiple compute engine translation
- Performance: the Spark translation will create native function (without the implementation of codeGen etc.), which will remove the need of serialization/de-serialization between Spark Catalyst and Scala code. Pushdown is not supported.
### How To Start:
Follow this link, but need some minor tweaks:
1. The version we used to test are Spark 2.3.0 and Scala 2.11.8. For latest Spark and Scala support, some build changes will be needed.
2. To build sample functions using

1. Copy people.json file from Spark installation example folder to /data/source/
2. spark-shell --jars transportable-udfs-example-udfs/build/libs/transportable-udfs-example-udfs-spark.jar
```
./gradlew build
```
“gradle build” might cause build issue.

Running an Example:
```
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/opt/spark/jars/slf4j-log4j12-1.7.16.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/opt/hadoop-2.8.2/share/hadoop/common/lib/slf4j-log4j12-1.7.10.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.Log4jLoggerFactory]
Setting default log level to "WARN".
To adjust logging level use sc.setLogLevel(newLevel). For SparkR, use setLogLevel(newLevel).
Spark context Web UI available at http://localhost:4040
Spark context available as 'sc' (master = local[*], app id = local-1617975482043).
Spark session available as 'spark'.
Welcome to
      ____              __
     / __/__  ___ _____/ /__
    _\ \/ _ \/ _ `/ __/  '_/
   /___/ .__/\_,_/_/ /_/\_\   version 2.3.0
      /_/
         
Using Scala version 2.11.8 (OpenJDK 64-Bit Server VM, Java 1.8.0_282)
Type in expressions to have them evaluated.
Type :help for more information.

scala> import com.linkedin.transport.examples.spark.NumericAddFunction
scala> val exampleUDF = NumericAddFunction.register("example_udf")
scala> spark.catalog.listFunctions.filter(_.name == "example_udf").show()
scala> import org.apache.spark.sql.types._
scala> val schema = new StructType().add("name", StringType, true).add("age", IntegerType, true)
scala> val df_with_schema = spark.read.schema(schema).json("/data/source/people.json")
scala> df_with_schema.printSchema()
scala> df_with_schema.show(false)
scala> df_with_schema.createOrReplaceTempView("people_with_schema")
scala> val sql_with_schema = spark.sql("select * from people_with_schema where age > 15")
scala> val sql_with_schema = spark.sql("select * from people_with_schema where example_udf(age, 5) > 25")
scala> sql_with_schema.show()
scala> sql_with_schema.explain(true)
== Parsed Logical Plan ==
'Project [*]
+- 'Filter ('example_udf('age, 5) > 25)
   +- 'UnresolvedRelation `people_with_schema`

== Analyzed Logical Plan ==
name: string, age: int
Project [name#78, age#79]
+- Filter (numericaddfunction(age#79, 5) > 25)
   +- SubqueryAlias people_with_schema
      +- Relation[name#78,age#79] json

== Optimized Logical Plan ==
Filter (numericaddfunction(age#79, 5) > 25)
+- Relation[name#78,age#79] json

== Physical Plan ==
Filter (numericaddfunction(age#79, 5) > 25)
+- FileScan json [name#78,age#79] Batched: false, Format: JSON, Location: InMemoryFileIndex[file:/data/source/people.json], PartitionFilters: [], PushedFilters: [], ReadSchema: struct<name:string,age:int>

scala> val sql_with_schema = spark.sql("select * from people_with_schema where age+5 > 25")
sql_with_schema: org.apache.spark.sql.DataFrame = [name: string, age: int]

scala> sql_with_schema.explain(true)
== Parsed Logical Plan ==
'Project [*]
+- 'Filter (('age + 5) > 25)
   +- 'UnresolvedRelation `people_with_schema`

== Analyzed Logical Plan ==
name: string, age: int
Project [name#78, age#79]
+- Filter ((age#79 + 5) > 25)
   +- SubqueryAlias people_with_schema
      +- Relation[name#78,age#79] json

== Optimized Logical Plan ==
Filter (isnotnull(age#79) && ((age#79 + 5) > 25))
+- Relation[name#78,age#79] json

== Physical Plan ==
*(1) Project [name#78, age#79]
+- *(1) Filter (isnotnull(age#79) && ((age#79 + 5) > 25))
   +- *(1) FileScan json [name#78,age#79] Batched: false, Format: JSON, Location: InMemoryFileIndex[file:/data/source/people.json], PartitionFilters: [], PushedFilters: [IsNotNull(age)], ReadSchema: struct<name:string,age:int>
```
