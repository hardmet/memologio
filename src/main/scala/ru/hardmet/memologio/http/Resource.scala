package ru.hardmet.memologio.http

import com.twitter.finagle.http.Response
import ru.hardmet.memologio.MemologioHttp
import ru.tinkoff.tschema.finagle.zioRouting.Fail.Rejected
import ru.tinkoff.tschema.finagle.{Rejection, Routed}
import tofu.syntax.foption._
import zio.blocking.blocking
import zio.{Ref, ULayer, ZIO, ZLayer}

class Resource {
  def apply(name: String, ct: String = ""): MemologioHttp[Response] =
    blocking(ZIO {
      val BufSize  = 1024
      val response = Response()
      response.contentType = ct
      val stream = getClass.getResourceAsStream(name)
      val arr    = Array.ofDim[Byte](BufSize)
      @scala.annotation.tailrec
      def readAll(): Unit =
        stream.read(arr) match {
          case BufSize =>
            response.write(arr)
            readAll()
          case size if size > 0 =>
            response.write(arr.slice(0, size))
            readAll()
          case _ =>
        }
      readAll()
      response
    }).catchAll(_ => ZIO.fail(Rejected(Rejection.notFound)))

  private def serveCheck(
      check: String => Boolean,
      mod: String => String
  )(ct: String = ""): MemologioHttp[Response] =
    Routed.path[MemologioHttp].map(_.toString).flatMap {
      case s if check(s) => apply(mod(s), ct)
      case _             => Routed.reject[MemologioHttp, Response](Rejection.notFound)
    }

  def folder(path: String, prefix: String = "", ct: String = ""): MemologioHttp[Response] =
    serveCheck(_.startsWith(path), prefix + _)(ct)

  def single(path: String, realName: String = "", ct: String = ""): MemologioHttp[Response] =
    serveCheck(_ == path, if (realName.isEmpty) identity else _ => realName)(ct)
}

class CachedResource extends Resource {
  override def apply(name: String, ct: String): MemologioHttp[Response] =
    ResourceCache
      .get(name)
      .getOrElseF(
        super.apply(name, ct).tap(ResourceCache.put(name, _))
      )
}

object Resource extends CachedResource

object ResourceCache {
  case class Service(cached: Ref[Map[String, Response]])

  val live: ULayer[ResourceCache] = ZLayer.fromEffect(Ref.make(Map[String, Response]()).map(Service))

  def get(name: String): MemologioHttp[Option[Response]] =
    ZIO.accessM[ResourceCache](_.get.cached.get.map(_.get(name)))

  def put(name: String, response: Response): MemologioHttp[Any] =
    ZIO.accessM[ResourceCache](_.get.cached.update(_.updated(name, response)))
}
