lazy val commonSettings = Seq(
  version := "0.0.1-SNAPSHOT",
  organization := "com.dbrsn",
  scalaVersion := "2.12.1",
  scalacOptions ++= Seq(
    "-deprecation", // Emit warning and location for usages of deprecated APIs.
    "-feature", // Emit warning and location for usages of features that should be imported explicitly.
    "-unchecked", // Enable additional warnings where generated code depends on assumptions.
    "-Xfatal-warnings", // Fail the compilation if there are any warnings.
    "-Xlint", // Enable recommended additional warnings.
    "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
    "-Ywarn-dead-code", // Warn when dead code is identified.
    "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
    "-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
    "-Ywarn-numeric-widen" // Warn when numerics are widened.
  )
)

lazy val `data-train-core` = (project in file("data-train-core"))
  .settings(commonSettings: _*)

lazy val `data-train-aws` = (project in file("data-train-aws"))
  .settings(commonSettings: _*)
  .dependsOn(`data-train-core`)

lazy val `data-train-image` = (project in file("data-train-image"))
  .settings(commonSettings: _*)
  .dependsOn(`data-train-core`)

lazy val `data-train-slick` = (project in file("data-train-slick"))
  .settings(commonSettings: _*)
  .dependsOn(`data-train-core`)

lazy val `data-train` = (project in file("."))
  .settings(commonSettings: _*)
  .dependsOn(`data-train-aws`)
  .dependsOn(`data-train-image`)
  .dependsOn(`data-train-slick`)
  .aggregate(`data-train-aws`)
  .aggregate(`data-train-image`)
  .aggregate(`data-train-slick`)
