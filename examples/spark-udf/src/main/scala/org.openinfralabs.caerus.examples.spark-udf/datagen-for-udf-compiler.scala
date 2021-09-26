package org.openinfralabs.caerus.examples

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{IntegerType, StringType, StructType}

//spark-submit --class org.openinfralabs.caerus.examples.DataGen --master spark://10.124.48.60:7077 spark-udf-1.0-SNAPSHOT.jar
case class Product(prod : String, prodCat : String, amt : Double)
object DataGen {

  val prods = for(i <- (0 until 1000000)) yield {
    Product(s"p_$i", {val m = i % 3; if (m == 0) "alcohol" else if (i == 1) "grocery" else "rest"}, (i % 200).toDouble)
  }

  def main(args: Array[String]): Unit = {
    // $example on:udf_scalar$
    val sparkSession = SparkSession
      .builder()
      .appName("Spark udf compiler benchmark dataGen")
      //.config("spark.master", "local")
      //.config("spark.sql.extensions", "com.nvidia.spark.udf.Plugin")
      .getOrCreate()

    import sparkSession.implicits._
    val prodDF = sparkSession.createDataset(prods).coalesce(16)

    // do data merge, or it will have OOM
    // TDOD: can take in parameters like billion/trillion, and file name etc.
    for(a<-1 to 10){
      Console.println("Writing %s million records.", a)
      prodDF.write.mode("append").parquet("hdfs://10.124.48.67:9000/testData10MRecords.parquet")
    }

    sparkSession.stop()
  }
}
