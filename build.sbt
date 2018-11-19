//import com.typesafe.sbt.SbtMultiJvm.multiJvmSettings
//import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm

scalaVersion := "2.12.7"

lazy val akkaVersion = "2.5.17"
lazy val clistVersion = "3.5.0"

lazy val hasher = project
  .in(file("."))
  .settings(
    organization := "com.github.leananeuber",
    name := "hasher",
    version := "0.0.1",
    libraryDependencies ++= Seq(
      "org.backuity.clist" %% "clist-core" % clistVersion,
      "org.backuity.clist" %% "clist-macros" % clistVersion % "provided",
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
      "com.github.romix.akka" %% "akka-kryo-serialization" % "0.5.2",
      // test
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
      "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion,
      "org.scalatest" %% "scalatest" % "3.0.5" % "test",
      // logging
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      // csv parsing
      "com.univocity" % "univocity-parsers" % "2.7.6",
    ),
    mainClass in assembly := Some("com.github.leananeuber.hasher.HasherApp"),
    // skip tests during assembly
    test in assembly := {},
  )
  // for multi-jvm tests:
//  .settings(multiJvmSettings: _*)
//  .configs(MultiJvm)