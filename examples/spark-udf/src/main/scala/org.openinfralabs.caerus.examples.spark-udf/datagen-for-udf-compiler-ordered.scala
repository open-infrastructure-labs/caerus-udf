package org.openinfralabs.caerus.examples

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{IntegerType, StringType, StructType}


//spark-submit --class org.openinfralabs.caerus.examples.DataGenOrdered --master spark://10.124.48.60:7077 spark-udf-1.0-SNAPSHOT.jar
object DataGenOrdered {

  def main(args: Array[String]): Unit = {
    // $example on:udf_scalar$
    val spark = SparkSession
      .builder()
      .appName("Spark udf compiler benchmark dataGen")
      //.config("spark.master", "local")
      //.config("spark.sql.extensions", "com.nvidia.spark.udf.Plugin")
      .getOrCreate()

    val prodDF = spark.read.parquet("hdfs://10.124.48.67:9000/testData100MRecords.parquet")

    prodDF.repartition(1).write.partitionBy("amt").format("parquet").save("hdfs://10.124.48.67:9000/testData100MRecords_OrderedByAmt.parquet")

    spark.stop()
  }
}
