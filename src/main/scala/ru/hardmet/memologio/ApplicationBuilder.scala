package ru.hardmet.memologio

import java.util.UUID

import cats.implicits._
import cats.effect.{ConcurrentEffect, ContextShift, Resource, Timer}
import natchez.Trace.Implicits.noop
import config.{AppConfig, ServerConfig}

import http.routes.Router
import http.routes.post.PostRouter
import http.server.HttpServer
import logging_infrastructure.ContextLogging
import repository.DBConnector
import repository.doobie.DoobieConnector
import services.{PostService, PostServiceImpl}

import scala.concurrent.ExecutionContext

trait ApplicationBuilder[F[_], PostId] {

  val executionContext: ExecutionContext

  val configReader: F[AppConfig]

  val dbConnector: DBConnector[F, PostId]

  def router(postService: PostService[F, PostId]): PostRouter[F, PostId]

  def httpServer(executionContext: ExecutionContext)
                (config: ServerConfig)
                (routers: Seq[Router[F]]): F[HttpServer[F]]

  def create: F[Unit]
}

class ApplicationBuilderBase[F[_] : ConcurrentEffect : ContextShift : Timer](val executionContext: ExecutionContext) extends ApplicationBuilder[F, UUID] {
  override val configReader: F[AppConfig] = AppConfig.init[F]
  override val dbConnector: DBConnector[F, UUID] = new DoobieConnector[F]()

  override def router(postService: PostService[F, UUID]): PostRouter[F, UUID] = new PostRouter(postService)

  override def httpServer(executionContext: ExecutionContext)
                (config: ServerConfig)
                (routers: Seq[Router[F]]): F[HttpServer[F]] =
    HttpServer.create(executionContext)(config)(routers.map(r => r.routes):_*)

  override def create: F[Unit] =
    (for {
      config <- Resource.liftF(configReader)
      repository <- dbConnector.connectToRepository(config.db)
      logger <- Resource.liftF(new ContextLogging[F].loggerForService[PostService[F, UUID]])
      postService = PostServiceImpl.apply(repository)(logger)
      r = router(postService)
      server <- Resource.liftF[F, HttpServer[F]](httpServer(executionContext)(config.server)(Seq(r)))
    } yield server)
      .use(server => server.serve)
}
