# Caerus Spark UDF Performance Improvement (Time and I/O Reduction) with TPCH Tests

## Introduction

In this generic introduction of Spark UDF performance: [Caerus Spark UDF Performance Improvement](README_SPARK_UDF_PERFORMANCE.md), we mentioned ideally we wanted to use standard test benchmarks like TPCH, TPCBB etc. to measure Caerus UDF compiler improvement both in query time and network/storage I/O reduction.

Since UDF Compiler is still in early stage of development, more features need to be added to use as it is for generic benchmark testing, but we can still make an effort to use such standard benchmark tests, by strictly following the test definition, to demonstrate Caerus UDF Compiler indeed can significantly speed up query time, while reduce the I/O reduction from both network (storage to compute), storage (from drive storage to storage controller). This document is to illustrate the procedures and results for such effort.

We decided to pick certain tests (Q06) from TPCH with implementation of UDFs within the tests to prove that the Caerus UDF Compiler solution can achieve time saving and I/O reduction.

The UDF design in Q06 is loosely following the test design from this paper (Brown Univeristy: Tuplex): https://cs.brown.edu/people/malte/pub/papers/2021-sigmod-tuplex.pdf 

The Caerus UDF Compiler can achieve query time saving and I/O reduction via two aspects:
- Spark compute side:
    - Comipler can auto translate UDF (if there is portion of UDF that is translatable) into Spark Catalyst expressions.
    - These expressions then can take advantage the optimization that Spark Catalyst implemented, such as WholeStageCodegen etc. 
    - These expression are also part of Spark query plans that can support operations like predicate/filter pushdown  
- Data source side (NDP support):
    - Ceratin data sources natively have NDP support like parquet files contain stats of range, min-max etc. so that Spark client can do data skipping based on the stats
    - Other NDP support, like Caerus NDP, database NDP etc.

So for the TPCH Q06 test, we will need to do two things:
- Spark side: make certain UDFs within the Q06 test (like wrap shipdate into a UDF), these UDFs are also part of the filter operation. To make sure the UDF indeed has correct translation and query plan (to pushdown)
- Data source side: using Spark to do ETL to the original generated TPCH data that is in the text form (CSV files), change to ordered parquet files. The ordered parquet file (lineitem data) is currently ordered by the shipdate, so that Q06 can achieve the I/O reduction   


## How to measure UDF performance and I/O reduction for TPCH tests (pick Q06 as an example)
### Steps to prepare test bed and data
### Step 1: set up compute and storage clusters
In our experiment, we set up:
- a compute cluster to include 3 Spark VMs nodes (one master and two workers) on one server
- a storage cluster to include 3 HDFS VMs nodes (one name node and two data nodes) on another server
### Step 2: Generate TPCH data using standard tool (Scale is 100 GB with raw text csv format) and upload to HDFS cluster
```
> git clone git@github.com:ssavvides/tpch-spark.git
> cd tpch-spark
> make
> ./dbgen -s 100
> $HADOOP_HOME/bin/hdfs dfs -put -f $LOCAL_DATA_PATH/$fileName/*  ${HDFS_DATA_PATH}/$tableName/
  > hdfs dfs -put -f spark-warehouse/tpch.db/*.tbl hdfs://10.124.48.67:9000/tpch_data100 
```
### Step 3: using Saprk to do ETL to change data to ordered parquet files
To simplify, since Q06 only targets the largest dataset in TPCH "lineitem", so we just make the transformation only on this object file, and we will only order the "shipdate" column in the data. 
Using spark-shell (see example below) or spark-submit to run ETL on the raw csv data
- prepare "lineitem" dataframe: either load or copy-paste the file content of "TpchSchemaProvider.scala" under the above repo: https://github.com/ssavvides/tpch-spark
- write the dataframe to a parquet file
- order the parquet file and save to a new parquet file
```
root@master:~# spark-shell
....
Welcome to
      ____              __
     / __/__  ___ _____/ /__
    _\ \/ _ \/ _ `/ __/  '_/
   /___/ .__/\_,_/_/ /_/\_\   version 3.1.2
      /_/

Using Scala version 2.12.10 (Java HotSpot(TM) 64-Bit Server VM, Java 11.0.11)
Type in expressions to have them evaluated.
Type :help for more information.

scala> :load TpchSchemaProvider.scala 
scala> or copy-paste the file content under saprk-shell
scala> lineitem.write.parquet("hdfs://10.124.48.67:9000/tpch_data100/lineitem100/lineitem.parquet")
scala> val lineitemDF = spark.read.parquet("hdfs://10.124.48.67:9000/tpch_data100/lineitem100/lineitem.parquet")
scala> val orderedDF = lineitemDF.orderBy("l_shipdate")
scala> orderedDF.repartition(1).write.format("parquet").save("hdfs://10.124.48.67:9000/tpch_data100/lineitem101/lineitem_OrderedByShippingDate.parquet")

