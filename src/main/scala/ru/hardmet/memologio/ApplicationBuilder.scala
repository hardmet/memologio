package ru.hardmet.memologio

import java.util.UUID

import cats.implicits._
import cats.effect.{ConcurrentEffect, ContextShift, Resource, Timer}
import config.{AppConfig, ServerConfig}
import infrastructure.http.HttpServer
import infrastructure.http.routes.{Router, RouterInterpreter}
import infrastructure.logging.ContextLogging
import infrastructure.repository.DBConnector
import infrastructure.repository.doobie.DoobieConnector
import natchez.Trace.Implicits.noop
import services.{PostParser, PostService, PostServiceInterpreter, PostValidator}

import scala.concurrent.ExecutionContext

trait ApplicationBuilder[F[_], PostId] {

  val executionContext: ExecutionContext

  val configReader: F[AppConfig]

  val dbConnector: DBConnector[F, PostId]

  def router(postService: PostService[F, PostId]): RouterInterpreter[F, PostId]

  def httpServer(executionContext: ExecutionContext)
                (config: ServerConfig)
                (routers: Seq[Router[F]]): F[HttpServer[F]]

  def create: F[Unit]
}

class ApplicationBuilderBase[F[_] : ConcurrentEffect : ContextShift : Timer](val executionContext: ExecutionContext) extends ApplicationBuilder[F, UUID] {
  override val configReader: F[AppConfig] = AppConfig.init[F]
  override val dbConnector: DBConnector[F, UUID] = new DoobieConnector[F]()

  override def router(postService: PostService[F, UUID]): RouterInterpreter[F, UUID] =
    new RouterInterpreter(postService)

  override def httpServer(executionContext: ExecutionContext)
                (config: ServerConfig)
                (routers: Seq[Router[F]]): F[HttpServer[F]] =
    HttpServer.create(executionContext)(config)(routers.map(r => r.routes):_*)

  override def create: F[Unit] =
    (for {
      config <- Resource.liftF(configReader)
      repository <- dbConnector.connectToRepository(config.db)
      logger <- Resource.liftF(new ContextLogging[F].loggerForService[PostService[F, UUID]])
      postService = PostServiceInterpreter(PostParser[F, UUID], PostValidator[F, UUID], repository)(logger)
      r = router(postService)
      server <- Resource.liftF[F, HttpServer[F]](httpServer(executionContext)(config.server)(Seq(r)))
    } yield server)
      .use(server => server.serve)
}
