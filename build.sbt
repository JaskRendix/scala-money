val specs2version = "4.20.2"
val specs2 = "org.specs2" %% "specs2-core" % specs2version % Test

val coreDeps = Seq(
  "org.specs2" %% "specs2-core"       % specs2version % Test,
  "org.specs2" %% "specs2-scalacheck" % specs2version % Test,
  "org.scalacheck" %% "scalacheck"    % "1.17.0"       % Test
)

lazy val commonSettings = Seq(
  organization := "com.lambdista",
  scalaVersion := "3.3.3",
  scalacOptions ++= Seq(
    "-encoding", "utf8",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-language:implicitConversions",
    "-language:postfixOps"
  ),
  scalafmtOnCompile := true,
  libraryDependencies ++= coreDeps
)

lazy val core = (project in file("core"))
  .settings(
    moduleName := "money",
    commonSettings
  )

lazy val examples = (project in file("examples"))
  .dependsOn(core)
  .settings(
    commonSettings,
    Compile / run / mainClass := Some("money.example.Usage")
  )