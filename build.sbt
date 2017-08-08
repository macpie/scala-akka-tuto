name := "ScalaTest"

version := "1.0"

scalaVersion := "2.12.3"


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.3" % Test
)

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % "test"