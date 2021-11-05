# Caerus Spark UDF Performance Improvement Related to Network/Storage I/O Reduction

## Introduction

For certain data sources that Spark natively support, like Parquet (columnar data with stats metadata), they can be optimized for Spark QL query performance improvements by only transporting needed data from storage to compute. The three major categories are:
  - **Predicate/filter pushdown**: Spark can evaluate filtering predicates in the query against metadata stored in the columnar (Parquet) files, to skip reading chunks of data if the provided filter predicate value in the query is outside the range of values stored for a given column.
  - **Partition pruning**: when data is partitioned by certain patterns (like based on zip code of IoT data etc.), Spark can read data from a list of partitions, based on a filter on the partition key, skipping the rest. 
  - **Column projection**: Spark can just read the data for columns that the query needs to process and skip the rest of the data.

Among these three, when Spark UDFs are involved in the query, **Partition pruning** and **Column projection** can always work, because the final query plan to include the UDF calculation will ask for certain partition or/adn columns, Spark can naturally take advantage these features.

However, this is not the case for predicate/filter pushdown of UDFs, when a native Spark UDF (blackbox or opaque) is used in the predicate (Where clause in SQL), Spark Catalyst can't natively translate the UDFs, thus in the final physical plan of a query will have empty PushedFilters (to tell datasource for pushdown) list. As a result, Spark compute side has to pull all the data, then filter on the compute side, this causes unnecessary I/Os (read data from storage/drives and transport data via network from storage to compute).

Via UDF compiler translation of UDFs, we hope the PushedFilters list will be formed naturally in the physical plan, so that Spark can use the stats in the Parquet file for predicate pushdown. This should speed up query in time spent and I/O reduction.

Other than UDF translation, predicate pushdown also has special requirements on the source data and NDP capability in storage (see below). 

To summerize, in order for Spark to pushdown predicate/filter, it must meet following conditions
  - Physical plan must contain PushedFilters
  - Data source must support pushdown – for native data source, only columnar type data sources like parquet support this operation
  - Data source must be ‘filterable’  --- min-max etc, see parquet-cli tool results , data transformation (ETL) might be needed before query
  - Any calculations beyond basic stats in the data source cannot be pushed down to storage, unless a custom datasource is implemented on Spark side and a NDP is implemented on the storage side. e.g. Caerus NDP, and certain database engines.  

For generic introduction of Spark UDF performance, please see ![Caerus Spark UDF Performance Improvement](README_SPARK_UDF_PERFORMANCE.md) for more details

## How to measure UDF performance using udf-compiler and tax calculation UDF for I/O reduction
### Steps to prepare test bed and data
#### Step 1 - 3: Same as before, see ![Caerus Spark UDF Performance Improvement](README_SPARK_UDF_PERFORMANCE.md) for more details

#### Step 4: clone and build SparkMeasure from CERN (the European Organization for Nuclear Research) github
```
> git clone git@github.com:cerndb/sparkMeasure.git
> sbt clean package
```
#### Step 5: Transform parquet data into 'ordered' data (for different sizes of data, 10 million, 100 million, 1 billion and 10 billion rows of data).
Note: for 10 billion row of data, the transformation can take up to 3 hours
```
spark-submit --class org.openinfralabs.caerus.examples.DataGenOrdered --master spark://10.124.48.60:7077 spark-udf-1.0-SNAPSHOT.jar
```

#### Step 6: Verify data are ordered
 - Build and Install parquet-cli tool: https://github.com/apache/parquet-mr
 - Run following commands (should see row groups with ordered data in different range or min/max)
 - Ordered parquet data is much smaller than non-ordered data when the data cardinality is low (7.2G vs 42G for 10 billion row of data), this is due to parquet encoding scheme (dictionary)
