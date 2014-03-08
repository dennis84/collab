import sbt._
import Keys._
import spray.revolver.RevolverPlugin._
import com.typesafe.sbt.SbtNativePackager._

object Collab extends Build {

  lazy val defaults = super.settings ++ Seq(
    organization := "d01100100",
    version := "0.1",
    scalaVersion := "2.10.3",
    scalacOptions := Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-language:postfixOps"),
    libraryDependencies ++= Seq(
      "io.spray"            %  "spray-can"     % "1.3.0",
      "io.spray"            %  "spray-routing" % "1.3.0",
      "io.spray"            %  "spray-testkit" % "1.3.0",
      "io.spray"            %% "spray-json"    % "1.2.5",
      "com.typesafe.akka"   %% "akka-actor"    % "2.3.0",
      "com.typesafe.akka"   %% "akka-testkit"  % "2.3.0",
      "net.debasishg"       %% "redisreact"    % "0.3"),
    resolvers ++= Seq(
      "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
      "Typesafe Repository"    at "http://repo.typesafe.com/typesafe/releases/",
      "spray repo"             at "http://repo.spray.io/")
  ) ++ Project.defaultSettings

  val collab = Project(
    id = "collab",
    base = file("."),
    settings =
      defaults ++
      Revolver.settings ++
      packageArchetype.java_application)
}
