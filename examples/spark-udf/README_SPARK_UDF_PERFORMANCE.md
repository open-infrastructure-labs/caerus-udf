# Caerus Spark UDF Performance

## Introduction

Spark native UDF is forced to run in a black box (opaque) on compute-side, and Spark Catalyst by design can't optimize (codegen, predict pushdown etc.) UDF. Thus Spark UDFs are often performance bottlenecks.

To solve the performance problems of the Spark UDFs, especially in the context of UDF pushdown for NDP, we think there are two fundamental approaches:
1. **Partial UDF Pushdown – Translatable UDF**: take full advantage of Spark Catalyst expressions, the UDF can somehow be translated into native expressions, so they can be automatically optimized by the Catalyst.
2. **Whole UDF Pushdown**: For cases like data intensive operations like ETL and ML/DL etc. They cannot, and cannot easily, be translated into expressions, a generic mechanism should be provided to allow UDF to be pushed down to storage, so that it can take advantage of NDP. But such UDFs should be used carefully, especially in SQL UDFs, to make sure it doesn’t not interfere the Spark SQL optimization.

**Partial UDF Pushdown - Translatable UDF**

Even when most of UDFs used in Spark SQL contain the contents, such as math operations, string manipulation, date and time, and relational operations etc. that can be translated into Spark Catalyst expression, but because UDFs are run in an opaque box, such contents inside the UDF cannot be detected.

