lazy val webUI = project.in(file("web-ui")).
  enablePlugins(ScalaJSPlugin).
  settings(
    scalaVersion := "2.11.6",
    // Add the sources of the calculator project
    unmanagedSourceDirectories in Compile +=
      (scalaSource in (assignmentProject, Compile)).value / "calculator",
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.8.0",
    persistLauncher := true
  )
