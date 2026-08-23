inThisBuild(
  Seq(
    scalaVersion := "3.3.8",
    versionScheme := Some("early-semver"),
    version := "0.0.1",
    scalacOptions += "-Wall"
  )
)

lazy val bloomFilter = project
  .in(file("modules/bloom-filter"))
  .settings(
    name := "Bloom Filter"
  )

lazy val root = project
  .in(file("."))
  .settings(
    name := "Scala probabilistic data structures",
    version := "0.1.0-SNAPSHOT",
  ).aggregate(bloomFilter)

addCommandAlias("prepare", "scalafixAll; scalafmtAll")
addCommandAlias("check", "; scalafixAll --check ; scalafmtCheckAll")
