package org.openinfralabs.caerus.examples

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{IntegerType, StringType, StructType}

//1. Spark native: spark-submit --class org.openinfralabs.caerus.examples.SubmitExampleTaxDiscountUDF --master spark://10.124.48.60:7077 --driver-memory 5g target/spark-udf-1.0-SNAPSHOT.jar
//2. Spark traslation: spark-submit --class org.openinfralabs.caerus.examples.SubmitExampleTaxDiscountUDF --master spark://10.124.48.60:7077 --driver-memory 5g --driver-class-path /root/caerus-spark-udf-compiler-from-rapids/udf-compiler/target/rapids-4-spark-udf_2.12-21.10.0-SNAPSHOT.jar --conf "spark.sql.extensions"="com.nvidia.spark.udf.Plugin" target/spark-udf-1.0-SNAPSHOT.jar
object SubmitExampleTaxDiscountUDF {

  def main(args: Array[String]): Unit = {
    // $example on:udf_scalar$
    val spark = SparkSession
      .builder()
      .appName("Spark SQL UDF example: tax and discount caculation")
      //.config("spark.master", "local")
      //.config("spark.sql.extensions", "com.nvidia.spark.udf.Plugin")
      .getOrCreate()

    // TODO: can take parameters in
    val prodDF = spark.read.parquet("hdfs://10.124.48.67:9000/testData10MRecords.parquet")

    prodDF.count

    prodDF.createOrReplaceGlobalTempView("products")

    spark.udf.register("taxAndDiscountF", {(prodCat : String, amt : Double) =>

      import java.time.{LocalDate, LocalDateTime}
      import java.time.format.DateTimeFormatter

      var taxRate = 9.5
      if (prodCat.equals("grocery")) taxRate = 0.0
      else if (prodCat.equals("alcohol")) taxRate = 10.5

      val dayOfMonth = LocalDateTime.parse("2021-02-01 08:59:12", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).getDayOfMonth
      val discount = if (dayOfMonth == 1 && prodCat.equals("alcohol")) 0.05 else 0.0

      amt * ( 1.0 - discount) * (1.0 + taxRate)
    })

    val funcBasedResDF =  spark.sql("select prod, taxAndDiscountF(prod, amt) from  global_temp.products where taxAndDiscountF(prod, amt) > 50")

    funcBasedResDF.explain(true)

    // Measure time spent on the following task, for perf comparison
    spark.time(funcBasedResDF.collect)

    spark.stop()
  }
}
