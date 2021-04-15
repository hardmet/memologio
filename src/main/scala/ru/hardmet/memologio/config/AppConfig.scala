package ru.hardmet.memologio
package config

import cats.effect.{ContextShift, Sync}
import derevo.derive
import derevo.pureconfig.config
import pureconfig.ConfigSource

@derive(config)
case class AppConfig(server: ServerConfig, db: DBConfig)

object AppConfig {
  def init[F[_] : Sync : ContextShift]: F[AppConfig] =
    F.delay(ConfigSource.default.loadOrThrow[AppConfig])
}
