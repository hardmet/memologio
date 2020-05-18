import _root_.sbt.Keys._

name := "memologio"

version := "1.0"

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

val akkaVersion       = "2.6.5"
val akkaHttpVersion   = "10.1.10"
val circeVersion      = "0.12.1"
val circeDerVersion   = "0.12.0-M7"
val catsVersion       = "2.0.0"
val catsEffectVersion = "2.0.0"
val fs2Version        = "2.0.0"
val shapelessVersion  = "2.3.3"
val simulacrumVersion = "1.0.0"
val zioVersion        = "1.0.0-RC18-2"
val zioCatsVersion    = "2.0.0.0-RC12"
val tofuVersion       = "0.7.4"
val tschemaVersion    = "0.12.2"
val derevoVersion     = "0.11.2"
val korolevVersion    = "0.14.0"
val magnoliaVersion   = "0.12.3"
val sttpVersion       = "2.0.7"
val pureconfigVersion = "0.12.3"
val doobieVersion     = "0.8.8"
val logbackVersion    = "1.2.3"

libraryDependencies += "org.scalactic"  %% "scalactic"            % "3.0.8"
libraryDependencies += "org.scalatest"  %% "scalatest"            % "3.0.8" % "it,test"
libraryDependencies += "org.typelevel"  %% "discipline-scalatest" % "1.0.1"
libraryDependencies += "org.scalacheck" %% "scalacheck"           % "1.14.1-RC2" % "test"
libraryDependencies += "org.mockito"    % "mockito-core"          % "3.0.0" % "test"

libraryDependencies += "com.typesafe.akka" %% "akka-actor"       % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-stream"      % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-remote"      % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j"       % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http"        % akkaHttpVersion

libraryDependencies += "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test"
libraryDependencies += "com.typesafe.akka" %% "akka-testkit"      % akkaVersion     % "test"

libraryDependencies += "org.typelevel" %% "cats-core"        % catsVersion
libraryDependencies += "org.typelevel" %% "cats-free"        % catsVersion
libraryDependencies += "org.typelevel" %% "cats-laws"        % catsVersion
libraryDependencies += "org.typelevel" %% "cats-laws"        % catsVersion % "test"
libraryDependencies += "org.typelevel" %% "cats-effect"      % catsEffectVersion
libraryDependencies += "co.fs2"        %% "fs2-core"         % fs2Version
libraryDependencies += "co.fs2"        %% "fs2-io"           % fs2Version
libraryDependencies += "dev.zio"       %% "zio"              % zioVersion
libraryDependencies += "dev.zio"       %% "zio-streams"      % zioVersion
libraryDependencies += "dev.zio"       %% "zio-interop-cats" % zioCatsVersion

libraryDependencies += "io.circe" %% "circe-core"                   % circeVersion
libraryDependencies += "io.circe" %% "circe-generic"                % circeVersion
libraryDependencies += "io.circe" %% "circe-parser"                 % circeVersion
libraryDependencies += "io.circe" %% "circe-generic-extras"         % circeVersion
libraryDependencies += "io.circe" %% "circe-derivation"             % circeDerVersion
libraryDependencies += "io.circe" %% "circe-derivation-annotations" % circeDerVersion

libraryDependencies += scalaOrganization.value % "scala-reflect"  % scalaVersion.value
libraryDependencies += scalaOrganization.value % "scala-compiler" % scalaVersion.value

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging"  % "3.9.2"
libraryDependencies += "ch.qos.logback"             % "logback-classic" % "1.2.3"

libraryDependencies += "com.chuusai"   %% "shapeless"  % shapelessVersion
libraryDependencies += "org.typelevel" %% "simulacrum" % simulacrumVersion

libraryDependencies += "com.github.fomkin" %% "korolev"                     % korolevVersion
libraryDependencies += "com.github.fomkin" %% "korolev-server"              % korolevVersion
libraryDependencies += "com.github.fomkin" %% "korolev-server-akkahttp"     % korolevVersion
libraryDependencies += "com.github.fomkin" %% "korolev-cats-effect-support" % korolevVersion
libraryDependencies += "com.github.fomkin" %% "korolev-async"               % korolevVersion

libraryDependencies += "com.propensive" %% "magnolia" % magnoliaVersion

addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1")
addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.11.0" cross CrossVersion.patch)

addCompilerPlugin("org.wartremover" %% "wartremover" % "2.4.5" cross CrossVersion.full)

scalacOptions += "-P:wartremover:traverser:org.wartremover.warts.AsInstanceOf"
scalacOptions += "-P:wartremover:traverser:org.wartremover.warts.MutableDataStructures"
//scalacOptions += "-P:wartremover:traverser:org.wartremover.warts.Null"
scalacOptions += "-P:wartremover:traverser:org.wartremover.warts.Return"
//scalacOptions += "-P:wartremover:traverser:org.wartremover.warts.Throw"
scalacOptions += "-P:wartremover:traverser:org.wartremover.warts.Var"
scalacOptions += "-P:wartremover:traverser:org.wartremover.warts.While"

