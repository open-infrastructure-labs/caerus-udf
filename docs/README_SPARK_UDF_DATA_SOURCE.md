# Spark Data Source Support: Verification of UDF and Others Pushdown to Data Source

Here are the reasons we started this investigation:
   1. With Spark SQL Macros for Spark UDF pushdown, it was confirmed that from the Spark (Catalyst) side, it indeed showed in the physical plan that certain portion of the UDF used in predicates/filters etc. were pushed down, 
but we are not quite sure what exact the contents that were pushed down to the data source. 
   2. There is a confusion related to UDFs pushdown in Spark: on one hand, it is a general consent that Spark UDFs cannot be pushed down to data source, they can only run on compute (Spark) side. On the other hand, we also read that UDFs can be pushed down by Spark for some data sources including 
SQL databases (oracle, Postgres, and MySql etc.), NoSQL database (MongoDB, Cassandra) and data warehouse products (Hive etc). 

The goals of this investigation are:
  - To identify an established data source as a verifiable source of truth for pushdown  
    - easily set up as a Spark data source for UDF pushdown
    - can turn on trace to visualize the actual content that is pushdown by Spark      
  - To confirm if such data sources indeed support UDF (what UDFs?) pushdown 
  - To understand how UDF pushdown works in Spark with at least one of such data sources
  - To borrow some ideas, if possible, from such data sources for Caerus since they are the pioneers in supporting UDF pushdown in Spark 

The conclusions of this investigation are:
  - UDF pushdown in Spark has actually two meanings:
    - "Spark UDF" pushdown: refers to Spark lambda-style Saprk UDF function being pushed down to data source. This is not supported. To pushdown such "Saprk UDF" functionality, you have to use solutions to change UDFs into Spark expression as Spark SQl Macros.
    - "Data Source UDF" pushdown in Spark: refers to UDFs that can be pushdown to data source, but these UDFs will not be able to use optimization that Spark Catalyst provide. 
  - A JDBC data source like Postgres can be used as a verifiable data source for Spark pushdown (see details next on how to set it up etc.)

## Set up Spark, a JDBC (Postgres) data source, and Spark SQL Macros UDF support

### How To Start:
Followings are manual steps to set up the entire system to run different kinds of Spark UDF pushdown with a JDBC data source. If needed, an automation can be added in the future:
  - Set up a JDBC data source, in this case Postgres database
  - Start Spark shell session (or submit) with plugins of a JDBC data source and Spark SQL Macros
  - Run different UDF pushdown experiments by turning on tarces from both Spark and Data sources sides   

#### Set up Postgres
1. Follow examples like this:
   https://www.digitalocean.com/community/tutorials/how-to-install-and-use-postgresql-on-ubuntu-18-04
2. Create sample database tables with data:
   https://zheguang.github.io/blog/systems/2019/02/16/connect-spark-to-postgres.html
3. Turn on Postgres trace/logging (see below link and sample config file in this folder)
   https://stackoverflow.com/questions/722221/how-to-log-postgresql-queries

Some sample outputs are listed as follows:
```
1. root@ubuntu1804:/home/ubuntu# service postgresql status
● postgresql.service - PostgreSQL RDBMS
   Loaded: loaded (/lib/systemd/system/postgresql.service; enabled; vendor preset: enabled)
   Active: active (exited) since Thu 2021-04-22 07:55:25 EDT; 11min ago
  Process: 2080 ExecStart=/bin/true (code=exited, status=0/SUCCESS)
Main PID: 2080 (code=exited, status=0/SUCCESS)

Apr 22 07:55:25 ubuntu1804 systemd[1]: Starting PostgreSQL RDBMS...
Apr 22 07:55:25 ubuntu1804 systemd[1]: Started PostgreSQL RDBMS.
2.	root@ubuntu1804:/home/ubuntu# sudo -i -u postgres
3.	postgres@ubuntu1804:~$ psql
psql (10.16 (Ubuntu 10.16-0ubuntu0.18.04.1))
Type "help" for help.

4.	postgres=# \conninfo
You are connected to database "postgres" as user "postgres" via socket in "/var/run/postgresql" at port "5432".
5.	postgres=# \d
         List of relations
Schema |  Name  | Type  |  Owner   
--------+--------+-------+----------
public | class  | table | postgres
public | person | table | postgres
(2 rows)

6.	postgres=# SELECT * FROM person;
name | age 
------+-----
Jim  |  21
John |  25
Jane |  19
(3 rows)

7.	postgres=# SELECT * FROM class;
name |   class   
------+-----------
Jane | nursing
Jim  | cs
John | chemistry
(3 rows)

8.	root@ubuntu1804:/home/ubuntu# ls /var/lib/postgresql/10/main/pg_log/
postgresql-2021-04-21_091656.log  postgresql-2021-04-22_075523.log
9.	root@ubuntu1804:/home/ubuntu# vi /var/lib/postgresql/10/main/pg_log/postgresql-2021-04-22_075523.log

```
#### Set up Spark with SQL Macros and JDBC support
1. Get postgres plugin
```
wget https://jdbc.postgresql.org/download/postgresql-9.4.1207.jar
```
2. Build Spark SQL macro Spakr plugin
   https://github.com/futurewei-cloud/caerus/blob/master/ndp/udf/docs/README_SPARK_UDF.md

