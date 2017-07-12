name := "UWT"

description := "Ulitmate Watering Tool"

version := "1.0"

scalaVersion := "2.12.1"

resolvers += Resolver.sonatypeRepo("public")

libraryDependencies += "com.pi4j" % "pi4j-core" % "1.2-SNAPSHOT"

val circeVersion = "0.8.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.16.0"

libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.9"

mainClass := Some("de.ax.uwt.Launscha")