package ru.hardmet.memologio
package posts

import java.util.UUID

import derevo.circe.codec
import derevo.derive
import derevo.pureconfig.config
import ru.tinkoff.tschema.param.HttpParam
import ru.tinkoff.tschema.swagger.{AsOpenApiParam, Swagger}
import tofu.logging.derivation.loggable

@derive(codec, Swagger, config, loggable)
final case class PostData(
                           name: String,
                           description: String
                         )

@derive(codec, Swagger, loggable, HttpParam, AsOpenApiParam)
final case class PostId(uuid: UUID)