Sample output from Spark Shell:
```
root@ubuntu1804:/home/ubuntu# spark-shell --driver-class-path /opt/spark-sql-macros/sql/target/scala-2.12/spark-sql-macros_2.12.10_0.1.0-SNAPSHOT.jar:postgresql-9.4.1207.jar --jars postgresql-9.4.1207.jar 
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/opt/spark/jars/slf4j-log4j12-1.7.30.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/opt/hadoop-2.8.2/share/hadoop/common/lib/slf4j-log4j12-1.7.10.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.Log4jLoggerFactory]
21/04/24 10:52:12 WARN util.Utils: Your hostname, ubuntu1804 resolves to a loopback address: 127.0.1.1; using 192.168.6.129 instead (on interface ens33)
21/04/24 10:52:12 WARN util.Utils: Set SPARK_LOCAL_IP if you need to bind to another address
Setting default log level to "WARN".
To adjust logging level use sc.setLogLevel(newLevel). For SparkR, use setLogLevel(newLevel).
21/04/24 10:52:28 WARN util.Utils: Service 'SparkUI' could not bind on port 4040. Attempting port 4041.
Spark context Web UI available at http://192.168.6.129:4041
Spark context available as 'sc' (master = local[*], app id = local-1619275949223).
Spark session available as 'spark'.
Welcome to
      ____              __
     / __/__  ___ _____/ /__
    _\ \/ _ \/ _ `/ __/  '_/
   /___/ .__/\_,_/_/ /_/\_\   version 3.1.1
      /_/
         
Using Scala version 2.12.10 (OpenJDK 64-Bit Server VM, Java 1.8.0_282)
Type in expressions to have them evaluated.
Type :help for more information.

scala> 

```
#### Run experiments
Sample output:
```
2.	scala> val url = "jdbc:postgresql://localhost:5432/postgres"
url: String = jdbc:postgresql://localhost:5432/postgres

3.	scala> import java.util.Properties
import java.util.Properties
4.	scala> val connectionProperties = new Properties()

connectionProperties: java.util.Properties = {}

5.	scala> connectionProperties.setProperty("Driver", "org.postgresql.Driver")
res0: Object = null

6.	scala> val query1 = "(SELECT * FROM person) as q1" 
query1: String = SELECT * FROM person

7.	scala> connectionProperties.put("user", "postgres")
res1: Object = null

8.	scala> connectionProperties.put("password", "postgres")
res2: Object = null

9.	scala> val query1df = spark.read.jdbc(url, query1, connectionProperties)

10.	scala> query1df.show()

11.	scala> query1df.explain(true)


//original postgres query, the spark just pushdown the entire sql query without optimization
12.	scala> val query2 = "(SELECT * FROM person where age >20) as q1" 
query2: String = (SELECT * FROM person where age >20) as q1

13.	scala> val query2df = spark.read.jdbc(url, query2, connectionProperties)
query2df: org.apache.spark.sql.DataFrame = [name: string, age: int]

