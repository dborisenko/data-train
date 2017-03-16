// https://mvnrepository.com/artifact/org.typelevel/cats_2.12
val `version.cats` = "0.9.0"
// https://mvnrepository.com/artifact/io.circe/circe-core_2.12
val `version.circe` = "0.7.0"
// https://mvnrepository.com/artifact/com.beachape/enumeratum_2.12
val `version.enumeratum` = "1.5.8"
// https://mvnrepository.com/artifact/com.chuusai/shapeless_2.12
val `version.shapeless` = "2.3.2"

libraryDependencies += "org.typelevel" %% "cats" % `version.cats`
libraryDependencies += "io.circe" %% "circe-generic" % `version.circe`
libraryDependencies += "io.circe" %% "circe-parser" % `version.circe`
libraryDependencies += "com.beachape" %% "enumeratum" % `version.enumeratum`
libraryDependencies += "com.chuusai" %% "shapeless" % `version.shapeless`
