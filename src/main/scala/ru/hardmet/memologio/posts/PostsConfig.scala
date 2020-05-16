package ru.hardmet.memologio.posts

import derevo.derive
import derevo.pureconfig.config

@derive(config)
case class PostsConfig(initial: List[PostData])

