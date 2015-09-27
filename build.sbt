val CONSUMERS = Set("C:/JavaWork/Scala/algorithmicProblems", "C:/JavaWork/Scala/LateX Editor 4.0")

lazy val distribute = taskKey[Unit]("Produces (if needed) Distributes the assembly jar and distributes it to all the consumers of the library")

lazy val commonSettings = Seq(
    version := "1.0",
    scalaVersion := "2.11.7",
    initialize := {
	  val _ = initialize.value
	  if (sys.props("java.specification.version") != "1.8")
	    sys.error("Java 8 is required for this project.")
	},
	exportJars := true,
	javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
	scalacOptions += "-target:jvm-1.8",
	distribute := { 
		import java.io.File
		import java.nio.file.Files
		import java.nio.file.Paths
		import java.nio.file.StandardCopyOption.REPLACE_EXISTING
		
		def copyFileToConsumer(file: File, consumerPath: String) = Files.copy(file.toPath, Paths.get(consumerPath + "/lib/" + file.getName), REPLACE_EXISTING)

		val assemblyFile = assembly.value
		val sourcesFile  = (Keys.packageSrc in Compile).value
		
		for (consumer <- CONSUMERS) {
			copyFileToConsumer(assemblyFile, consumer)
			copyFileToConsumer(sourcesFile , consumer)
		}


	},
	assemblyExcludedJars in assembly := (fullClasspath in assembly).value.filter(_.data.getName.toLowerCase.contains("scala")),
	libraryDependencies += "junit" % "junit" % "4.11" % "test"
)

lazy val root = 
	(project in file("."))
		.settings(commonSettings: _*)
		.settings(name := "dici-utils")

