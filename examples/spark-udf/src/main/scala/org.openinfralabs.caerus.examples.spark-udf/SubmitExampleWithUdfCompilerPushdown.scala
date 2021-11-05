package org.openinfralabs.caerus.examples

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{IntegerType, StringType, StructType}
// if needed, can un-comment out pom.xml for macros support
// import org.apache.spark.sql.defineMacros._

object SubmitExampleWithUdfCompilerPushdown {

  def main(args: Array[String]): Unit = {
    // $example on:udf_scalar$
    val spark = SparkSession
      .builder()
      .appName("Spark SQL UDF scalar example")
      .config("spark.master", "local")
      .config("spark.sql.extensions", "com.nvidia.spark.udf.Plugin")
      .getOrCreate()

    spark.sparkContext.setLogLevel("TRACE");

    val schema = new StructType().add("name", StringType, true).add("age", IntegerType, true)
    val df_with_schema = spark.read.schema(schema).json("file:///data/source/people.json")
    df_with_schema.createOrReplaceTempView("people_with_schema")

    spark.udf.register("intUDF", (i: Int) => {
      val j = 2
      i + j
    })

    spark.udf.register("intUDF1", (i: Int) => {
      i
    })
    spark.udf.register("isOlderThan20", (i: Int) => {i>20})



    // if needed, can un-comment out pom.xml for macros support
    // spark.registerMacro("isOlderThan20udm", spark.udm((age: Int)  => {age > 20}))


    val udfResult = spark.sql("SELECT * FROM people_with_schema WHERE intUDF1(age) > 22")
    udfResult.explain(true)
    udfResult.show()

    spark.stop()
  }
}
