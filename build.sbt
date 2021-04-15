import _root_.sbt.Keys._

name := "memologio"

version := "0.0.0"

scalaVersion := "2.13.2"

scalacOptions := List(
  "-encoding",
  "utf8",
  "-feature",
  "-unchecked",
  "-deprecation",
  "-target:jvm-1.8",
  "-language:_",
  "-Ymacro-annotations"
)
bloopExportJarClassifiers in Global := Some(Set("sources"))

val circeVersion = "0.12.1"
val circeDerVersion = "0.12.0-M7"
val catsVersion = "2.0.0"
val catsEffectVersion = "2.0.0"
val fs2Version = "2.0.0"
val http4sVersion = "0.21.11"
val derevoVersion = "0.11.2"
val pureconfigVersion = "0.12.3"
val doobieVersion = "0.8.8"
val skunkVersion = "0.0.21"
val logbackVersion = "1.2.3"
val tofuVersion = "0.7.4"


addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.patch)
addCompilerPlugin("org.augustjune" %% "context-applied" % "0.1.4")
addCompilerPlugin("org.wartremover" %% "wartremover" % "2.4.5" cross CrossVersion.full)

scalacOptions += "-P:wartremover:traverser:org.wartremover.warts.AsInstanceOf"
scalacOptions += "-P:wartremover:traverser:org.wartremover.warts.MutableDataStructures"
//scalacOptions += "-P:wartremover:traverser:org.wartremover.warts.Null"
scalacOptions += "-P:wartremover:traverser:org.wartremover.warts.Return"
scalacOptions += "-P:wartremover:traverser:org.wartremover.warts.Throw"
scalacOptions += "-P:wartremover:traverser:org.wartremover.warts.Var"
scalacOptions += "-P:wartremover:traverser:org.wartremover.warts.While"

libraryDependencies += scalaOrganization.value % "scala-reflect" % scalaVersion.value
libraryDependencies += scalaOrganization.value % "scala-compiler" % scalaVersion.value

libraryDependencies += "org.typelevel" %% "cats-core" % catsVersion
libraryDependencies += "org.typelevel" %% "cats-effect" % catsEffectVersion
libraryDependencies += "co.fs2" %% "fs2-core" % fs2Version
libraryDependencies += "io.estatico" %% "newtype" % "0.4.4"

libraryDependencies += "io.circe" %% "circe-core" % circeVersion
libraryDependencies += "io.circe" %% "circe-generic" % circeVersion
libraryDependencies += "io.circe" %% "circe-parser" % circeVersion
libraryDependencies += "io.circe" %% "circe-derivation" % circeDerVersion
libraryDependencies += "io.circe" %% "circe-derivation-annotations" % circeDerVersion
libraryDependencies += "org.manatki" %% "derevo-pureconfig" % derevoVersion
libraryDependencies += "com.github.pureconfig" %% "pureconfig-core" % pureconfigVersion

libraryDependencies += "org.http4s" %% "http4s-blaze-server" % http4sVersion
libraryDependencies += "org.http4s" %% "http4s-dsl" % http4sVersion
libraryDependencies += "org.http4s" %% "http4s-circe" % http4sVersion
libraryDependencies += "org.tpolecat" %% "doobie-core" % doobieVersion
libraryDependencies += "org.tpolecat" %% "doobie-postgres" % doobieVersion
libraryDependencies += "org.tpolecat" %% "skunk-core" % skunkVersion
libraryDependencies += "ch.qos.logback" % "logback-classic" % logbackVersion
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
libraryDependencies += "ru.tinkoff" %% "tofu-logging" % tofuVersion

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.8"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % Test
libraryDependencies += "org.typelevel" %% "discipline-scalatest" % "2.1.0" % Test
libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.15.1" % Test
libraryDependencies += "org.mockito" % "mockito-core" % "3.0.0" % Test
libraryDependencies += "org.scalatestplus" %% "scalacheck-1-14" % "3.2.2.0" % Test
libraryDependencies += "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % "1.2.5" % Test

Defaults.itSettings
configs(IntegrationTest)