```
root@yong1:~/parquet-mr/parquet-cli# hdfs dfs -ls /testData10BRecords_OrderedByAmt1.parquet
Found 2 items
-rw-r--r--   1 root supergroup          0 2021-11-04 19:19 /testData10BRecords_OrderedByAmt1.parquet/_SUCCESS
-rw-r--r--   1 root supergroup 7733986926 2021-11-04 19:19 /testData10BRecords_OrderedByAmt1.parquet/part-00000-2da22ae5-7046-4ac8-922c-e0c2573287fd-c000.snappy.parquet
root@yong1:~/parquet-mr/parquet-cli# hdfs dfs -du -s -h /testData10BRecords_OrderedByAmt1.parquet
7.2 G  /testData10BRecords_OrderedByAmt1.parquet
root@yong1:~/parquet-mr/parquet-cli# hdfs dfs -du -s -h /testData10BRecords.parquet
41.9 G  /testData10BRecords.parquet

root@yong1:~/parquet-mr/parquet-cli# hadoop jar target/parquet-cli-1.13.0-SNAPSHOT-runtime.jar org.apache.parquet.cli.Main meta hdfs://10.124.48.67:9000/testData10BRecords_OrderedByAmt1.parquet/part-00000-2da22ae5-7046-4ac8-922c-e0c2573287fd-c000.snappy.parquet

File path:  hdfs://10.124.48.67:9000/testData10BRecords_OrderedByAmt1.parquet/part-00000-2da22ae5-7046-4ac8-922c-e0c2573287fd-c000.snappy.parquet
Created by: parquet-mr version 1.10.1 (build a89df8f9932b6ef6633d06069e50c9b7970bebd1)
Properties:
                   org.apache.spark.version: 3.1.2
  org.apache.spark.sql.parquet.row.metadata: {"type":"struct","fields":[{"name":"prod","type":"string","nullable":true,"metadata":{}},{"name":"prodCat","type":"string","nullable":true,"metadata":{}},{"name":"amt","type":"double","nullable":true,"metadata":{}}]}
Schema:
message spark_schema {
  optional binary prod (STRING);
  optional binary prodCat (STRING);
  optional double amt;
}


Row group 0:  count: 185596881  0.71 B records  start: 4  total: 125.497 MB
--------------------------------------------------------------------------------
         type      encodings count     avg size   nulls   min / max
prod     BINARY    S _ R     185596881 0.69 B     0       "p_0" / "p_999807"
prodCat  BINARY    S _ R     185596881 0.02 B     0       "alcohol" / "rest"
amt      DOUBLE    S _ R     185596881 0.00 B     0       "-0.0" / "7.0"

Row group 1:  count: 168113967  0.78 B records  start: 134217728  total: 125.702 MB
--------------------------------------------------------------------------------
         type      encodings count     avg size   nulls   min / max
prod     BINARY    S _ R     168113967 0.77 B     0       "p_100012" / "p_999814"
prodCat  BINARY    S _ R     168113967 0.02 B     0       "alcohol" / "rest"
amt      DOUBLE    S _ R     168113967 0.00 B     0       "7.0" / "15.0"

Row group 2:  count: 182558448  0.72 B records  start: 268435456  total: 125.512 MB
--------------------------------------------------------------------------------
         type      encodings count     avg size   nulls   min / max
prod     BINARY    S _ R     182558448 0.70 B     0       "p_100015" / "p_999821"
prodCat  BINARY    S _ R     182558448 0.02 B     0       "alcohol" / "rest"
amt      DOUBLE    S _ R     182558448 0.00 B     0       "15.0" / "21.0"

Row group 3:  count: 166701913  0.79 B records  start: 402653184  total: 125.707 MB
--------------------------------------------------------------------------------
         type      encodings count     avg size   nulls   min / max
prod     BINARY    S _ R     166701913 0.77 B     0       "p_100022" / "p_999828"
prodCat  BINARY    S _ R     166701913 0.02 B     0       "alcohol" / "rest"
amt      DOUBLE    S _ R     166701913 0.00 B     0       "21.0" / "29.0"

Row group 4:  count: 184394203  0.72 B records  start: 536870912  total: 125.759 MB
--------------------------------------------------------------------------------
         type      encodings count     avg size   nulls   min / max
prod     BINARY    S _ R     184394203 0.70 B     0       "p_100029" / "p_999836"
prodCat  BINARY    S _ R     184394203 0.02 B     0       "alcohol" / "rest"
amt      DOUBLE    S _ R     184394203 0.00 B     0       "29.0" / "36.0"

Row group 5:  count: 167486500  0.79 B records  start: 671088640  total: 126.079 MB
--------------------------------------------------------------------------------
         type      encodings count     avg size   nulls   min / max
prod     BINARY    S _ R     167486500 0.77 B     0       "p_100037" / "p_999844"
prodCat  BINARY    S _ R     167486500 0.02 B     0       "alcohol" / "rest"
amt      DOUBLE    S _ R     167486500 0.00 B     0       "36.0" / "44.0"

.........
```
### Steps to run the performance tests
#### run before-after and take the spark time from the log 
**Before - Spark native UDF**
```
root@master:~/caerus-udf/examples/spark-udf# vi src/main/scala/org.openinfralabs.caerus.examples.spark-udf/submit-example-tax-discount-udf-io.scala
root@master:~/caerus-udf/examples/spark-udf# mvn clean package
root@master:~/caerus-udf/examples/spark-udf# spark-submit --jars /root/sparkMeasure/target/scala-2.12/spark-measure_2.12-0.18-SNAPSHOT.jar --class org.openinfralabs.caerus.examples.SubmitExampleTaxDiscountUDFIO --master spark://10.124.48.60:7077 --driver-memory 5g target/spark-udf-1.0-SNAPSHOT.jar
....
21/11/05 11:18:14 INFO datasources.FileSourceStrategy: Output Data Schema: struct<prod: string, amt: double>
== Parsed Logical Plan ==
'Project ['prod, unresolvedalias('taxAndDiscountF('prod, 'amt), None)]
+- 'Filter ('amtUDF('amt) < 5.0)
   +- 'UnresolvedRelation [global_temp, products], [], false

== Analyzed Logical Plan ==
prod: string, taxAndDiscountF(prod, amt): double
Project [prod#0, if (isnull(amt#2)) null else taxAndDiscountF(prod#0, knownnotnull(amt#2)) AS taxAndDiscountF(prod, amt)#19]
+- Filter (if (isnull(amt#2)) null else amtUDF(knownnotnull(amt#2)) < cast(5.0 as double))
   +- SubqueryAlias global_temp.products
      +- Relation[prod#0,prodCat#1,amt#2] parquet

== Optimized Logical Plan ==
Project [prod#0, if (isnull(amt#2)) null else taxAndDiscountF(prod#0, knownnotnull(amt#2)) AS taxAndDiscountF(prod, amt)#19]
+- Filter (if (isnull(amt#2)) null else amtUDF(knownnotnull(amt#2)) < 5.0)
   +- Relation[prod#0,prodCat#1,amt#2] parquet

== Physical Plan ==
*(1) Project [prod#0, if (isnull(amt#2)) null else taxAndDiscountF(prod#0, knownnotnull(amt#2)) AS taxAndDiscountF(prod, amt)#19]
+- *(1) Filter (if (isnull(amt#2)) null else amtUDF(knownnotnull(amt#2)) < 5.0)
   +- *(1) ColumnarToRow
      +- FileScan parquet [prod#0,amt#2] Batched: true, DataFilters: [(if (isnull(amt#2)) null else amtUDF(knownnotnull(amt#2)) < 5.0)], Format: Parquet, Location: InMemoryFileIndex[hdfs://10.124.48.67:9000/testData10BRecords_OrderedByAmt1.parquet], PartitionFilters: [], PushedFilters: [], ReadSchema: struct<prod:string,amt:double>

21/11/05 11:18:14 INFO storage.BlockManagerInfo: Removed broadcast_2_piece0 on master:35263 in memory (size: 7.0 KiB, free: 2.8 GiB)
...
21/11/05 11:18:21 INFO scheduler.DAGScheduler: Job 2 finished: count at submit-example-tax-discount-udf-io.scala:59, took 6.171050 s
250000000Time taken: 6320 ms
Time taken: 6321 ms
....
21/11/05 11:18:21 INFO codegen.CodeGenerator: Code generated in 12.325683 ms

Scheduling mode = FIFO
Spark Context default degree of parallelism = 8
Aggregated Spark stage metrics:
numStages => 2
numTasks => 59
elapsedTime => 6164 (6 s)
stageDuration => 6157 (6 s)
executorRunTime => 43434 (43 s)
executorCpuTime => 26171 (26 s)
executorDeserializeTime => 1287 (1 s)
executorDeserializeCpuTime => 431 (0.4 s)
resultSerializationTime => 18 (18 ms)
jvmGCTime => 718 (0.7 s)
shuffleFetchWaitTime => 0 (0 ms)
shuffleWriteTime => 59 (59 ms)
resultSize => 129774 (126.0 KB)
diskBytesSpilled => 0 (0 Bytes)
memoryBytesSpilled => 0 (0 Bytes)
peakExecutionMemory => 0
recordsRead => 10000000000
bytesRead => 7978193 (7.0 MB)
recordsWritten => 0
bytesWritten => 0 (0 Bytes)
shuffleRecordsRead => 58
shuffleTotalBlocksFetched => 58
shuffleLocalBlocksFetched => 25
shuffleRemoteBlocksFetched => 33
shuffleTotalBytesRead => 3259 (3.0 KB)
shuffleLocalBytesRead => 1411 (1411 Bytes)
shuffleRemoteBytesRead => 1848 (1848 Bytes)
shuffleRemoteBytesReadToDisk => 0 (0 Bytes)
shuffleBytesWritten => 3259 (3.0 KB)
shuffleRecordsWritten => 58
```

