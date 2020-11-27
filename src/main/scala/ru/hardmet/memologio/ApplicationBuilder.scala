package ru.hardmet.memologio

import java.util.UUID

import cats.effect.{ConcurrentEffect, ContextShift, Resource, Timer}
import natchez.Trace.Implicits.noop
import ru.hardmet.memologio.config.{AppConfig, ServerConfig}
import ru.hardmet.memologio.http.routes.Router
import ru.hardmet.memologio.http.server.HttpServer
import ru.hardmet.memologio.repository.{DBConnector, SkunkConnector}
import ru.hardmet.memologio.services.PostService

import scala.concurrent.ExecutionContext

trait ApplicationBuilder[F[_], PostId] {

  val executionContext: ExecutionContext

  val configReader: F[AppConfig]

  val dbConnector: DBConnector[F, PostId]

  def router(postService: PostService[F, PostId]): Router[F, PostId]

  def create: F[Unit]
}

class ApplicationBuilderBase[F[_] : ConcurrentEffect : ContextShift : Timer](val executionContext: ExecutionContext) extends ApplicationBuilder[F, UUID] {
  override val configReader: F[AppConfig] = AppConfig.init[F]
  override val dbConnector: DBConnector[F, UUID] = new SkunkConnector[F]()

  override def router(postService: PostService[F, UUID]): Router[F, UUID] = ???

  def httpServer(executionContext: ExecutionContext)
                (config: ServerConfig)
                (r: Router[F, UUID]): F[HttpServer[F]] =
    HttpServer.create(executionContext)(config)(r.routes: _*)

  override def create: F[Unit] =
    (for {
      config <- Resource.liftF(configReader)
      repository <- dbConnector.connectToRepository(config.db)
      postService = PostService(repository)
      r = router(postService)
      server <- Resource.liftF[F, HttpServer[F]](httpServer(executionContext)(config.server)(r))
    } yield server).use(server => server.serve)
}
