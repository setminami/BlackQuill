import AssemblyKeys._

import Keys._

import sbt._

import sbtassembly.Plugin._


name := "BlackQuill"

version := "0.1.7"

scalaVersion := "2.10.0"

organization := "net.setminami"

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

seq(assemblySettings: _*)

seq(aetherPublishSettings: _*)

mainClass in assembly := Some("org.blackquill.main")

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case _ => MergeStrategy.first
  }
}

scalacOptions ++= Seq("-encoding","UTF-8")

libraryDependencies ++= Seq(
	"org.apache.commons" % "commons-lang3" % "3.1",
	"commons-io" % "commons-io" % "2.4",
  "commons-logging" % "commons-logging" % "1.0.4",
  "uk.ac.ed.ph.snuggletex" % "snuggletex-core" % "1.2.2",
  "org.scalacheck" %% "scalacheck" % "1.10.1" % "test"
)


resolvers ++= Seq(
  "snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "releases" at "http://oss.sonatype.org/content/repositories/releases",
  "www2.ph.ed.ac.uk-releases" at "http://www2.ph.ed.ac.uk/maven2",
 "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases"
)

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := (
  <url>http://setminami.net/BlackQuill</url>
  <licenses>
    <license>
      <name>BSD-style</name>
      <url>http://www.opensource.org/licenses/bsd-license.php</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:setminami/BlackQuill</url>
    <connection>scm:git:git@github.com:setminami/BlackQuill.git</connection>
  </scm>
  <developers>
    <developer>
      <id>SetMinami</id>
      <name>Setsushi Minami</name>
      <url>http://setminami.net</url>
    </developer>
  </developers>)

assemblySettings

