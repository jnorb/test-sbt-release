name := "test-sbt-release"

scalaVersion := "2.11.8"

lazy val root = (project in file("."))
  .settings(Release.settings)