ThisBuild / version := "0.3.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.14"

lazy val root = (project in file("."))
  .settings(
    name := "flatland"
  )

// Add a separate main class for key testing
Compile / run / mainClass := Some("Main")
lazy val keyTest = taskKey[Unit]("Run the key test")
keyTest := (Compile / runMain).toTask(" KeyTest").value

libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.19"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % "test"
libraryDependencies += "org.jline" % "jline-terminal" % "3.21.0"