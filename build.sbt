// https://mvnrepository.com/artifact/org.spire-math/kind-projector_2.12
val `version.kind-projector` = "0.9.3"

addCompilerPlugin("org.spire-math" %% "kind-projector" % `version.kind-projector`)

lazy val commonSettings = Seq(
  version := "0.0.1-SNAPSHOT",
  organization := "com.dbrsn",
  scalaVersion := "2.12.1",
  crossScalaVersions := Seq("2.12.1", "2.11.8"),
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

lazy val `data-train-scrimage` = (project in file("data-train-scrimage"))
  .settings(commonSettings: _*)
  .dependsOn(`data-train-core`)

lazy val `data-train-slick` = (project in file("data-train-slick"))
  .settings(commonSettings: _*)
  .dependsOn(`data-train-core`)

lazy val `data-train-slick-postgresql` = (project in file("data-train-slick-postgresql"))
  .settings(commonSettings: _*)
  .dependsOn(`data-train-slick`)

lazy val `data-train-file` = (project in file("data-train-file"))
  .settings(commonSettings: _*)
  .dependsOn(`data-train-core`)

lazy val `data-train` = (project in file("."))
  .settings(commonSettings: _*)
  .dependsOn(`data-train-aws`)
  .dependsOn(`data-train-scrimage`)
  .dependsOn(`data-train-slick`)
  .dependsOn(`data-train-slick-postgresql`)
  .dependsOn(`data-train-file`)
  .aggregate(`data-train-aws`)
  .aggregate(`data-train-scrimage`)
  .aggregate(`data-train-slick`)
  .aggregate(`data-train-slick-postgresql`)
  .aggregate(`data-train-file`)
