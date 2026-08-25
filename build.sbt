inThisBuild(
  Seq(
    scalaVersion := "3.3.8",
    versionScheme := Some("early-semver"),
    version := "0.0.1",
    scalacOptions ++= Seq("-Wall", "-Yfuture-lazy-vals", "-java-output-version", "17")
  )
)

lazy val bloomFilter = project
  .in(file("modules/bloom-filter"))
  .settings(
    name := "Bloom Filter",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.3.5" % Test,
      "org.scalameta" %% "munit-scalacheck" % "1.3.0" % Test,
      "org.typelevel" %% "discipline-munit" % "2.0.0" % Test
    ),
    Test / fork := true,
    Test / javaOptions += "--add-opens=java.base/java.lang=ALL-UNNAMED"
  )

lazy val root = project
  .in(file("."))
  .settings(
    name := "Scala probabilistic data structures",
    version := "0.1.0-SNAPSHOT",
  ).aggregate(bloomFilter)

addCommandAlias("prepare", "scalafixAll; scalafmtAll")
addCommandAlias("check", "; scalafixAll --check ; scalafmtCheckAll")
