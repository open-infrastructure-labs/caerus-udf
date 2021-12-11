name := "Spark TPC-H Queries"

version := "1.0"

//scalaVersion := "2.11.7"
scalaVersion := "2.12.10"

libraryDependencies += "org.apache.spark" %% "spark-core" % "2.4.0"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.4.0"
// adding spark measure from this: https://github.com/LucaCanali/sparkMeasure
libraryDependencies += "ch.cern.sparkmeasure" %% "spark-measure" % "0.15"
