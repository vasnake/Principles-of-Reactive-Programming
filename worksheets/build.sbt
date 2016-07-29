name := "ReactiveWorksheets"

version := "1.0"

scalaVersion := "2.11.8"

val depsAkka = Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.3.10",
    "com.typesafe.akka" %% "akka-testkit" % "2.3.10",
    "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3.10",
    "com.ning" % "async-http-client" % "1.7.24",
    "org.jsoup" % "jsoup" % "1.8.3",
    "com.typesafe.akka" %% "akka-cluster" % "2.3.10"
)

libraryDependencies ++= Seq(
    "com.netflix.rxjava" % "rxjava-scala" % "0.15.1",
    "org.scalatest" %% "scalatest" % "2.2.4",
    "junit" % "junit" % "4.11",
    "org.scala-lang.modules" %% "scala-async" % "0.9.2",
    "com.squareup.retrofit" % "retrofit" % "1.2.2"
) ++ depsAkka
