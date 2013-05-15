name := "BlackQuill"

version := "0.1.0"

scalaVersion := "2.9.2"

scalacOptions ++= Seq("-encoding","UTF-8")

libraryDependencies ++= Seq(
	"org.apache.commons" % "commons-lang3" % "3.1",
	"commons-io" % "commons-io" % "2.4",
	"org.specs2" %% "specs2" % "1.12.3" % "test"
)


resolvers ++= Seq(
  "snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "releases" at "http://oss.sonatype.org/content/repositories/releases"
)
