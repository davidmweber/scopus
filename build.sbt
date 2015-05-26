import SonatypeKeys._

sonatypeSettings

name := "Scopus"

organization := "za.co.monadic"

version := "0.3.5"

scalaVersion := "2.11.6"

fork in Test := true

crossScalaVersions := Seq("2.10.5", "2.11.6")

licenses += "CC BY 4.0." -> url("https://creativecommons.org/licenses/by/4.0/")

homepage := Some(url("https://github.com/davidmweber/scopus"))

// Needed only for IntelliJ (13.1.3) because ScalaTest cannot find the XML library it needs
libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMajor)) if scalaMajor >= 11 =>
      libraryDependencies.value :+ "org.scala-lang.modules" %% "scala-xml" % "1.0.2" % "test"
    case _ =>
      libraryDependencies.value
  }
}

libraryDependencies ++= List(
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
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

