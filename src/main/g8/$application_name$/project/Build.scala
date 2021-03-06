import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {
  
  val appFront         = "$application_name$-front"
  val appCore          = "$application_name$-core"
  def prjPath = (s:String) => "prj-"+s

  val scalaSettings = Seq(
    scalaVersion := "2.10.1",
    scalacOptions := Seq("-Xlint", "-deprecation", "-unchecked", "-Xcheckinit", "-encoding", "utf8"),
    javacOptions ++= Seq("-Xlint:unchecked", "-encoding", "utf8")
  )

  val appVersion      = "0.1-SNAPSHOT"

  val coreDependencies = Seq(
    "org.scalaz" %% "scalaz-core" % "7.0.0-M7" withSources(),
    "org.specs2" %% "specs2" % "latest.release" % "test"
  )

  // val core = Project(appCore, prjPath(appCore), applicationVersion=appVersion, dependencies=coreDependencies).settings(
  val core = Project(
    id = appCore, 
    base = prjPath(appCore),
    aggregate = Nil, // : => Seq[ProjectReference] = Nil, 
    // dependencies = coreDependencies, // : => Seq[ClasspathDep[ProjectReference]] = Nil,
    delegates = Nil // : => Seq[ProjectReference] = Nil,
  ).settings(
    scalaSettings:_*
  )


  val front = play.Project(
    name = appFront,
    applicationVersion = appVersion,
    dependencies = Nil, 
    path = prjPath(appFront)
  ) settings (
    // 
  ) dependsOn core

}
