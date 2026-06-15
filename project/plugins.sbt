lazy val scoverageVersion  = "2.2.2"
lazy val sbtUpdatesVersion = "0.6.4"
lazy val scalafmtVersion   = "2.5.2"

// Plugins
addSbtPlugin("org.scalameta"    % "sbt-scalafmt"   % scalafmtVersion)
addSbtPlugin("com.timushev.sbt" % "sbt-updates"    % sbtUpdatesVersion)
addSbtPlugin("org.scoverage"    % "sbt-scoverage"  % scoverageVersion)

// Temporarily disabled mdoc to unblock compilation
// addSbtPlugin("org.scalameta"    % "sbt-mdoc"       % "2.5.4")