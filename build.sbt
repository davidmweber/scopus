name := "Scopus"

organization := "za.co.monadic"

version := "0.1.2-SNAPSHOT"

scalaVersion := "2.10.3"

libraryDependencies ++= List(
  "org.scalatest" %% "scalatest" % "2.0" % "test"
)

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

licenses := Seq(" Creative Commons Attribution 4.0 International License." -> url("https://creativecommons.org/licenses/by/4.0/"))

homepage := Some(url("https://github.com/davidmweber/scopus"))

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

