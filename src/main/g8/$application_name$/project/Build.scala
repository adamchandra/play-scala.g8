import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {
  
  val appFront         = "$application_name$-front"
  val appCore          = "$application_name$-core"
  def prjPath = (s:String) => "prj-"+s

  val appVersion      = "0.1-SNAPSHOT"

  val coreDependencies = Seq(
    "org.scalaz" %% "scalaz-core" % "7.0.0-M7" withSources(),
    "org.specs2" %% "specs2" % "latest.release" % "test"
  )

  val core = Project(appCore, prjPath(appCore), appVersion, coreDependencies, mainLang = SCALA).settings(

  )

  val front = play.Project(appFront, prjPath(appFront), appVersion, mainLang = SCALA).settings(

  ) dependsOn core

}
