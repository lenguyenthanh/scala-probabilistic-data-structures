ThisBuild / tlBaseVersion := "0.0"
ThisBuild / scalaVersion  := "3.3.8"
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / scalacOptions += "-Yfuture-lazy-vals"
ThisBuild / semanticdbEnabled := true
ThisBuild / organization      := "se.thanh.pds"
ThisBuild / organizationName  := "Thanh Le"
ThisBuild / developers        := List(
  tlGitHubDev("lenguyenthanh", "Thanh Le")
)
ThisBuild / githubWorkflowJavaVersions := Seq(
  JavaSpec.temurin("18"),
  JavaSpec.temurin("21"),
  JavaSpec.temurin("25")
)
ThisBuild / tlCiMimaBinaryIssueCheck            := true
ThisBuild / tlCiDependencyGraphJob              := false
ThisBuild / tlSiteJavaVersion                   := JavaSpec.temurin("25")
ThisBuild / tlSitePublishBranch                 := Some("main")
ThisBuild / githubWorkflowPublishTargetBranches := Seq()
ThisBuild / githubWorkflowBuild += WorkflowStep.Sbt(
  List("check"),
  name = Some("Check formatting and Scalafix"),
  cond = Some("matrix.java == 'temurin@25' && matrix.os == 'ubuntu-22.04'")
)

ThisBuild / tlJdkRelease    := Some(18)
ThisBuild / tlFatalWarnings := true

lazy val bloomFilter = project
  .in(file("modules/bloom-filter"))
  .settings(
    name := "Bloom Filter",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit"            % "1.3.5" % Test,
      "org.scalameta" %% "munit-scalacheck" % "1.3.0" % Test,
      "org.typelevel" %% "discipline-munit" % "2.0.0" % Test
    ),
    Test / fork := true
  )

lazy val benchmark = project
  .in(file("modules/benchmark"))
  .dependsOn(bloomFilter)
  .enablePlugins(JmhPlugin, NoPublishPlugin)
  .settings(
    name := "benchmark",
    resolvers ++= Seq("lila-maven".at("https://raw.githubusercontent.com/lichess-org/lila-maven/master")),
    Jmh / javacOptions := (Jmh / javacOptions).value
      .filterNot(_ == "-Xlint:all") :+ "-Xlint:all,-path",
    libraryDependencies +=
      "com.github.alexandrnikitin" %% "bloom-filter" % "0.13.1_lila-1"
  )

lazy val docs = project
  .in(file("site"))
  .dependsOn(bloomFilter)
  .enablePlugins(TypelevelSitePlugin)
  .settings(
    name                   := "Scala probabilistic data structures",
    description            := "Probabilistic data structures for Scala 3",
    githubWorkflowJobSetup :=
      List(WorkflowStep.CheckoutFull, WorkflowStep.SetupSbt) ++
        WorkflowStep.SetupJava(List(tlSiteJavaVersion.value))
  )

lazy val root = project
  .in(file("."))
  .enablePlugins(NoPublishPlugin)
  .settings(
    name := "Scala probabilistic data structures"
  )
  .aggregate(bloomFilter, benchmark)

addCommandAlias("prepare", "scalafixAll; scalafmtAll")
addCommandAlias("check", "; scalafixAll --check ; scalafmtCheckAll")