There are several open-source solutions that we can get inspiration from:
  - [Nvidia spark-rapids udf-compiler](https://github.com/NVIDIA/spark-rapids)
  - [Spark-SQL-Macros](https://github.com/hbutani/spark-sql-macros)
  - [Informatica manual implement UDF as Spark native "functions"](https://databricks.com/session_eu20/optimizing-apache-spark-udfs): no source code, just a presentation

Since all above solutions attempt to translate opaque UDFs into Spark expressions, their performance gains should be very similar, they are majorly from following areas:
  - Operations (e.g. predicate) Pushdown
  - Catalyst related optimizations
    - WholeStageCodegen: optimization in serialization/de-serialization
    - Null optimization
    - Other Catalyst optimizations
    
For translatable UDF, since all above solutions need a lot additional work (see above restrictions/functional gaps of each solution) to have full production-level support, it is difficult to identify test benchmark for performance measurement.

For example, udf-compiler doesn't support string equal sign "=" comparison yet, so any existing customers' UDFs that contain such string comparison will either fall back to native Spark UDF calculation (note: currently there is a defect that any exception will fail the entire UDF in UDF-compiler), or need to be rewritten to use supported expression (String.equal() function is supported).

For Spark UDF performance measurement, it can be divided into two categories:
  - “standard” benchmarks – especially from tpc series
    - [tpcx-bb](http://tpc.org/tpcx-bb/default5.asp): UDFs are clearly defined and separated, and normally take 1/3 of the total query (total 30 queries) time. However,
      - It has strong dependency on Cloudera products
      - It has many assumptions like Spark worker nodes coexist with storage HDFS data nodes (Spark and storage use the same yarn for control etc.), this might limit our NDP measurement
      - The UDFs in current form are not translatable by the ufd-compiler
      - It is still valuable in future test
    - [tpch](http://tpc.org/tpch/default5.asp): it has some UDFs, but they are optional, e.g. [Databricks’s queries replace all UDFs with expressions](https://github.com/databricks/spark-sql-perf), we need explore more in the future.
  - “custom” benchmarks – based on real custom use cases, for example,
    - [SQL Macros tax and discount calculation with retails](https://github.com/hbutani/spark-sql-macros#larger-exampletaxrate-calculation)
    - [Facebook hive UDFs migration](https://www.youtube.com/watch?v=wnZlLRMsNDY)
    - [Informatica Expression Language Functions](https://databricks.com/session_eu20/optimizing-apache-spark-udfs)
    
Since it seems too early to measure the translation solution against “standard benchmarks, considering solution like udf-compiler is still under development, it cannot support most of existing UDFs as they are in existing “standard” benchmarks, it has been decided to use ‘custom’ benchmark first to show the performance improvement of UDF translation. The SQL Macros tax and discount calculation in retails is used (adapted to support udf-compiler) in this experiment because of its availability.

## How to measure UDF performance using udf-compiler and tax calculation UDF
### Steps to prepare test bed and data
#### Step 1: Get the latest Caerus UDF Compiler code and build udf project
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
#### Step 2: Build Spark UDF Example Jar
```
root@ubuntu1804:/home/ubuntu/openinfralabs/caerus-udf/examples/spark-udf# mvn clean package
root@ubuntu1804:/home/ubuntu/openinfralabs/caerus-udf/examples/spark-udf# ls -la target/
...
-rw-r--r-- 1 root root 8293 Aug 25 09:02 spark-udf-1.0-SNAPSHOT.jar
root@ubuntu1804:/home/ubuntu/openinfralabs/caerus-udf/examples/spark-udf#
```
#### Step 3: set up compute and storage clusters
In our experiment, we set up:
- a compute cluster to include 3 Spark VMs nodes (one master and two workers) on one server
- a storage cluster to include 3 HDFS VMs nodes (one name node that has yarn, two data nodes) on another server
- the data generation tool (modified from the SQL Macros) to solve the Spark OOM issues etc. 
- the data generation tool can generate any scale factors as needed, the data sizes are as follows:
    ```
    root@yong1:~#  hdfs dfs -du -s -h /testData10MRecords.parquet
    42.9 M  /testData10MRecords.parquet
    root@yong1:~#  hdfs dfs -du -s -h /testData100MRecords.parquet
    428.8 M  /testData100MRecords.parquet
    root@yong1:~# hdfs dfs -du -s -h /testData1BRecords.parquet
    4.2 G  /testData1BRecords.parquet
    ```
Run the following command on a Spark node (e.g. master) to generate data:
```
spark-submit --class org.openinfralabs.caerus.examples.DataGen --master spark://10.124.48.60:7077 spark-udf-1.0-SNAPSHOT.jar
```
### Steps to run the performance tests
#### Step 1-2: same as above
#### Step 3: run before-after and take the spark time from the log 
**Before - Spark native UDF**
```
root@master:~/caerus-udf/examples/spark-udf# spark-submit --class org.openinfralabs.caerus.examples.SubmitExampleTaxDiscountUDF --master spark://10.124.48.60:7077 --driver-memory 5g target/spark-udf-1.0-SNAPSHOT.jar
........
21/09/24 14:56:05 INFO storage.BlockManagerMasterEndpoint: Trying to remove executor 10 from BlockManagerMaster.
21/09/24 14:56:05 INFO client.StandaloneAppClient$ClientEndpoint: Executor updated: app-20210924145549-0023/11 is now RUNNING
Time taken: 9314 ms
21/09/24 14:58:13 INFO datasources.FileSourceStrategy: Output Data Schema: struct<prod: string, amt: double>
== Parsed Logical Plan ==
'Project ['prod, unresolvedalias('taxAndDiscountF('prod, 'amt), None)]
+- 'Filter ('taxAndDiscountF('prod, 'amt) > 50)
   +- 'UnresolvedRelation [global_temp, products], [], false

== Analyzed Logical Plan ==
prod: string, taxAndDiscountF(prod, amt): double
Project [prod#0, if (isnull(amt#2)) null else taxAndDiscountF(prod#0, knownnotnull(amt#2)) AS taxAndDiscountF(prod, amt)#17]
+- Filter (if (isnull(amt#2)) null else taxAndDiscountF(prod#0, knownnotnull(amt#2)) > cast(50 as double))
   +- SubqueryAlias global_temp.products
      +- Relation[prod#0,prodCat#1,amt#2] parquet

== Optimized Logical Plan ==
Project [prod#0, if (isnull(amt#2)) null else taxAndDiscountF(prod#0, knownnotnull(amt#2)) AS taxAndDiscountF(prod, amt)#17]
+- Filter (if (isnull(amt#2)) null else taxAndDiscountF(prod#0, knownnotnull(amt#2)) > 50.0)
   +- Relation[prod#0,prodCat#1,amt#2] parquet

== Physical Plan ==
*(1) Project [prod#0, if (isnull(amt#2)) null else taxAndDiscountF(prod#0, knownnotnull(amt#2)) AS taxAndDiscountF(prod, amt)#17]
+- *(1) Filter (if (isnull(amt#2)) null else taxAndDiscountF(prod#0, knownnotnull(amt#2)) > 50.0)
   +- *(1) ColumnarToRow
      +- FileScan parquet [prod#0,amt#2] Batched: true, DataFilters: [(if (isnull(amt#2)) null else taxAndDiscountF(prod#0, knownnotnull(amt#2)) > 50.0)], Format: Parquet, Location: InMemoryFileIndex[hdfs://10.124.48.67:9000/testData10MRecords.parquet], PartitionFilters: [], PushedFilters: [], ReadSchema: struct<prod:string,amt:double>
```
**After - compiler-udf for auto translation**
```
root@master:~/caerus-udf/examples/spark-udf# spark-submit --class org.openinfralabs.caerus.examples.SubmitExampleTaxDiscountUDF --master spark://10.124.48.60:7077 --driver-memory 5g --driver-class-path /root/caerus-spark-udf-compiler-from-rapids/udf-compiler/target/rapids-4-spark-udf_2.12-21.10.0-SNAPSHOT.jar --conf "spark.sql.extensions"="com.nvidia.spark.udf.Plugin" target/spark-udf-1.0-SNAPSHOT.jar

21/09/24 14:51:26 INFO client.StandaloneAppClient$ClientEndpoint: Executor updated: app-20210924145111-0021/10 is now RUNNING
Time taken: 5692 ms
21/09/24 14:54:23 INFO datasources.FileSourceStrategy: Post-Scan Filters: isnotnull(amt#2),(((amt#2 * (1.0 - if (NOT (cast((prod#0 <=> alcohol) as int) = 0)) 0.05 else 0.0)) * (1.0 + if (NOT (cast((prod#0 <=> grocery) as int) = 0)) 0.0 else if (((cast((prod#0 <=> grocery) as int) = 0) AND NOT (cast((prod#0 <=> alcohol) as int) = 0))) 10.5 else 9.5)) > 50.0)
21/09/24 14:54:23 INFO datasources.FileSourceStrategy: Output Data Schema: struct<prod: string, amt: double>
== Parsed Logical Plan ==
'Project ['prod, unresolvedalias('taxAndDiscountF('prod, 'amt), None)]
+- 'Filter ('taxAndDiscountF('prod, 'amt) > 50)
   +- 'UnresolvedRelation [global_temp, products], [], false

== Analyzed Logical Plan ==
prod: string, taxAndDiscountF(prod, amt): double
Project [prod#0, ((amt#2 * (1.0 - if (((((((cast((prod#0 <=> grocery) as int) = 0) AND (cast((prod#0 <=> alcohol) as int) = 0)) OR ((cast((prod#0 <=> grocery) as int) = 0) AND NOT (cast((prod#0 <=> alcohol) as int) = 0))) OR NOT (cast((prod#0 <=> grocery) as int) = 0)) AND NOT NOT (dayofmonth(cast(to_timestamp(2021-02-01 08:59:12, Some(yyyy-MM-dd HH:mm:ss)) as date)) = 1)) AND NOT (cast((prod#0 <=> alcohol) as int) = 0))) 0.05 else 0.0)) * (1.0 + if (NOT (cast((prod#0 <=> grocery) as int) = 0)) 0.0 else if (((cast((prod#0 <=> grocery) as int) = 0) AND NOT (cast((prod#0 <=> alcohol) as int) = 0))) 10.5 else 9.5)) AS taxAndDiscountF(prod, amt)#17]
+- Filter (((amt#2 * (1.0 - if (((((((cast((prod#0 <=> grocery) as int) = 0) AND (cast((prod#0 <=> alcohol) as int) = 0)) OR ((cast((prod#0 <=> grocery) as int) = 0) AND NOT (cast((prod#0 <=> alcohol) as int) = 0))) OR NOT (cast((prod#0 <=> grocery) as int) = 0)) AND NOT NOT (dayofmonth(cast(to_timestamp(2021-02-01 08:59:12, Some(yyyy-MM-dd HH:mm:ss)) as date)) = 1)) AND NOT (cast((prod#0 <=> alcohol) as int) = 0))) 0.05 else 0.0)) * (1.0 + if (NOT (cast((prod#0 <=> grocery) as int) = 0)) 0.0 else if (((cast((prod#0 <=> grocery) as int) = 0) AND NOT (cast((prod#0 <=> alcohol) as int) = 0))) 10.5 else 9.5)) > cast(50 as double))
   +- SubqueryAlias global_temp.products
      +- Relation[prod#0,prodCat#1,amt#2] parquet

== Optimized Logical Plan ==
Project [prod#0, ((amt#2 * (1.0 - if (NOT (cast((prod#0 <=> alcohol) as int) = 0)) 0.05 else 0.0)) * (1.0 + if (NOT (cast((prod#0 <=> grocery) as int) = 0)) 0.0 else if (((cast((prod#0 <=> grocery) as int) = 0) AND NOT (cast((prod#0 <=> alcohol) as int) = 0))) 10.5 else 9.5)) AS taxAndDiscountF(prod, amt)#17]
+- Filter (isnotnull(amt#2) AND (((amt#2 * (1.0 - if (NOT (cast((prod#0 <=> alcohol) as int) = 0)) 0.05 else 0.0)) * (1.0 + if (NOT (cast((prod#0 <=> grocery) as int) = 0)) 0.0 else if (((cast((prod#0 <=> grocery) as int) = 0) AND NOT (cast((prod#0 <=> alcohol) as int) = 0))) 10.5 else 9.5)) > 50.0))
   +- Relation[prod#0,prodCat#1,amt#2] parquet

== Physical Plan ==
*(1) Project [prod#0, ((amt#2 * (1.0 - if (NOT (cast((prod#0 <=> alcohol) as int) = 0)) 0.05 else 0.0)) * (1.0 + if (NOT (cast((prod#0 <=> grocery) as int) = 0)) 0.0 else if (((cast((prod#0 <=> grocery) as int) = 0) AND NOT (cast((prod#0 <=> alcohol) as int) = 0))) 10.5 else 9.5)) AS taxAndDiscountF(prod, amt)#17]
+- *(1) Filter (isnotnull(amt#2) AND (((amt#2 * (1.0 - if (NOT (cast((prod#0 <=> alcohol) as int) = 0)) 0.05 else 0.0)) * (1.0 + if (NOT (cast((prod#0 <=> grocery) as int) = 0)) 0.0 else if (((cast((prod#0 <=> grocery) as int) = 0) AND NOT (cast((prod#0 <=> alcohol) as int) = 0))) 10.5 else 9.5)) > 50.0))
   +- *(1) ColumnarToRow
      +- FileScan parquet [prod#0,amt#2] Batched: true, DataFilters: [isnotnull(amt#2), (((amt#2 * (1.0 - if (NOT (cast((prod#0 <=> alcohol) as int) = 0)) 0.05 else 0..., Format: Parquet, Location: InMemoryFileIndex[hdfs://10.124.48.67:9000/testData10MRecords.parquet], PartitionFilters: [], PushedFilters: [IsNotNull(amt)], ReadSchema: struct<prod:string,amt:double>
```
### Preliminary results
- 10 million records (parquet file with 42M data size on HDFS) were used for performance measurement as an example
- Measurement:
  - **Before** (Spark native UDF): 3 repeat measurements in ms: 9314, 9593, 9738 **average 9548**
  - **After** (udf-compiler): 3 repeat measurements in ms: 5692, 5942, 5516 **average 5716**
  - **Improvement of UDF translation: 167%**
- ***NDP pushdown is not tested yet, will work on the datasource and storage next***
- ***Larger dataset, 100m and 1b records etc., cause Spark to throw OOM exception, need further investigation on Spark settings and/or cluster setup*** 
- ***Continue to identify other datasets/queries for Spark UDF translation***  
