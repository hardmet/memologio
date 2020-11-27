package ru.hardmet.memologio
package config

import derevo.derive
import derevo.pureconfig.config

@derive(config)
case class ServerConfig(host: String, port: Int)
