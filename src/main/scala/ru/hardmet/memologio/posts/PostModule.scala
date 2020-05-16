package ru.hardmet.memologio
package posts

import com.twitter.finagle.http.Response
import ru.hardmet.memologio.posts.Posts
import ru.tinkoff.tschema.custom.syntax._
import ru.tinkoff.tschema.finagle.MkService
import ru.tinkoff.tschema.swagger.{MkSwagger, SwaggerBuilder}
import ru.tinkoff.tschema.syntax._
import zio.{RIO, URIO, ZIO}

object PostModule extends HttpModule {

  def getAll = opGet |> json[Seq[PostData]]
  def addOne = opPost |> jsonBody[PostData]("data") |> plainErr[AlreadyExists, Unit]
  def error  = operation("error") |> post |> plain[String]

  def api = tagPrefix("Posts") |> (getAll <> addOne <> error)

  object handler {
    def get = RIO.access[Posts](_.get).flatMap(_.getData)
    def post(data: PostData) =
      RIO.access[Posts](_.get).flatMap(_.putOne(data).unit.refineToOrDie[AlreadyExists])
    def error: Memologio[String] = URIO(throw new RuntimeException)
  }

  def route: MemologioHttp[Response] = MkService[MemologioHttp](api)(handler)

  def swag: SwaggerBuilder = MkSwagger(api)
}
