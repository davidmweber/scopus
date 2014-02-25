name := "Scopus"

version := "0.0"

scalaVersion := "2.10.3"

resolvers ++= List(
  "Spray repo" at "http://nightlies.spray.io",
  "sonatype-public" at "https://oss.sonatype.org/content/groups/public"
)

scalacOptions ++= Seq("-feature", "-language:postfixOps")

libraryDependencies ++= List(
  "org.scalatest" %% "scalatest" % "2.0" % "test"
)
