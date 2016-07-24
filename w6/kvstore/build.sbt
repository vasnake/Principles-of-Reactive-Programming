submitProjectName := "kvstore"

scalaVersion := "2.11.5"

scalacOptions ++= Seq("-deprecation", "-feature")

(fork in Test) := false

projectDetailsMap := {
  val currentCourseId = "reactive-002"

  val depsNode = Seq(
    "io.reactivex" %% "rxscala" % "0.23.0",
    "io.reactivex" % "rxswing" % "0.21.0", // for Swing Scheduler in suggestions
    "org.json4s" %% "json4s-native" % "3.2.11",
    "org.scala-lang.modules" %% "scala-swing" % "1.0.1",
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.0",
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "org.slf4j" % "slf4j-api" % "1.7.5",
    "org.slf4j" % "slf4j-simple" % "1.7.5",
    "com.squareup.retrofit" % "retrofit" % "1.0.0",
    "org.scala-lang.modules" %% "scala-async" % "0.9.2"
  )

  val depsAkka = Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.3.9",
    "com.typesafe.akka" %% "akka-testkit" % "2.3.9",
    "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3.9"
  )

  Map(
     "example" -> ProjectDetails(
                    packageName = "example",
                    assignmentPartId = "fTzFogNl",
                    maxScore = 10d,
                    styleScoreRatio = 0.0,
                    courseId=currentCourseId),
    "quickcheck" -> ProjectDetails(
                    packageName = "quickcheck",
                    assignmentPartId = "02Vi5q7m",
                    maxScore = 10d,
                    styleScoreRatio = 0.0,
                    courseId=currentCourseId,
                    dependencies = Seq("org.scalacheck" %% "scalacheck" % "1.12.1")),
    "calculator" -> ProjectDetails(
                    packageName = "calculator",
                    assignmentPartId = "8uURtbi7",
                    maxScore = 10d,
                    styleScoreRatio = 0.0,
                    courseId=currentCourseId),
    "nodescala" -> ProjectDetails(
                    packageName = "nodescala",
                    assignmentPartId = "RvoTAbRy",
                    maxScore = 10d,
                    styleScoreRatio = 0.0,
                    courseId=currentCourseId,
                    dependencies = depsNode),
    "suggestions" -> ProjectDetails(
                    packageName = "suggestions",
                    assignmentPartId = "rLLdQLGN",
                    maxScore = 10d,
                    styleScoreRatio = 0.0,
                    courseId=currentCourseId),
    "actorbintree" -> ProjectDetails(
                    packageName = "actorbintree",
                    assignmentPartId = "VxIlIKoW",
                    maxScore = 10d,
                    styleScoreRatio = 0.0,
                    courseId=currentCourseId,
                    dependencies = depsAkka),
    "kvstore"      -> ProjectDetails(
                    packageName = "kvstore",
                    assignmentPartId = "nuvh59Zi",
                    maxScore = 20d,
                    styleScoreRatio = 0.0,
                    courseId=currentCourseId,
                    dependencies = depsAkka)
)}
