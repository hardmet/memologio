package ru.hardmet.memologio.resources

import java.util.Locale

import cats.instances.list._
import cats.syntax.foldable._
import cats.syntax.semigroupk._
import com.twitter.finagle.http.Response
import io.circe.Printer
import io.circe.syntax._
import ru.hardmet.memologio.http.Resource
import ru.hardmet.memologio.{MemologioHttp, http}
import ru.tinkoff.tschema.finagle.Routed
import ru.tinkoff.tschema.finagle.util.message
import ru.tinkoff.tschema.swagger
import ru.tinkoff.tschema.swagger.MkSwagger.PathSpec
import ru.tinkoff.tschema.swagger.{OpenApiInfo, PathDescription, SwaggerBuilder}
import zio._

object OpenAPI {
  private implicit val printer: Printer = Printer.spaces2.copy(dropNullValues = true)

  private val swaggerHttp: MemologioHttp[Response] = {
    val response = message.stringResponse(swagger.SwaggerIndex("/swagger", "/webjars"))
    response.setContentType("text/html(UTF-8)")
    Routed.checkPath[MemologioHttp, Response]("/swagger.php", ZIO.succeed(response))
  }

  private val swaggerResources: MemologioHttp[Response] = Resource.folder("/webjars", "/META-INF/resources")

  val swaggerBuilder: SwaggerBuilder = http.modules.foldMap(_.swag).map(PathSpec.path.update(_, "api" +: _))

  private val swaggerJson: MemologioHttp[Response] =
    RIO.effectTotal {
      val descriptions =
        PathDescription.utf8I18n("swagger", Locale.forLanguageTag("ru"))
      val json = swaggerBuilder
        .describe(descriptions)
        .make(OpenApiInfo("memologio", version = "0.0.1"))
        .asJson
        .printWith(printer)
      message.jsonResponse(json)
    }.flatMap(response => Routed.checkPath[MemologioHttp, Response]("/swagger", ZIO.succeed(response)))

  val route: MemologioHttp[Response] = swaggerResources <+> swaggerHttp <+> swaggerJson
}
