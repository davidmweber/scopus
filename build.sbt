name := "Scopus"

organization := "za.co.monadic"

version := "0.1.6"

scalaVersion := "2.10.4"

crossScalaVersions := Seq("2.10.4", "2.11.1")

licenses += "CC BY 4.0." -> url("https://creativecommons.org/licenses/by/4.0/")

homepage := Some(url("https://github.com/davidmweber/scopus"))

libraryDependencies ++= List(
  "org.scalatest" %% "scalatest" % "2.1.3" % "test"
)

publishMavenStyle := true

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