14.	scala> query2df.explain(true)
== Parsed Logical Plan ==
Relation[name#13,age#14] JDBCRelation((SELECT * FROM person where age >20) as q1) [numPartitions=1]

== Analyzed Logical Plan ==
name: string, age: int
Relation[name#13,age#14] JDBCRelation((SELECT * FROM person where age >20) as q1) [numPartitions=1]

== Optimized Logical Plan ==
Relation[name#13,age#14] JDBCRelation((SELECT * FROM person where age >20) as q1) [numPartitions=1]

== Physical Plan ==
*(1) Scan JDBCRelation((SELECT * FROM person where age >20) as q1) [numPartitions=1] [name#13,age#14] PushedFilters: [], ReadSchema: struct<name:string,age:int>


//postgres log
2021-04-22 08:22:25.235 EDT [13327] postgres@postgres LOG:  execute <unnamed>: SELECT * FROM (SELECT * FROM person where age >20) as q1 WHERE 1=0

//use spark context like filter() etc. the pushdown is optimized by Spark
15.	scala> query1df.filter($"age" === 21).explain(true)
== Parsed Logical Plan ==
'Filter ('age = 21)
+- Relation[name#0,age#1] JDBCRelation((SELECT * FROM person) as q1) [numPartitions=1]

== Analyzed Logical Plan ==
name: string, age: int
Filter (age#1 = 21)
+- Relation[name#0,age#1] JDBCRelation((SELECT * FROM person) as q1) [numPartitions=1]

== Optimized Logical Plan ==
Filter (isnotnull(age#1) AND (age#1 = 21))
+- Relation[name#0,age#1] JDBCRelation((SELECT * FROM person) as q1) [numPartitions=1]

== Physical Plan ==
*(1) Scan JDBCRelation((SELECT * FROM person) as q1) [numPartitions=1] [name#0,age#1] PushedFilters: [*IsNotNull(age), *EqualTo(age,21)], ReadSchema: struct<name:string,age:int>


16.	scala> query1df.filter($"age" === 21).show(true)
//postgres log
2021-04-22 08:39:32.555 EDT [17240] postgres@postgres LOG:  execute <unnamed>: SELECT "name","age" FROM (SELECT * FROM person) as q1 WHERE ("age" IS NOT NULL) AND ("age" = 21)

// followings are UDFs
17.	scala>   spark.udf.register("intUDF", (i: Int) => {
     |        val j = 2
     |        i + j
     |       })
res15: org.apache.spark.sql.expressions.UserDefinedFunction = SparkUserDefinedFunction($Lambda$3297/1827007387@63b16764,IntegerType,List(Some(class[value[0]: int])),Some(class[value[0]: int]),Some(intUDF),false,true)

18.	scala> import org.apache.spark.sql.defineMacros._
import org.apache.spark.sql.defineMacros._

19.	scala> spark.registerMacro("intUDM", spark.udm((i: Int) => {
     |    val j = 2
     |    i + j
     |   }))

20.	scala> query1df.filter("""intUDF(age) == 21""").show(true)
+----+---+
|name|age|
+----+---+
|Jane| 19|
+----+---+


21.	scala> query1df.filter("""intUDF(age) == 21""").explain(true)
== Parsed Logical Plan ==
'Filter ('intUDF('age) = 21)
+- Relation[name#0,age#1] JDBCRelation((SELECT * FROM person) as q1) [numPartitions=1]

== Analyzed Logical Plan ==
name: string, age: int
Filter (if (isnull(age#1)) null else intUDF(knownnotnull(age#1)) = 21)
+- Relation[name#0,age#1] JDBCRelation((SELECT * FROM person) as q1) [numPartitions=1]

== Optimized Logical Plan ==
Filter (if (isnull(age#1)) null else intUDF(knownnotnull(age#1)) = 21)
+- Relation[name#0,age#1] JDBCRelation((SELECT * FROM person) as q1) [numPartitions=1]

== Physical Plan ==
*(1) Filter (if (isnull(age#1)) null else intUDF(knownnotnull(age#1)) = 21)
+- *(1) Scan JDBCRelation((SELECT * FROM person) as q1) [numPartitions=1] [name#0,age#1] PushedFilters: [], ReadSchema: struct<name:string,age:int>


22.	scala> query1df.filter("""intUDM(age) == 21""").show(true)
+----+---+
|name|age|
+----+---+
|Jane| 19|
+----+---+


23.	scala> query1df.filter("""intUDM(age) == 21""").explain(true)
== Parsed Logical Plan ==
'Filter ('intUDM('age) = 21)
+- Relation[name#0,age#1] JDBCRelation((SELECT * FROM person) as q1) [numPartitions=1]

== Analyzed Logical Plan ==
name: string, age: int
Filter ((age#1 + 2) = 21)
+- Relation[name#0,age#1] JDBCRelation((SELECT * FROM person) as q1) [numPartitions=1]

== Optimized Logical Plan ==
Filter (isnotnull(age#1) AND ((age#1 + 2) = 21))
+- Relation[name#0,age#1] JDBCRelation((SELECT * FROM person) as q1) [numPartitions=1]

== Physical Plan ==
*(1) Filter ((age#1 + 2) = 21)
+- *(1) Scan JDBCRelation((SELECT * FROM person) as q1) [numPartitions=1] [name#0,age#1] PushedFilters: [*IsNotNull(age)], ReadSchema: struct<name:string,age:int>
//postgres trace log
2021-04-22 08:55:53.781 EDT [21066] postgres@postgres LOG:  execute <unnamed>: SET extra_float_digits = 3
2021-04-22 08:55:53.782 EDT [21066] postgres@postgres LOG:  execute <unnamed>: SELECT "name","age" FROM (SELECT * FROM person) as q1 WHERE ("age" IS NOT NULL)


//Can I pushdown a postgres UDF via spark?
24.	scala> val query3 = "(SELECT * FROM person where intUDF(age) >20) as q1" 
query3: String = (SELECT * FROM person where intUDF(age) >20) as q1

25.	scala> val query3df = spark.read.jdbc(url, query3, connectionProperties)
org.postgresql.util.PSQLException: ERROR: function intudf(integer) does not exist
  Hint: No function matches the given name and argument types. You might need to add explicit type casts.
  Position: 43
  at org.postgresql.core.v3.QueryExecutorImpl.receiveErrorResponse(QueryExecutorImpl.java:2182)
  at org.postgresql.core.v3.QueryExecutorImpl.processResults(QueryExecutorImpl.java:1911)
  at org.postgresql.core.v3.QueryExecutorImpl.execute(QueryExecutorImpl.java:173)
  at org.postgresql.jdbc.PgStatement.execute(PgStatement.java:622)
  at org.postgresql.jdbc.PgStatement.executeWithFlags(PgStatement.java:472)
  at org.postgresql.jdbc.PgStatement.executeQuery(PgStatement.java:386)
  at org.apache.spark.sql.execution.datasources.jdbc.JDBCRDD$.resolveTable(JDBCRDD.scala:61)
  at org.apache.spark.sql.execution.datasources.jdbc.JDBCRelation$.getSchema(JDBCRelation.scala:226)
  at org.apache.spark.sql.execution.datasources.jdbc.JdbcRelationProvider.createRelation(JdbcRelationProvider.scala:35)
  at org.apache.spark.sql.execution.datasources.DataSource.resolveRelation(DataSource.scala:354)
  at org.apache.spark.sql.DataFrameReader.loadV1Source(DataFrameReader.scala:326)
  at org.apache.spark.sql.DataFrameReader.$anonfun$load$3(DataFrameReader.scala:308)
  at scala.Option.getOrElse(Option.scala:189)
  at org.apache.spark.sql.DataFrameReader.load(DataFrameReader.scala:308)
  at org.apache.spark.sql.DataFrameReader.load(DataFrameReader.scala:226)
  at org.apache.spark.sql.DataFrameReader.jdbc(DataFrameReader.scala:341)
  ... 49 elided
                // postgres trace
2021-04-22 09:00:52.208 EDT [22278] postgres@postgres ERROR:  function intudf(integer) does not exist at character 43
2021-04-22 09:00:52.208 EDT [22278] postgres@postgres HINT:  No function matches the given name and argument types. You might need to add explicit type casts.
2021-04-22 09:00:52.208 EDT [22278] postgres@postgres STATEMENT:  SELECT * FROM (SELECT * FROM person where intUDF(age) >20) as q1 WHERE 1=0


26.	scala> postgres=# create function intudf(_t text)
postgres-# returns int
postgres-# language plpgsql
postgres-# as $$
postgres$# declare plustwo integer;
postgres$#  begin
postgres$# select $1+2 into plustwo from person; 
postgres$# return plustwo;
postgres$#  end;
postgres$# $$;
CREATE FUNCTION

Or 
create function intudf(int)
returns int
language plpgsql
as $$
declare plustwo integer;
begin
select $1+2 into plustwo from person; 
return plustwo;
end;
$$;

postgres=# DROP FUNCTION IF EXISTS intudf(_t);
NOTICE:  type "_t" does not exist, skipping
DROP FUNCTION

// list all udfs
select n.nspname as function_schema,
       p.proname as function_name,
       l.lanname as function_language,
       case when l.lanname = 'internal' then p.prosrc
            else pg_get_functiondef(p.oid)
            end as definition,
       pg_get_function_arguments(p.oid) as function_arguments,
       t.typname as return_type
from pg_proc p
left join pg_namespace n on p.pronamespace = n.oid
left join pg_language l on p.prolang = l.oid
left join pg_type t on t.oid = p.prorettype 
where n.nspname not in ('pg_catalog', 'information_schema')
order by function_schema,
         function_name;
27.	cala> val query3df = spark.read.jdbc(url, query3, connectionProperties)
query3df: org.apache.spark.sql.DataFrame = [name: string, age: int]

28.	scala> query3df.show(true)
+----+---+
|name|age|
+----+---+
| Jim| 21|
|John| 25|
|Jane| 19|
+----+---+


29.	scala> query3df.explain(true)
== Parsed Logical Plan ==
Relation[name#64,age#65] JDBCRelation((SELECT * FROM person where intudf(age) >20) as q1) [numPartitions=1]

== Analyzed Logical Plan ==
name: string, age: int
Relation[name#64,age#65] JDBCRelation((SELECT * FROM person where intudf(age) >20) as q1) [numPartitions=1]

== Optimized Logical Plan ==
Relation[name#64,age#65] JDBCRelation((SELECT * FROM person where intudf(age) >20) as q1) [numPartitions=1]

== Physical Plan ==
*(1) Scan JDBCRelation((SELECT * FROM person where intudf(age) >20) as q1) [numPartitions=1] [name#64,age#65] PushedFilters: [], ReadSchema: struct<name:string,age:int>
// postgres trace
2021-04-22 14:38:25.583 EDT [74719] postgres@postgres LOG:  execute <unnamed>: SET extra_float_digits = 3
2021-04-22 14:38:25.585 EDT [74719] postgres@postgres LOG:  execute <unnamed>: SELECT "name","age" FROM (SELECT * FROM person where intudf(age) >20) as q1

30.	scala>
scala> val query4 = "(SELECT * FROM person where intUDF(age) >21) as q1" 
query4: String = (SELECT * FROM person where intUDF(age) >21) as q1

scala> val query4df = spark.read.jdbc(url, query4, connectionProperties)
query4df: org.apache.spark.sql.DataFrame = [name: string, age: int]

scala> query4df.explain(true)
== Parsed Logical Plan ==
Relation[name#77,age#78] JDBCRelation((SELECT * FROM person where intUDF(age) >21) as q1) [numPartitions=1]

== Analyzed Logical Plan ==
name: string, age: int
Relation[name#77,age#78] JDBCRelation((SELECT * FROM person where intUDF(age) >21) as q1) [numPartitions=1]

== Optimized Logical Plan ==
Relation[name#77,age#78] JDBCRelation((SELECT * FROM person where intUDF(age) >21) as q1) [numPartitions=1]

== Physical Plan ==
*(1) Scan JDBCRelation((SELECT * FROM person where intUDF(age) >21) as q1) [numPartitions=1] [name#77,age#78] PushedFilters: [], ReadSchema: struct<name:string,age:int>


scala> query4df.show(true)
+----+---+
|name|age|
+----+---+
| Jim| 21|
|John| 25|
+----+---+


2021-04-22 14:52:03.394 EDT [77758] postgres@postgres LOG:  execute <unnamed>: SET extra_float_digits = 3
2021-04-22 14:52:03.396 EDT [77758] postgres@postgres LOG:  execute <unnamed>: SELECT * FROM (SELECT * FROM person where intUDF(age) >21) as q1 WHERE 1=0
2021-04-22 14:52:24.432 EDT [77859] postgres@postgres LOG:  execute <unnamed>: SET extra_float_digits = 3
2021-04-22 14:52:24.434 EDT [77859] postgres@postgres LOG:  execute <unnamed>: SELECT "name","age" FROM (SELECT * FROM person where intUDF(age) >21) as q1


```