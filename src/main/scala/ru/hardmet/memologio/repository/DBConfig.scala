package ru.hardmet.memologio.repository

import derevo.derive
import derevo.pureconfig.config

@derive(config)
case class DBConfig(
    host: String,
    port: Int,
    database: String,
    user: String,
    password: String
)