//val appSettings = List(
//  scalaVersion := "2.13.2",
//  libraryDependencies += "ru.tinkoff"                   %% "tofu"                          % tofuVersion,
//  libraryDependencies += "ru.tinkoff"                   %% "tofu-logging"                  % tofuVersion,
//  libraryDependencies += "ru.tinkoff"                   %% "tofu-derivation"               % tofuVersion,
//  libraryDependencies += "ru.tinkoff"                   %% "tofu-zio-interop"              % tofuVersion,
//  libraryDependencies += "ru.tinkoff"                   %% "typed-schema-finagle-zio"      % tschemaVersion,
//  libraryDependencies += "ru.tinkoff"                   %% "typed-schema-finagle-env"      % tschemaVersion,
//  libraryDependencies += "ru.tinkoff"                   %% "typed-schema-finagle-custom"   % tschemaVersion,
//  libraryDependencies += "ru.tinkoff"                   %% "typed-schema-swagger"          % tschemaVersion,
//  libraryDependencies += "ru.tinkoff"                   %% "typed-schema-swagger-ui"       % tschemaVersion,
//  libraryDependencies += "org.manatki"                  %% "derevo-cats"                   % derevoVersion,
//  libraryDependencies += "org.manatki"                  %% "derevo-pureconfig"             % derevoVersion,
//  libraryDependencies += "org.manatki"                  %% "derevo-circe"                  % derevoVersion,
//  libraryDependencies += "org.manatki"                  %% "derevo-tethys"                 % derevoVersion,
//  libraryDependencies += "com.softwaremill.sttp.client" %% "core"                          % sttpVersion,
//  libraryDependencies += "com.softwaremill.sttp.client" %% "async-http-client-backend-zio" % sttpVersion,
//  libraryDependencies += "com.softwaremill.sttp.client" %% "circe"                         % sttpVersion,
//  libraryDependencies += "org.tpolecat"                 %% "doobie-core"                   % doobieVersion,
//  libraryDependencies += "org.tpolecat"                 %% "doobie-h2"                     % doobieVersion,
//  libraryDependencies += "ch.qos.logback"               % "logback-classic"                % logbackVersion,
//  libraryDependencies += "com.typesafe.akka"            %% "akka-actor-typed"              % akkaVersion,
//  addCompilerPlugin(
//    "com.olegpy" %% "better-monadic-for" % "0.3.1"
//  ),
//  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.patch),
//  scalacOptions := List(
//    "-encoding",
//    "utf8",
//    "-feature",
//    "-Ymacro-annotations",
//    "-deprecation",
//    "-language:_"
//  )
//)

libraryDependencies += "ru.tinkoff"                   %% "tofu"                          % tofuVersion
libraryDependencies += "ru.tinkoff"                   %% "tofu-logging"                  % tofuVersion
libraryDependencies += "ru.tinkoff"                   %% "tofu-derivation"               % tofuVersion
libraryDependencies += "ru.tinkoff"                   %% "tofu-zio-interop"              % tofuVersion
libraryDependencies += "ru.tinkoff"                   %% "typed-schema-finagle-zio"      % tschemaVersion
libraryDependencies += "ru.tinkoff"                   %% "typed-schema-finagle-env"      % tschemaVersion
libraryDependencies += "ru.tinkoff"                   %% "typed-schema-finagle-custom"   % tschemaVersion
libraryDependencies += "ru.tinkoff"                   %% "typed-schema-swagger"          % tschemaVersion
libraryDependencies += "ru.tinkoff"                   %% "typed-schema-swagger-ui"       % tschemaVersion
libraryDependencies += "org.manatki"                  %% "derevo-cats"                   % derevoVersion
libraryDependencies += "org.manatki"                  %% "derevo-pureconfig"             % derevoVersion
libraryDependencies += "org.manatki"                  %% "derevo-circe"                  % derevoVersion
libraryDependencies += "org.manatki"                  %% "derevo-tethys"                 % derevoVersion
libraryDependencies += "com.softwaremill.sttp.client" %% "core"                          % sttpVersion
libraryDependencies += "com.softwaremill.sttp.client" %% "async-http-client-backend-zio" % sttpVersion
libraryDependencies += "com.softwaremill.sttp.client" %% "circe"                         % sttpVersion
libraryDependencies += "org.tpolecat"                 %% "doobie-core"                   % doobieVersion
libraryDependencies += "org.tpolecat"                 %% "doobie-h2"                     % doobieVersion
libraryDependencies += "org.tpolecat"                 %% "doobie-postgres"               % doobieVersion
libraryDependencies += "ch.qos.logback"               % "logback-classic"                % logbackVersion
libraryDependencies += "com.typesafe.akka"            %% "akka-actor-typed"              % akkaVersion

addCompilerPlugin(
  "com.olegpy" %% "better-monadic-for" % "0.3.1"
)
addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.patch)

Defaults.itSettings
configs(IntegrationTest)
