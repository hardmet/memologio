package ru.hardmet.memologio.http

import cats.instances.list._
import cats.syntax.foldable._
import cats.syntax.semigroupk._
import com.twitter.finagle.http.Response
import com.twitter.finagle.http.filter.Cors
import com.twitter.finagle.{Http, ListeningServer}
import ru.hardmet.memologio.config.Config
import ru.hardmet.memologio.resources.{OpenAPI, UI}
import ru.hardmet.memologio.{Memologio, MemologioEnv, MemologioHttp, SystemEnv}
import ru.tinkoff.tschema.finagle.zioRouting.HasRouting
import ru.tinkoff.tschema.finagle.{Routed, RunHttp}
import zio._
import zio.console.putStrLn

object Server {

  private val apiRoute: MemologioHttp[Response] =
    Routed
      .checkPrefix("/api", modules.foldMapK(_.route))
      .provideLayer(
        ZLayer.identity[MemologioEnv with HasRouting] +!+ RequestInfo.build
      )
  private val route = apiRoute <+> OpenAPI.route <+> UI.route

  private val cors = new Cors.HttpFilter(Cors.UnsafePermissivePolicy)

  private val start: Memologio[ListeningServer] = for {
    ServerConfig(host, port) <- ZIO.access[Config](_.get.server)
    srv <- RunHttp.run[Memologio](route.onError(e => putStrLn(e.prettyPrint)))
    full = cors.andThen(srv)
    binding <- ZIO.effect(Http.serve(s"$host:$port", full))
  } yield binding

  private def report(srv: ListeningServer) =
    s"started at http://${srv.boundAddress} see also memologio http:/${srv.boundAddress}/swagger.php for swagger-ui "

  /** Сервер как ресурс - когда процесс, использующий сервер будет прерван
   * сервер будет остановлен
   */
  private val managedServer: RManaged[MemologioEnv, ListeningServer] =
    start.toManaged(s => UIO.effectAsync[Any](cb => s.close().respond(_ => cb(UIO.unit))))

  val run: URIO[SystemEnv, Any] =
    managedServer.use { srv => putStrLn(report(srv)) *> ZIO.never }
      .provideLayer(Memologio.live)
      .catchAll(error => putStrLn(error.toString))
}
