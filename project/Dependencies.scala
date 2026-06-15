import sbt._

object Dependencies {
  lazy val projectScalaVersion = "3.3.3" // Target LTS version

  // Versions
  // Specs2 4.20.0+ has much better Scala 3 support
  lazy val specs2version = "4.20.2" 

  // Libraries
  val specs2 = "org.specs2" %% "specs2-core" % specs2version % Test

  // Projects
  val coreDeps = Seq(specs2)
}