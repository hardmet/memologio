package ru.hardmet.memologio.config

import derevo.derive
import derevo.pureconfig.config
import pureconfig.ConfigSource
import ru.hardmet.memologio.posts.PostsConfig
import ru.hardmet.memologio.db.DBConfig
import ru.hardmet.memologio.http.ServerConfig
import zio.blocking.{Blocking, effectBlocking}
import zio.{RLayer, ZLayer}

@derive(config)
case class MemologioConfig(
                            server: ServerConfig,
                            posts: PostsConfig,
                            db: DBConfig
)

object Config {
  val live: RLayer[Blocking, Config] =
    ZLayer.fromEffect(effectBlocking(ConfigSource.default.loadOrThrow[MemologioConfig]))
}
