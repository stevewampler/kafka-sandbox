name := "kafka-sandbox"

version := "1.0"

scalaVersion := "2.11.7"

val kafkaVersion = "0.9.0.0"

libraryDependencies ++= Seq(
  "org.apache.kafka" % "kafka-clients" % kafkaVersion,
  "org.slf4j" % "slf4j-log4j12" % "1.7.16"
)