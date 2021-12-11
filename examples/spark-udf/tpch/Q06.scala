package main.scala

import org.apache.spark.sql.DataFrame
import org.apache.spark.SparkContext
import org.apache.spark.sql.functions.sum
import org.apache.spark.sql.functions.udf

/**
 * TPC-H Query 6
 * Savvas Savvides <savvas@purdue.edu>
 *
 */
class Q06 extends TpchQuery {

  override def execute(sc: SparkContext, schemaProvider: TpchSchemaProvider): DataFrame = {

    // this is used to implicitly convert an RDD to a DataFrame.
    val sqlContext = new org.apache.spark.sql.SQLContext(sc)
    import sqlContext.implicits._
    import schemaProvider._

    import org.apache.spark.sql.functions.udf
    sqlContext.udf.register("discountUDF", { (discount: Double) => discount })

    //sqlContext.udf.register("shipdateUDF", {(shipdate:String) => shipdate})
    //sqlContext.udf.register("shipdateUDF", {(shipdate:String) => shipdate})

    //sqlContext.udf.register("discountUDFcomp", { (discount: Double) =>
      //discount >= 0.05 && discount <= 0.07
    //})

    //sqlContext.udf.register("shipdateUDF", {(shipdate:String) => shipdate})
    sqlContext.udf.register("shipdateUDFcomp", { (shipdate: String) =>
      //import java.sql.Date

      //val date = Date.valueOf(shipdate)
      //val low =  Date.valueOf("1994-01-01")
      //val high = Date.valueOf("1995-01-01")
      //date.compareTo(low) >= 0 && date.compareTo(high) < 0
      //
      import java.time.{LocalDate, LocalDateTime}
      import java.time.format.DateTimeFormatter

      val year = LocalDateTime.parse(shipdate + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).getYear
      val month = LocalDateTime.parse(shipdate + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).getMonthValue
      val dayOfMonth = LocalDateTime.parse(shipdate + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).getDayOfMonth
      val yearLow = LocalDateTime.parse("1994-01-01 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).getYear
      val monthLow = LocalDateTime.parse("1994-01-01 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).getMonthValue
      val dayOfMonthLow = LocalDateTime.parse("1994-01-01 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).getDayOfMonth
      val yearHigh = LocalDateTime.parse("1995-01-01 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).getYear
      val monthHigh = LocalDateTime.parse("1995-01-01 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).getMonthValue
      val dayOfMonthHigh = LocalDateTime.parse("1995-01-01 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).getDayOfMonth


      //year == yearLow
      year >= yearLow && year < yearHigh
    })

    // TODO: UDF compiler doesn't support date comparison while it is running as part of the filter, so just wrap the date string into a UDF,
    //  Without UDF compiler, this will cause Spark to not form the push down query plan for the "shipdate", and cannot be codegen-ed. With compiler however
    //  we will expect both query plan and codegen will work, thus we can see the query time saving and I/O reduction.
    //  Once the date comparison feature is developed, we can rework this UDF. The result should be similar.
    sqlContext.udf.register("shipdateUDF", { (shipdate: String) =>
      shipdate
    })


    val sqlStatement3 =
      """
    select
        sum(l_extendedprice * discountUDF(l_discount)) as revenue
    from
        lineitem
    where
        l_shipdate >= date '1994-01-01'
        and l_shipdate < date '1995-01-01'
        and discountUDFcomp(l_discount)
        and l_quantity < 24"""

    val sqlStatement2 =
      """
    select
        sum(l_extendedprice * l_discount) as revenue
    from
        lineitem
    where
        shipdateUDFcomp(l_shipdate)
        and discountUDFcomp(l_discount)
        and l_quantity < 24"""

    val sqlStatement4 =
      """
    select
        sum(l_extendedprice * discountUDF(l_discount)) as revenue
    from
        lineitem
    where
        shipdateUDF(l_shipdate) >=  '1994-01-01'
        and shipdateUDF(l_shipdate) < '1995-01-01'
        and discountUDF(l_discount) between .05 and .07
        and l_quantity < 24"""

    val sqlStatement =
      """
    select
        sum(l_extendedprice * discountUDF(l_discount)) as revenue
    from
        lineitem
    where
        shipdateUDF(l_shipdate) >= 1994
        and shipdateUDF(l_shipdate) < 1995
        and discountUDF(l_discount) between .05 and .07
        and l_quantity < 24"""


    val sqlStatement1 =
      """
     select
         count (l_shipdate)
     from
         lineitem
     where
        l_shipdate IS NULL or l_shipdate = ''
     """

    sqlContext.sql(sqlStatement4)
    //lineitem.filter($"l_shipdate" >= "1994-01-01" && $"l_shipdate" < "1995-01-01" && $"l_discount" >= 0.05 && $"l_discount" <= 0.07 && $"l_quantity" < 24)
    // .agg(sum($"l_extendedprice" * $"l_discount"))
    //lineitem.filter($"l_shipdate" >= "1994-01-01" && $"l_shipdate" < "1995-01-01" && callUdf("discountUDF", struct($"l_discount")) >= 0.05 && callUdf("discountUDF", struct($"l_discount")) <= 0.07 && $"l_quantity" < 24)
    //.agg(sum($"l_extendedprice" * $"l_discount"))

  }

}
