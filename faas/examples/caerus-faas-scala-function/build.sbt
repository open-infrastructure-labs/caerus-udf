import Dependencies._

ThisBuild / scalaVersion := "2.12.6"

lazy val hello = (project in file("."))
  .settings(
    name := "openfaas-template-scala",
    libraryDependencies += scalaTest % Test,
    libraryDependencies += json4sJackson,
    mainClass in assembly := Some("Main"),
    assemblyJarName in assembly := "handler.jar"
  )