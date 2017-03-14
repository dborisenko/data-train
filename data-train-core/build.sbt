// https://mvnrepository.com/artifact/org.spire-math/kind-projector_2.12
val `version.kind-projector` = "0.9.3"
// https://mvnrepository.com/artifact/joda-time/joda-time
val `version.joda-time` = "2.9.7"
// https://mvnrepository.com/artifact/org.typelevel/cats_2.12
val `version.cats` = "0.9.0"

addCompilerPlugin("org.spire-math" %% "kind-projector" % `version.kind-projector`)

libraryDependencies += "joda-time" % "joda-time" % `version.joda-time`
libraryDependencies += "org.typelevel" %% "cats" % `version.cats`

scalacOptions := Seq("-Ypartial-unification")
