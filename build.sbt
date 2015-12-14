
sonatypeSettings

name := "Scopus"

organization := "za.co.monadic"

version := "0.3.6"

scalaVersion := "2.11.7"

fork in Test := true

licenses += "CC BY 4.0." -> url("https://creativecommons.org/licenses/by/4.0/")

homepage := Some(url("https://github.com/davidmweber/scopus"))

libraryDependencies ++= List(
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)

publishMavenStyle := true

// sbt publishSigned
// sbt sonatypeRelease
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := { _ => false }

pomExtra :=
    <scm>
      <url>git@github.com:davidmweber/scopus.git</url>
      <connection>scm:git:git@github.com:davidmweber/scopus.git</connection>
    </scm>
    <developers>
      <developer>
        <id>davidmweber</id>
        <name>David Weber</name>
        <url>https://github.com/davidmweber</url>
      </developer>
    </developers>

