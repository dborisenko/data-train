// https://mvnrepository.com/artifact/com.typesafe.slick/slick_2.12
val `version.slick` = "3.2.0"
// https://mvnrepository.com/artifact/io.underscore/slickless_2.12
val `version.slickless` = "0.3.1"
// https://mvnrepository.com/artifact/io.underscore/slickless_2.11
val `version.slickless_2.11` = "0.3.0"

libraryDependencies += "com.typesafe.slick" %% "slick" % `version.slick`
libraryDependencies += "io.underscore" %% "slickless" % {
  if (scalaVersion.value startsWith "2.12")
    `version.slickless`
  else
    `version.slickless_2.11`
}
