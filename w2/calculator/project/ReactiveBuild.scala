import sbt._
import Keys._
import ch.epfl.lamp.CourseraBuild
import ch.epfl.lamp.SbtCourseraPlugin.autoImport._

import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

object ProgfunBuild extends CourseraBuild {
  override def assignmentSettings: Seq[Setting[_]] = Seq(
    // This setting allows to restrict the source files that are compiled and tested
    // to one specific project. It should be either the empty string, in which case all
    // projects are included, or one of the project names from the projectDetailsMap.
    currentProject := "",

    // Packages in src/main/scala that are used in every project. Included in every
    // handout, submission.
    commonSourcePackages += "common",

    // Packages in src/test/scala that are used for grading projects. Always included
    // compiling tests, grading a project.

    libraryDependencies += "ch.epfl.lamp" %% "scala-grading-runtime" % "0.1",

    // Files that we hand out to the students
    handoutFiles <<= (baseDirectory, projectDetailsMap, commonSourcePackages) map {
      (basedir, detailsMap, commonSrcs) =>
      (projectName: String) => {
        val details = detailsMap.getOrElse(projectName, sys.error("Unknown project name: "+ projectName))
        val commonFiles = (PathFinder.empty /: commonSrcs)((files, pkg) =>
          files +++ (basedir / "src" / "main" / "scala" / pkg ** "*.scala")
        )
        val forAll = {
          (basedir / "src" / "main" / "scala" / details.packageName ** "*.scala") +++
          commonFiles +++
          (basedir / "src" / "main" / "resources" / details.packageName / "*") +++
          (basedir / "src" / "test" / "scala" / details.packageName ** "*.scala") +++
          (basedir / "build.sbt") +++
          (basedir / "project" / "build.properties") +++
          (basedir / "project" ** ("*.scala" || "*.sbt")) +++
          (basedir / "project" / "scalastyle_config_reactive.xml") +++
          (basedir / "lib_managed" ** "*.jar") +++
          (basedir * (".classpath" || ".project")) +++
          (basedir / ".settings" / "org.scala-ide.sdt.core.prefs")
        }
        if (projectName == "calculator") {
          forAll +++
          (basedir / "webui.sbt") +++
          (basedir / "web-ui" / "index.html") +++
          (basedir / "web-ui" / "src" / "main" / "scala" ** "*.scala")
        } else
          forAll
      }
    })
}
