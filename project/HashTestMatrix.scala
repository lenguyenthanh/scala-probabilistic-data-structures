import sbt.{ ForkOptions, TestDefinition, Tests }

object HashTestMatrix {

  private val PackageName = "se.thanh.pds.bloomfilter.hashmatrix"

  final private case class UnsafeMode(
      name: String,
      suitePrefix: String,
      argument: Option[String]
  )

  final private case class Configuration(
      mode: UnsafeMode,
      addOpens: Boolean
  ) {
    val suiteName: String =
      s"$PackageName.Hash${mode.suitePrefix}${if (addOpens) "Opened" else "Closed"}Spec"

    val groupName: String =
      s"hash-${mode.name}-${if (addOpens) "opened" else "closed"}"

    val jvmOptions: Vector[String] =
      mode.argument.toVector.map(value => s"--sun-misc-unsafe-memory-access=$value") ++
        (if (addOpens) Vector("--add-opens=java.base/java.lang=ALL-UNNAMED") else Vector.empty) ++
        Vector(
          s"-Dhash.matrix.unsafe-mode=${mode.name}",
          s"-Dhash.matrix.add-opens=$addOpens"
        )
  }

  private val modes = Vector(
    UnsafeMode("default", "Default", None),
    UnsafeMode("allow", "Allow", Some("allow")),
    UnsafeMode("warn", "Warn", Some("warn")),
    UnsafeMode("debug", "Debug", Some("debug")),
    UnsafeMode("deny", "Deny", Some("deny"))
  )

  private val configurations =
    for {
      mode     <- modes
      addOpens <- Vector(false, true)
    } yield Configuration(mode, addOpens)

  private val matrixSuiteNames = configurations.iterator.map(_.suiteName).toSet

  def groups(
      tests: Seq[TestDefinition],
      inheritedForkOptions: ForkOptions
  ): Seq[Tests.Group] = {
    val ordinaryTests = tests.filterNot(test => matrixSuiteNames(test.name))
    val ordinaryGroup =
      Tests.Group("ordinary", ordinaryTests, Tests.SubProcess(inheritedForkOptions))

    val javaFeature              = Runtime.version().feature()
    val applicableConfigurations =
      configurations.filter(configuration => configuration.mode.argument.isEmpty || javaFeature >= 23)

    ordinaryGroup +: applicableConfigurations.map { configuration =>
      val groupTests = tests.filter(_.name == configuration.suiteName)
      require(
        groupTests.size == 1,
        s"Expected exactly one test definition for ${configuration.suiteName}, found ${groupTests.size}"
      )
      val forkOptions = inheritedForkOptions.withRunJVMOptions(
        inheritedForkOptions.runJVMOptions ++ configuration.jvmOptions
      )
      Tests.Group(configuration.groupName, groupTests, Tests.SubProcess(forkOptions))
    }
  }
}
