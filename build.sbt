name := "Scopus"
organization := "za.co.monadic"
version := "0.5.0"
scalaVersion := "2.13.2"
crossScalaVersions := Seq("2.12.11", "2.13.2")
scalacOptions ++= Seq(
  "-deprecation"
)
fork in Test := true

organizationName := "David Weber"
startYear := Some(2020)
licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))
homepage := Some(url("https://github.com/davidmweber/scopus"))

libraryDependencies ++= List(
  "org.scalatest" %% "scalatest-shouldmatchers" % "3.2.0" % "test",
  "org.scalatest" %% "scalatest-funspec" % "3.2.0" % "test"
)

publishMavenStyle := true

// sbt publishSigned
// sbt sonatypeRelease
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := { _ =>
  false
}

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