**After - udf-compiler for auto translation**
```
21/11/05 11:26:09 INFO datasources.FileSourceStrategy: Output Data Schema: struct<prod: string, amt: double>
== Parsed Logical Plan ==
'Project ['prod, unresolvedalias('taxAndDiscountF('prod, 'amt), None)]
+- 'Filter ('amtUDF('amt) < 5.0)
   +- 'UnresolvedRelation [global_temp, products], [], false

== Analyzed Logical Plan ==
prod: string, taxAndDiscountF(prod, amt): double
Project [prod#0, ((amt#2 * (1.0 - if (((((((cast((prod#0 <=> grocery) as int) = 0) AND (cast((prod#0 <=> alcohol) as int) = 0)) OR ((cast((prod#0 <=> grocery) as int) = 0) AND NOT (cast((prod#0 <=> alcohol) as int) = 0))) OR NOT (cast((prod#0 <=> grocery) as int) = 0)) AND NOT NOT (dayofmonth(cast(to_timestamp(2021-02-01 08:59:12, Some(yyyy-MM-dd HH:mm:ss)) as date)) = 1)) AND NOT (cast((prod#0 <=> alcohol) as int) = 0))) 0.05 else 0.0)) * (1.0 + if (NOT (cast((prod#0 <=> grocery) as int) = 0)) 0.0 else if (((cast((prod#0 <=> grocery) as int) = 0) AND NOT (cast((prod#0 <=> alcohol) as int) = 0))) 10.5 else 9.5)) AS taxAndDiscountF(prod, amt)#19]
+- Filter (amt#2 < cast(5.0 as double))
   +- SubqueryAlias global_temp.products
      +- Relation[prod#0,prodCat#1,amt#2] parquet

== Optimized Logical Plan ==
Project [prod#0, ((amt#2 * (1.0 - if (NOT (cast((prod#0 <=> alcohol) as int) = 0)) 0.05 else 0.0)) * (1.0 + if (NOT (cast((prod#0 <=> grocery) as int) = 0)) 0.0 else if (((cast((prod#0 <=> grocery) as int) = 0) AND NOT (cast((prod#0 <=> alcohol) as int) = 0))) 10.5 else 9.5)) AS taxAndDiscountF(prod, amt)#19]
+- Filter (isnotnull(amt#2) AND (amt#2 < 5.0))
   +- Relation[prod#0,prodCat#1,amt#2] parquet

== Physical Plan ==
*(1) Project [prod#0, ((amt#2 * (1.0 - if (NOT (cast((prod#0 <=> alcohol) as int) = 0)) 0.05 else 0.0)) * (1.0 + if (NOT (cast((prod#0 <=> grocery) as int) = 0)) 0.0 else if (((cast((prod#0 <=> grocery) as int) = 0) AND NOT (cast((prod#0 <=> alcohol) as int) = 0))) 10.5 else 9.5)) AS taxAndDiscountF(prod, amt)#19]
+- *(1) Filter (isnotnull(amt#2) AND (amt#2 < 5.0))
   +- *(1) ColumnarToRow
      +- FileScan parquet [prod#0,amt#2] Batched: true, DataFilters: [isnotnull(amt#2), (amt#2 < 5.0)], Format: Parquet, Location: InMemoryFileIndex[hdfs://10.124.48.67:9000/testData10BRecords_OrderedByAmt1.parquet], PartitionFilters: [], PushedFilters: [IsNotNull(amt), LessThan(amt,5.0)], ReadSchema: struct<prod:string,amt:double>

21/11/05 11:26:10 INFO datasources.FileSourceStrategy: Pushed Filters: IsNotNull(amt),LessThan(amt,5.0)
.....
21/11/05 11:26:12 INFO scheduler.DAGScheduler: Job 2 finished: count at submit-example-tax-discount-udf-io.scala:59, took 2.073311 s
250000000Time taken: 2180 ms
Time taken: 2180 ms

.....
21/11/05 11:26:13 INFO codegen.CodeGenerator: Code generated in 13.850167 ms

Scheduling mode = FIFO
Spark Context default degree of parallelism = 8
Aggregated Spark stage metrics:
numStages => 2
numTasks => 59
elapsedTime => 2067 (2 s)
stageDuration => 2060 (2 s)
executorRunTime => 8014 (8 s)
executorCpuTime => 4582 (5 s)
executorDeserializeTime => 381 (0.4 s)
executorDeserializeCpuTime => 217 (0.2 s)
resultSerializationTime => 1 (1 ms)
jvmGCTime => 178 (0.2 s)
shuffleFetchWaitTime => 0 (0 ms)
shuffleWriteTime => 36 (36 ms)
resultSize => 125302 (122.0 KB)
diskBytesSpilled => 0 (0 Bytes)
memoryBytesSpilled => 0 (0 Bytes)
peakExecutionMemory => 0
recordsRead => 662213581
bytesRead => 2428777 (2.0 MB)
recordsWritten => 0
bytesWritten => 0 (0 Bytes)
shuffleRecordsRead => 58
shuffleTotalBlocksFetched => 58
shuffleLocalBlocksFetched => 27
shuffleRemoteBlocksFetched => 31
shuffleTotalBytesRead => 3259 (3.0 KB)
shuffleLocalBytesRead => 1515 (1515 Bytes)
shuffleRemoteBytesRead => 1744 (1744 Bytes)
shuffleRemoteBytesReadToDisk => 0 (0 Bytes)
shuffleBytesWritten => 3259 (3.0 KB)
shuffleRecordsWritten => 58
21/11/05 11:26:13 INFO server.AbstractConnector: Stopped Spark@6c931d35{HTTP/1.1, (http/1.1)}{0.0.0.0:4040}

```
### Preliminary results
*Caerus UDF-compiler translation has up to 3.3x Network I/O Reduction than Spark native UDF*

Raw data:
 - Before (Spark native UDF): 6321 ms query time, 7978193 bytes read
 - After (UDF Compilation): 2180 ms query time, 2428777 bytes read
 - I/O Reduction: 7978193/2428777 = 3.3x
 - Query time Speed up: 6321/2180 = 2.8x