```
### Step 4: Confirm ordered TPCH data on HDFS with needed stats
For 100GB TPCH data scale, the ordered lineitem parquet file has total 183 equal-sized row groups, where the Q06 targeted range (from 1994-1-1 to 1995-1-1) has total 28 row groups.
So when we use UDF in Q06, when the Caerus UDF compiler is used, Spark will only need read 28 out of 183 row groups of data (different data scale might have different ratios, this ratio is from 100 GB of TPCH dataset), thus to achieve the data reduction.
```
root@yong1:~/parquet-mr/parquet-cli# hadoop jar target/parquet-cli-1.13.0-SNAPSHOT-runtime.jar org.apache.parquet.cli.Main meta  
     hdfs://10.124.48.67:9000/tpch_data100/lineitem101/lineitem_OrderedByShippingDate.parquet/part-00000-0c9206ea-fd04-4579-9dea-f2598b4bd2a8-c000.snappy.parquet

File path:  hdfs://10.124.48.67:9000/tpch_data100/lineitem101/lineitem_OrderedByShippingDate.parquet/part-00000-0c9206ea-fd04-4579-9dea-f2598b4bd2a8-c000.snappy.parquet
Created by: parquet-mr version 1.10.1 (build a89df8f9932b6ef6633d06069e50c9b7970bebd1)
Properties:
                   org.apache.spark.version: 3.1.2
  org.apache.spark.sql.parquet.row.metadata: {"type":"struct","fields":[{"name":"l_orderkey","type":"long","nullable":true,"metadata":{}},{"name":"l_partkey","type":"long","nullable":true,"metadata":{}},{"name":"l_suppkey","type":"long",
"nullable":true,"metadata":{}},{"name":"l_linenumber","type":"long","nullable":true,"metadata":{}},{"name":"l_quantity","type":"double","nullable":true,"metadata":{}},{"name":"l_extendedprice","type":"double","nullable":true,"metadata":{
}},{"name":"l_discount","type":"double","nullable":true,"metadata":{}},{"name":"l_tax","type":"double","nullable":true,"metadata":{}},{"name":"l_returnflag","type":"string","nullable":true,"metadata":{}},{"name":"l_linestatus","type":"st
ring","nullable":true,"metadata":{}},{"name":"l_shipdate","type":"string","nullable":true,"metadata":{}},{"name":"l_commitdate","type":"string","nullable":true,"metadata":{}},{"name":"l_receiptdate","type":"string","nullable":true,"metad
ata":{}},{"name":"l_shipinstruct","type":"string","nullable":true,"metadata":{}},{"name":"l_shipmode","type":"string","nullable":true,"metadata":{}},{"name":"l_comment","type":"string","nullable":true,"metadata":{}}]}
Schema:
message spark_schema {
  optional int64 l_orderkey;
  optional int64 l_partkey;
  optional int64 l_suppkey;
  optional int64 l_linenumber;
  optional double l_quantity;
  optional double l_extendedprice;
  optional double l_discount;
  optional double l_tax;
  optional binary l_returnflag (STRING);
  optional binary l_linestatus (STRING);
  optional binary l_shipdate (STRING);
  optional binary l_commitdate (STRING);
  optional binary l_receiptdate (STRING);
  optional binary l_shipinstruct (STRING);
  optional binary l_shipmode (STRING);
  optional binary l_comment (STRING);
}


Row group 0:  count: 3260100  38.56 B records  start: 4  total: 119.883 MB
--------------------------------------------------------------------------------
                 type      encodings count     avg size   nulls   min / max
l_orderkey       INT64     S   _     3260100   5.06 B     0       "292" / "599999971"
l_partkey        INT64     S   _     3260100   5.44 B     0       "13" / "19999994"
l_suppkey        INT64     S   _     3260100   4.93 B     0       "1" / "1000000"
l_linenumber     INT64     S _ R     3260100   0.38 B     0       "1" / "7"
l_quantity       DOUBLE    S _ R     3260100   0.75 B     0       "1.0" / "50.0"
l_extendedprice  DOUBLE    S   _     3260100   5.76 B     0       "902.02" / "104778.0"
l_discount       DOUBLE    S _ R     3260100   0.50 B     0       "-0.0" / "0.1"
l_tax            DOUBLE    S _ R     3260100   0.50 B     0       "-0.0" / "0.08"
l_returnflag     BINARY    S _ R     3260100   0.13 B     0       "A" / "R"
l_linestatus     BINARY    S _ R     3260100   0.00 B     0       "F" / "F"
l_shipdate       BINARY    S _ R     3260100   0.00 B     0       "1992-01-02" / "1992-02-26"
l_commitdate     BINARY    S _ R     3260100   0.88 B     0       "1992-01-31" / "1992-05-25"
l_receiptdate    BINARY    S _ R     3260100   0.83 B     0       "1992-01-03" / "1992-03-27"
l_shipinstruct   BINARY    S _ R     3260100   0.25 B     0       "COLLECT COD" / "TAKE BACK RETURN"
l_shipmode       BINARY    S _ R     3260100   0.38 B     0       "AIR" / "TRUCK"
l_comment        BINARY    S   _     3260100   12.75 B    0       "! accounts" / "zzle? quic"

......
Row group 51:  count: 3260100  38.43 B records  start: 6392966641  total: 119.482 MB
--------------------------------------------------------------------------------
                 type      encodings count     avg size   nulls   min / max
l_orderkey       INT64     S   _     3260100   4.92 B     0       "195" / "599999845"
l_partkey        INT64     S   _     3260100   5.44 B     0       "10" / "19999999"
l_suppkey        INT64     S   _     3260100   4.93 B     0       "1" / "1000000"
l_linenumber     INT64     S _ R     3260100   0.38 B     0       "1" / "7"
l_quantity       DOUBLE    S _ R     3260100   0.75 B     0       "1.0" / "50.0"
l_extendedprice  DOUBLE    S   _     3260100   5.76 B     0       "903.07" / "104861.5"
l_discount       DOUBLE    S _ R     3260100   0.50 B     0       "-0.0" / "0.1"
l_tax            DOUBLE    S _ R     3260100   0.50 B     0       "-0.0" / "0.08"
l_returnflag     BINARY    S _ R     3260100   0.13 B     0       "A" / "R"
l_linestatus     BINARY    S _ R     3260100   0.00 B     0       "F" / "F"
l_shipdate       BINARY    S _ R     3260100   0.00 B     0       "1993-12-29" / "1994-01-11"
l_commitdate     BINARY    S _ R     3260100   1.00 B     0       "1993-09-29" / "1994-04-10"
l_receiptdate    BINARY    S _ R     3260100   0.73 B     0       "1993-12-30" / "1994-02-10"
l_shipinstruct   BINARY    S _ R     3260100   0.25 B     0       "COLLECT COD" / "TAKE BACK RETURN"
l_shipmode       BINARY    S _ R     3260100   0.38 B     0       "AIR" / "TRUCK"
l_comment        BINARY    S   _     3260100   12.75 B    0       "! Tiresias across the bli..." / "zzle; bold pinto beans sl..."

......
Row group 79:  count: 3266851  38.43 B records  start: 9903099812  total: 119.739 MB
--------------------------------------------------------------------------------
                 type      encodings count     avg size   nulls   min / max
l_orderkey       INT64     S   _     3266851   4.92 B     0       "359" / "599999681"
l_partkey        INT64     S   _     3266851   5.44 B     0       "4" / "19999998"
l_suppkey        INT64     S   _     3266851   4.93 B     0       "1" / "1000000"
l_linenumber     INT64     S _ R     3266851   0.38 B     0       "1" / "7"
l_quantity       DOUBLE    S _ R     3266851   0.75 B     0       "1.0" / "50.0"
l_extendedprice  DOUBLE    S   _     3266851   5.76 B     0       "902.62" / "104855.0"
l_discount       DOUBLE    S _ R     3266851   0.50 B     0       "-0.0" / "0.1"
l_tax            DOUBLE    S _ R     3266851   0.50 B     0       "-0.0" / "0.08"
l_returnflag     BINARY    S _ R     3266851   0.13 B     0       "A" / "R"
l_linestatus     BINARY    S _ R     3266851   0.00 B     0       "F" / "F"
l_shipdate       BINARY    S _ R     3266851   0.00 B     0       "1994-12-30" / "1995-01-12"
l_commitdate     BINARY    S _ R     3266851   1.00 B     0       "1994-09-30" / "1995-04-11"
l_receiptdate    BINARY    S _ R     3266851   0.73 B     0       "1994-12-31" / "1995-02-11"
l_shipinstruct   BINARY    S _ R     3266851   0.25 B     0       "COLLECT COD" / "TAKE BACK RETURN"
l_shipmode       BINARY    S _ R     3266851   0.38 B     0       "AIR" / "TRUCK"
l_comment        BINARY    S   _     3266851   12.75 B    0       "! accounts" / "zzle. unusual foxes haggl..."

.....
Row group 183:  count: 3054795  38.36 B records  start: 22908142332  total: 111.758 MB
--------------------------------------------------------------------------------
                 type      encodings count     avg size   nulls   min / max
l_orderkey       INT64     S   _     3054795   5.07 B     0       "34" / "599999043"
l_partkey        INT64     S   _     3054795   5.44 B     0       "10" / "19999999"
l_suppkey        INT64     S   _     3054795   4.93 B     0       "1" / "1000000"
l_linenumber     INT64     S _ R     3054795   0.38 B     0       "1" / "7"
l_quantity       DOUBLE    S _ R     3054795   0.75 B     0       "1.0" / "50.0"
l_extendedprice  DOUBLE    S   _     3054795   5.76 B     0       "902.46" / "104777.0"
l_discount       DOUBLE    S _ R     3054795   0.50 B     0       "-0.0" / "0.1"
l_tax            DOUBLE    S _ R     3054795   0.50 B     0       "-0.0" / "0.08"
l_returnflag     BINARY    S _ R     3054795   0.00 B     0       "N" / "N"
l_linestatus     BINARY    S _ R     3054795   0.00 B     0       "O" / "O"
l_shipdate       BINARY    S _ R     3054795   0.00 B     0       "1998-10-09" / "1998-12-01"
l_commitdate     BINARY    S _ R     3054795   0.88 B     0       "1998-07-10" / "1998-10-31"
l_receiptdate    BINARY    S _ R     3054795   0.76 B     0       "1998-10-10" / "1998-12-31"
l_shipinstruct   BINARY    S _ R     3054795   0.25 B     0       "COLLECT COD" / "TAKE BACK RETURN"
l_shipmode       BINARY    S _ R     3054795   0.38 B     0       "AIR" / "TRUCK"
l_comment        BINARY    S   _     3054795   12.75 B    0       "! accounts" / "zzle; furiously pending i..."

```
### Step 5: Update the scala files from the src directory of this folder to the tpch-spark repo folder 
Copy Q06.scala, TpchQuery.scala and tpch.sbt to the tpch-spark folder (see Step 2), then build
```
> cd tpch-spark
> sbt package
```
### Step 6: Build Spark UDF Example Jar
```
root@ubuntu1804:/home/ubuntu/openinfralabs/caerus-udf/examples/spark-udf# mvn clean package
root@ubuntu1804:/home/ubuntu/openinfralabs/caerus-udf/examples/spark-udf# ls -la target/
...
-rw-r--r-- 1 root root 8293 Aug 25 09:02 spark-udf-1.0-SNAPSHOT.jar
root@ubuntu1804:/home/ubuntu/openinfralabs/caerus-udf/examples/spark-udf#
```

### Steps to run the performance tests
run before-after and take the spark time from the log 
- **Before - Spark native UDF**

Abstract from below:
- recordsRead => 1200075804
- bytesRead => 8629567890 (8.0 GB)
```
root@master:~/tpch-spark# spark-submit --jars /root/sparkMeasure/target/scala-2.12/spark-measure_2.12-0.18-SNAPSHOT.jar  --class "main.scala.TpchQuery" --master spark://10.124.48.60:7077 target/scala-2.12/spark-tpc-h-queries_2.12-1.0.jar 06
.....
21/12/06 10:15:47 INFO scheduler.DAGScheduler: Job 1 finished: show at TpchQuery.scala:46, took 42.480128 s
21/12/06 10:15:47 INFO codegen.CodeGenerator: Code generated in 9.33116 ms
+--------------------+
|             revenue|
+--------------------+
|1.233042688846365...|
+--------------------+

21/12/06 10:15:47 INFO datasources.FileSourceStrategy: Pushed Filters: IsNotNull(l_quantity),LessThan(l_quantity,24.0)
21/12/06 10:15:47 INFO datasources.FileSourceStrategy: Post-Scan Filters: isnotnull(l_quantity#38),(shipdateUDF(l_shipdate#44) >= 1994-01-01),(shipdateUDF(l_shipdate#44) < 1995-01-01),(if (isnull(l_discount#40)) null else discountUDF(kno
wnnotnull(l_discount#40)) >= 0.05),(if (isnull(l_discount#40)) null else discountUDF(knownnotnull(l_discount#40)) <= 0.07),(l_quantity#38 < 24.0)
21/12/06 10:15:47 INFO datasources.FileSourceStrategy: Output Data Schema: struct<l_quantity: double, l_extendedprice: double, l_discount: double, l_shipdate: string ... 2 more fields>
== Parsed Logical Plan ==
'Project ['sum(('l_extendedprice * 'discountUDF('l_discount))) AS revenue#234]
+- 'Filter ((('shipdateUDF('l_shipdate) >= 1994-01-01) AND ('shipdateUDF('l_shipdate) < 1995-01-01)) AND ((('discountUDF('l_discount) >= 0.05) AND ('discountUDF('l_discount) <= 0.07)) AND ('l_quantity < 24)))
   +- 'UnresolvedRelation [lineitem], [], false

== Analyzed Logical Plan ==
revenue: double
Aggregate [sum((l_extendedprice#39 * if (isnull(l_discount#40)) null else discountUDF(knownnotnull(l_discount#40)))) AS revenue#234]
+- Filter (((shipdateUDF(l_shipdate#44) >= 1994-01-01) AND (shipdateUDF(l_shipdate#44) < 1995-01-01)) AND (((if (isnull(l_discount#40)) null else discountUDF(knownnotnull(l_discount#40)) >= cast(0.05 as double)) AND (if (isnull(l_discoun
t#40)) null else discountUDF(knownnotnull(l_discount#40)) <= cast(0.07 as double))) AND (l_quantity#38 < cast(24 as double))))
   +- SubqueryAlias lineitem
      +- Relation[l_orderkey#34L,l_partkey#35L,l_suppkey#36L,l_linenumber#37L,l_quantity#38,l_extendedprice#39,l_discount#40,l_tax#41,l_returnflag#42,l_linestatus#43,l_shipdate#44,l_commitdate#45,l_receiptdate#46,l_shipinstruct#47,l_ship
mode#48,l_comment#49] parquet

== Optimized Logical Plan ==
Aggregate [sum((l_extendedprice#39 * if (isnull(l_discount#40)) null else discountUDF(knownnotnull(l_discount#40)))) AS revenue#234]
+- Project [l_extendedprice#39, l_discount#40]
   +- Filter (((((isnotnull(l_quantity#38) AND (shipdateUDF(l_shipdate#44) >= 1994-01-01)) AND (shipdateUDF(l_shipdate#44) < 1995-01-01)) AND (if (isnull(l_discount#40)) null else discountUDF(knownnotnull(l_discount#40)) >= 0.05)) AND (i
f (isnull(l_discount#40)) null else discountUDF(knownnotnull(l_discount#40)) <= 0.07)) AND (l_quantity#38 < 24.0))
      +- Relation[l_orderkey#34L,l_partkey#35L,l_suppkey#36L,l_linenumber#37L,l_quantity#38,l_extendedprice#39,l_discount#40,l_tax#41,l_returnflag#42,l_linestatus#43,l_shipdate#44,l_commitdate#45,l_receiptdate#46,l_shipinstruct#47,l_ship
mode#48,l_comment#49] parquet

== Physical Plan ==
*(2) HashAggregate(keys=[], functions=[sum((l_extendedprice#39 * if (isnull(l_discount#40)) null else discountUDF(knownnotnull(l_discount#40))))], output=[revenue#234])
+- Exchange SinglePartition, ENSURE_REQUIREMENTS, [id=#84]
   +- *(1) HashAggregate(keys=[], functions=[partial_sum((l_extendedprice#39 * if (isnull(l_discount#40)) null else discountUDF(knownnotnull(l_discount#40))))], output=[sum#256])
      +- *(1) Project [l_extendedprice#39, l_discount#40]
         +- *(1) Filter (((((isnotnull(l_quantity#38) AND (shipdateUDF(l_shipdate#44) >= 1994-01-01)) AND (shipdateUDF(l_shipdate#44) < 1995-01-01)) AND (if (isnull(l_discount#40)) null else discountUDF(knownnotnull(l_discount#40)) >= 0.
05)) AND (if (isnull(l_discount#40)) null else discountUDF(knownnotnull(l_discount#40)) <= 0.07)) AND (l_quantity#38 < 24.0))
            +- *(1) ColumnarToRow
               +- FileScan parquet [l_quantity#38,l_extendedprice#39,l_discount#40,l_shipdate#44] Batched: true, DataFilters: [isnotnull(l_quantity#38), (shipdateUDF(l_shipdate#44) >= 1994-01-01), (shipdateUDF(l_shipdate#44..., Format: P
arquet, Location: InMemoryFileIndex[hdfs://10.124.48.67:9000/tpch_data100/lineitem101/lineitem_OrderedByShippingDat..., PartitionFilters: [], PushedFilters: [IsNotNull(l_quantity), LessThan(l_quantity,24.0)], ReadSchema: struct<l_quantit
y:double,l_extendedprice:double,l_discount:double,l_shipdate:string>

21/12/06 10:15:48 INFO datasources.FileSourceStrategy: Pushed Filters: IsNotNull(l_quantity),LessThan(l_quantity,24.0)
21/12/06 10:15:48 INFO datasources.FileSourceStrategy: Post-Scan Filters: isnotnull(l_quantity#38),(shipdateUDF(l_shipdate#44) >= 1994-01-01),(shipdateUDF(l_shipdate#44) < 1995-01-01),(if (isnull(l_discount#40)) null else discountUDF(kno
wnnotnull(l_discount#40)) >= 0.05),(if (isnull(l_discount#40)) null else discountUDF(knownnotnull(l_discount#40)) <= 0.07),(l_quantity#38 < 24.0)
......

21/12/06 10:16:29 INFO codegen.CodeGenerator: Code generated in 15.857309 ms

Scheduling mode = FIFO
Spark Context default degree of parallelism = 8
Aggregated Spark stage metrics:
numStages => 4
numTasks => 346
elapsedTime => 83516 (1.4 min)
stageDuration => 83258 (1.4 min)
executorRunTime => 645596 (11 min)
executorCpuTime => 308873 (5.1 min)
executorDeserializeTime => 5438 (5 s)
executorDeserializeCpuTime => 2222 (2 s)
resultSerializationTime => 2 (2 ms)
jvmGCTime => 12553 (13 s)
shuffleFetchWaitTime => 0 (0 ms)
shuffleWriteTime => 192 (0.2 s)
resultSize => 384592 (375.0 KB)
diskBytesSpilled => 0 (0 Bytes)
memoryBytesSpilled => 0 (0 Bytes)
peakExecutionMemory => 0
recordsRead => 1200075804
bytesRead => 8629567890 (8.0 GB)
recordsWritten => 1
bytesWritten => 30 (30 Bytes)
shuffleRecordsRead => 344
shuffleTotalBlocksFetched => 344
shuffleLocalBlocksFetched => 177
shuffleRemoteBlocksFetched => 167
shuffleTotalBytesRead => 19716 (19.0 KB)
shuffleLocalBytesRead => 10145 (9.0 KB)
shuffleRemoteBytesRead => 9571 (9.0 KB)
shuffleRemoteBytesReadToDisk => 0 (0 Bytes)
shuffleBytesWritten => 19716 (19.0 KB)
shuffleRecordsWritten => 344
21/12/06 10:16:29 INFO codegen.CodeGenerator: Code generated in 10.902982 ms

```

- **After - udf-compiler for auto translation**

Abstract from below:
  - recordsRead => 189193816
  - bytesRead => 1536960970 (1465.0 MB)

```
root@master:~/tpch-spark#  spark-submit --class "main.scala.TpchQuery" --jars /root/sparkMeasure/target/scala-2.12/spark-measure_2.12-0.18-SNAPSHOT.jar  
--master spark://10.124.48.60:7077 --driver-class-path 
/root/caerus-spark-udf-compiler-from-rapids/udf-compiler/target/rapids-4-spark-udf_2.12-21.10.0-SNAPSHOT.jar 
--conf "spark.sql.extensions"="com.nvidia.spark.udf.Plugin" target/scala-2.12/spark-tpc-h-queries_2.12-1.0.jar 06

.......
21/12/06 10:19:02 INFO scheduler.DAGScheduler: Job 1 finished: show at TpchQuery.scala:46, took 11.077535 s
21/12/06 10:19:02 INFO codegen.CodeGenerator: Code generated in 8.952901 ms
+--------------------+
|             revenue|
+--------------------+
|1.233042688846366...|
+--------------------+

21/12/06 10:19:02 INFO datasources.FileSourceStrategy: Pushed Filters: IsNotNull(l_shipdate),IsNotNull(l_discount),IsNotNull(l_quantity),GreaterThanOrEqual(l_shipdate,1994-01-01),LessThan(l_shipdate,1995-01-01),GreaterThanOrEqual(l_disco
unt,0.05),LessThanOrEqual(l_discount,0.07),LessThan(l_quantity,24.0)
21/12/06 10:19:02 INFO datasources.FileSourceStrategy: Post-Scan Filters: isnotnull(l_shipdate#44),isnotnull(l_discount#40),isnotnull(l_quantity#38),(l_shipdate#44 >= 1994-01-01),(l_shipdate#44 < 1995-01-01),(l_discount#40 >= 0.05),(l_di
scount#40 <= 0.07),(l_quantity#38 < 24.0)
21/12/06 10:19:02 INFO datasources.FileSourceStrategy: Output Data Schema: struct<l_quantity: double, l_extendedprice: double, l_discount: double, l_shipdate: string ... 2 more fields>
== Parsed Logical Plan ==
'Project ['sum(('l_extendedprice * 'discountUDF('l_discount))) AS revenue#234]
+- 'Filter ((('shipdateUDF('l_shipdate) >= 1994-01-01) AND ('shipdateUDF('l_shipdate) < 1995-01-01)) AND ((('discountUDF('l_discount) >= 0.05) AND ('discountUDF('l_discount) <= 0.07)) AND ('l_quantity < 24)))
   +- 'UnresolvedRelation [lineitem], [], false

== Analyzed Logical Plan ==
revenue: double
Aggregate [sum((l_extendedprice#39 * l_discount#40)) AS revenue#234]
+- Filter (((l_shipdate#44 >= 1994-01-01) AND (l_shipdate#44 < 1995-01-01)) AND (((l_discount#40 >= cast(0.05 as double)) AND (l_discount#40 <= cast(0.07 as double))) AND (l_quantity#38 < cast(24 as double))))
   +- SubqueryAlias lineitem
      +- Relation[l_orderkey#34L,l_partkey#35L,l_suppkey#36L,l_linenumber#37L,l_quantity#38,l_extendedprice#39,l_discount#40,l_tax#41,l_returnflag#42,l_linestatus#43,l_shipdate#44,l_commitdate#45,l_receiptdate#46,l_shipinstruct#47,l_ship
mode#48,l_comment#49] parquet

== Optimized Logical Plan ==
Aggregate [sum((l_extendedprice#39 * l_discount#40)) AS revenue#234]
+- Project [l_extendedprice#39, l_discount#40]
   +- Filter (((((((isnotnull(l_shipdate#44) AND isnotnull(l_discount#40)) AND isnotnull(l_quantity#38)) AND (l_shipdate#44 >= 1994-01-01)) AND (l_shipdate#44 < 1995-01-01)) AND (l_discount#40 >= 0.05)) AND (l_discount#40 <= 0.07)) AND (
l_quantity#38 < 24.0))
      +- Relation[l_orderkey#34L,l_partkey#35L,l_suppkey#36L,l_linenumber#37L,l_quantity#38,l_extendedprice#39,l_discount#40,l_tax#41,l_returnflag#42,l_linestatus#43,l_shipdate#44,l_commitdate#45,l_receiptdate#46,l_shipinstruct#47,l_ship
mode#48,l_comment#49] parquet

== Physical Plan ==
*(2) HashAggregate(keys=[], functions=[sum((l_extendedprice#39 * l_discount#40))], output=[revenue#234])
+- Exchange SinglePartition, ENSURE_REQUIREMENTS, [id=#84]
   +- *(1) HashAggregate(keys=[], functions=[partial_sum((l_extendedprice#39 * l_discount#40))], output=[sum#241])
      +- *(1) Project [l_extendedprice#39, l_discount#40]
         +- *(1) Filter (((((((isnotnull(l_shipdate#44) AND isnotnull(l_discount#40)) AND isnotnull(l_quantity#38)) AND (l_shipdate#44 >= 1994-01-01)) AND (l_shipdate#44 < 1995-01-01)) AND (l_discount#40 >= 0.05)) AND (l_discount#40 <= 0
.07)) AND (l_quantity#38 < 24.0))
            +- *(1) ColumnarToRow
               +- FileScan parquet [l_quantity#38,l_extendedprice#39,l_discount#40,l_shipdate#44] Batched: true, DataFilters: [isnotnull(l_shipdate#44), isnotnull(l_discount#40), isnotnull(l_quantity#38), (l_shipdate#44 >= ..., Format: P
arquet, Location: InMemoryFileIndex[hdfs://10.124.48.67:9000/tpch_data100/lineitem101/lineitem_OrderedByShippingDat..., PartitionFilters: [], PushedFilters: [IsNotNull(l_shipdate), IsNotNull(l_discount), IsNotNull(l_quantity), GreaterTha
nOrEqual(l_shipda..., ReadSchema: struct<l_quantity:double,l_extendedprice:double,l_discount:double,l_shipdate:string>

21/12/06 10:19:02 INFO datasources.FileSourceStrategy: Pushed Filters: IsNotNull(l_shipdate),IsNotNull(l_discount),IsNotNull(l_quantity),GreaterThanOrEqual(l_shipdate,1994-01-01),LessThan(l_shipdate,1995-01-01),GreaterThanOrEqual(l_disco
unt,0.05),LessThanOrEqual(l_discount,0.07),LessThan(l_quantity,24.0)
21/12/06 10:19:02 INFO datasources.FileSourceStrategy: Post-Scan Filters: isnotnull(l_shipdate#44),isnotnull(l_discount#40),isnotnull(l_quantity#38),(l_shipdate#44 >= 1994-01-01),(l_shipdate#44 < 1995-01-01),(l_discount#40 >= 0.05),(l_di
--More--
......



21/12/06 10:19:12 INFO codegen.CodeGenerator: Code generated in 13.269001 ms

Scheduling mode = FIFO
Spark Context default degree of parallelism = 8
Aggregated Spark stage metrics:
numStages => 4
numTasks => 346
elapsedTime => 19937 (20 s)
stageDuration => 19681 (20 s)
executorRunTime => 145315 (2.4 min)
executorCpuTime => 41778 (42 s)
executorDeserializeTime => 4024 (4 s)
executorDeserializeCpuTime => 1492 (1 s)
resultSerializationTime => 7 (7 ms)
jvmGCTime => 3081 (3 s)
shuffleFetchWaitTime => 5 (5 ms)
shuffleWriteTime => 253 (0.3 s)
resultSize => 376121 (367.0 KB)
diskBytesSpilled => 0 (0 Bytes)
memoryBytesSpilled => 0 (0 Bytes)
peakExecutionMemory => 0
recordsRead => 189193816
bytesRead => 1536960970 (1465.0 MB)
recordsWritten => 1
bytesWritten => 29 (29 Bytes)
shuffleRecordsRead => 344
shuffleTotalBlocksFetched => 344
shuffleLocalBlocksFetched => 194
shuffleRemoteBlocksFetched => 150
shuffleTotalBytesRead => 19716 (19.0 KB)
shuffleLocalBytesRead => 11112 (10.0 KB)
shuffleRemoteBytesRead => 8604 (8.0 KB)
shuffleRemoteBytesReadToDisk => 0 (0 Bytes)
shuffleBytesWritten => 19716 (19.0 KB)
shuffleRecordsWritten => 344
21/12/06 10:19:12 INFO codegen.CodeGenerator: Code generated in 11.907056 ms
21/12/06 10:19:12 WARN sparkmeasure.StageMetrics: Accumulables metrics data refreshed into temp view AccumulablesStageMetrics
21/12/06 10:19:12 INFO codegen.CodeGenerator: Code generated in 5.455508 ms
21/12/06 10:19:12 INFO storage.BlockManagerInfo: Removed broadcast_14_piece0 on 10.124.48.62:45829 in memory (size: 12.0 KiB, free: 434.4 MiB)
21/12/06 10:19:12 INFO storage.BlockManagerInfo: Removed broadcast_14_piece0 on 10.124.48.61:34107 in memory (size: 12.0 KiB, free: 434.3 MiB)
21/12/06 10:19:12 INFO codegen.CodeGenerator: Code generated in 34.348111 ms
21/12/06 10:19:12 INFO codegen.CodeGenerator: Code generated in 12.941315 ms


```

- ** Get query time spent for before and after**:
  - get time spent from TIMES.text: root@master:~/tpch-spark# cat TIMES.txt
```
root@master:~/tpch-spark# cat TIMES.txt
Q06     23.60451317
Q06     88.55850983
Q06     23.89324512
Q06     87.74353027
Q06     24.83885574
Q06     87.19568723
root@master:~/tpch-spark#

```
### Preliminary results
**Caerus UDF-compiler translation speed up query time 3.64x, and reduce storage/network I/O 5.61x, comparing to Spark native UDF**

Spark measurement (based on the ordered data of TPCH 100 GB dataset with Q06):
 - Before (Spark native UDF): 87.833 s (+/- 0.685) query time, 7978193 bytes read 
   - raw data
     - query time in seconds: 88.55850983, 87.74353027, 87.19568723
     - bytes read: 8629567890 (8.0 GB), 8629567890 (8.0 GB), 8629567890 (8.0 GB)
 - After (UDF Compilation): 24.112 s (+/- 0.646) query time, 2428777 bytes read 
   - raw data
     - query time in seconds: 23.89324512, 23.60451317, 24.83885574
     - bytes read: 1536960970 (1465.0 MB), 1536960970 (1465.0 MB), 1536960970 (1465.0 MB)
 - **I/O Reduction**: 8629567890/1536960970 = **5.61x**
 - **Query time Speed up**: 87.833/24.112 = **3.64x**
