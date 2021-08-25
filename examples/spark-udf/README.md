# Caerus Spark UDF Compiler - Auto Spark UDF Pushdown

## Introduction
[Caerus UDF Compiler](https://github.com/open-infrastructure-labs/caerus-spark-udf-compiler-from-rapids) is modified from a plugin called [udf-compiler](https://github.com/NVIDIA/spark-rapids/tree/branch-21.10/udf-compiler) of [Nvidia RAPIDS Accelerator For Apache Saprk](https://github.com/NVIDIA/spark-rapids). 

It is a UDF compiler extension (via Spark rule injection) to attempt for auto translation of Spark UDFs bytecode into Spark Catalyst expressions, so that it can take advantage of Catalyst optimization including codegen, predict pushdown etc. for better UDF performance. 

## Examples On How To Use Caerus UDF Compiler 
Here are the steps:
### Step 1: Get the latest Caerus UDF Compiler code and build udf project
```
> git clone git@github.com:open-infrastructure-labs/caerus-spark-udf-compiler-from-rapids.git
(if needed) > git pull
> cd $CAERUS_UDF_COMPILER_HOME/
> root@ubuntu1804:/home/ubuntu/openinfralabs/caerus-spark-udf-compiler-from-rapids# mvn --projects udf-compiler --also-make -DskipTests=true clean package 
root@ubuntu1804:/home/ubuntu/openinfralabs/caerus-spark-udf-compiler-from-rapids# ls -la udf-compiler/target/
...
-rw-r--r--  1 root root 139592 Aug 24 08:50 rapids-4-spark-udf_2.12-21.10.0-SNAPSHOT.jar
...
root@ubuntu1804:/home/ubuntu/openinfralabs/caerus-spark-udf-compiler-from-rapids#
```
### Step 2: Build Spark UDF Example Jar
```
root@ubuntu1804:/home/ubuntu/openinfralabs/caerus-udf/examples/spark-udf# mvn clean package
root@ubuntu1804:/home/ubuntu/openinfralabs/caerus-udf/examples/spark-udf# ls -la target/
...
-rw-r--r-- 1 root root 8293 Aug 25 09:02 spark-udf-1.0-SNAPSHOT.jar
root@ubuntu1804:/home/ubuntu/openinfralabs/caerus-udf/examples/spark-udf#
```
### Step 3: Use Caerus UDF Compiler For Spark Application
```
root@ubuntu1804:/home/ubuntu/openinfralabs/caerus-udf/examples/spark-udf# spark-submit --jars /home/ubuntu/openinfralabs/caerus-spark-udf-compiler-from-rapids/udf-compiler/target/rapids-4-spark-udf_2.12-21.10.0-SNAPSHOT.jar --conf spark.sql.extensions=com.nvidia.spark.udf.Plugin --class org.openinfralabs.caerus.examples.SubmitExampleNativeSparkUdf --master local[4] target/spark-udf-1.0-SNAPSHOT.jar 
........
== Parsed Logical Plan ==
'Project [*]
+- 'Filter ('intUDF('age) > 15)
   +- 'UnresolvedRelation [people_with_schema], [], false

== Analyzed Logical Plan ==
name: string, age: int
Project [name#0, age#1]
+- Filter ((age#1 + 2) > 15)
   +- SubqueryAlias people_with_schema
      +- Relation[name#0,age#1] json

== Optimized Logical Plan ==
Filter (isnotnull(age#1) AND ((age#1 + 2) > 15))
+- Relation[name#0,age#1] json

== Physical Plan ==
*(1) Filter (isnotnull(age#1) AND ((age#1 + 2) > 15))
+- FileScan json [name#0,age#1] Batched: false, DataFilters: [isnotnull(age#1), ((age#1 + 2) > 15)], Format: JSON, Location: InMemoryFileIndex[file:/data/source/people.json], PartitionFilters: [], 
PushedFilters: [IsNotNull(age)], ReadSchema: struct<name:string,age:int>
........
root@ubuntu1804:/home/ubuntu/openinfralabs/caerus-udf/examples/spark-udf# 
```
To do a before-after comparison, simply remove "--conf spark.sql.extensions=com.nvidia.spark.udf.Plugin" (the --jars can be removed too, but without the --conf, it will do no-op):
```
root@ubuntu1804:/home/ubuntu/openinfralabs/caerus-udf/examples/spark-udf# spark-submit --jars /home/ubuntu/openinfralabs/caerus-spark-udf-compiler-from-rapids/udf-compiler/target/rapids-4-spark-udf_2.12-21.10.0-SNAPSHOT.jar  --class org.openinfralabs.caerus.examples.SubmitExampleNativeSparkUdf --master local[4] target/spark-udf-1.0-SNAPSHOT.jar 
.......
== Parsed Logical Plan ==
'Project [*]
+- 'Filter ('intUDF('age) > 15)
   +- 'UnresolvedRelation [people_with_schema], [], false

== Analyzed Logical Plan ==
name: string, age: int
Project [name#0, age#1]
+- Filter (if (isnull(age#1)) null else intUDF(knownnotnull(age#1)) > 15)
   +- SubqueryAlias people_with_schema
      +- Relation[name#0,age#1] json

== Optimized Logical Plan ==
Filter (if (isnull(age#1)) null else intUDF(knownnotnull(age#1)) > 15)
+- Relation[name#0,age#1] json

== Physical Plan ==
*(1) Filter (if (isnull(age#1)) null else intUDF(knownnotnull(age#1)) > 15)
+- FileScan json [name#0,age#1] Batched: false, DataFilters: [(if (isnull(age#1)) null else intUDF(knownnotnull(age#1)) > 15)], Format: JSON, Location: InMemoryFileIndex[file:/data/source/people.json], PartitionFilters: [], 
PushedFilters: [], ReadSchema: struct<name:string,age:int>

......
root@ubuntu1804:/home/ubuntu/openinfralabs/caerus-udf/examples/spark-udf# 

```
The other option is to implement --config option inside Spark application, then run following command:
```
root@ubuntu1804:/home/ubuntu/openinfralabs/caerus-udf/examples/spark-udf# spark-submit --jars /home/ubuntu/openinfralabs/caerus-spark-udf-compiler-from-rapids/udf-compiler/target/rapids-4-spark-udf_2.12-21.10.0-SNAPSHOT.jar --class org.openinfralabs.caerus.examples.SubmitExampleWithUdfCompiler --master local[4] target/spark-udf-1.0-SNAPSHOT.jar 
......
== Parsed Logical Plan ==
'Project [*]
+- 'Filter ('intUDF('age) > 15)
   +- 'UnresolvedRelation [people_with_schema], [], false

== Analyzed Logical Plan ==
name: string, age: int
Project [name#0, age#1]
+- Filter ((age#1 + 2) > 15)
   +- SubqueryAlias people_with_schema
      +- Relation[name#0,age#1] json

== Optimized Logical Plan ==
Filter (isnotnull(age#1) AND ((age#1 + 2) > 15))
+- Relation[name#0,age#1] json

== Physical Plan ==
*(1) Filter (isnotnull(age#1) AND ((age#1 + 2) > 15))
+- FileScan json [name#0,age#1] Batched: false, DataFilters: [isnotnull(age#1), ((age#1 + 2) > 15)], Format: JSON, Location: InMemoryFileIndex[file:/data/source/people.json], PartitionFilters: [], 
PushedFilters: [IsNotNull(age)], ReadSchema: struct<name:string,age:int>
......
root@ubuntu1804:/home/ubuntu/openinfralabs/caerus-udf/examples/spark-udf# 

```