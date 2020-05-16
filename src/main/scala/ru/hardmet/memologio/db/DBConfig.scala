package ru.hardmet.memologio.db

import derevo.derive
import derevo.pureconfig.config

@derive(config)
case class DBConfig(
    url: String,
    user: String,
    password: String
)
