package ru.hardmet.memologio

import com.twitter.finagle.http.Response
import ru.tinkoff.tschema.swagger.SwaggerBuilder

trait HttpModule {
  def route: MemologioHttp[Response]
  def swag: SwaggerBuilder
}
