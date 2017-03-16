// https://mvnrepository.com/artifact/org.typelevel/cats_2.12
val `version.cats` = "0.9.0"
// https://mvnrepository.com/artifact/io.circe/circe-core_2.12
val `version.circe` = "0.7.0"

libraryDependencies += "org.typelevel" %% "cats" % `version.cats`
libraryDependencies += "io.circe" %% "circe-generic" % `version.circe`
libraryDependencies += "io.circe" %% "circe-parser" % `version.circe`
