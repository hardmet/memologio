package ru.hardmet.memologio.http

import derevo.derive
import derevo.pureconfig.config

@derive(config)
case class ServerConfig(
    host: String,
    port: Int
)
