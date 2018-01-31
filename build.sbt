name := "test-sbt-release"

scalaVersion := "2.11.8"

lazy val root = (project in file("."))
  .settings(Release.settings)
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test"