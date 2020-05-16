package ru.hardmet.memologio.posts.services

import java.util.UUID

import ru.hardmet.memologio.Memologio
import ru.hardmet.memologio.posts.{Post, Posts}
import ru.hardmet.memologio.config.Config
import ru.hardmet.memologio.db.DB
import ru.hardmet.memologio.posts.{PostData, PostId}
import zio.{UIO, URLayer}

object PostService {

  trait Service {

    def getData: Memologio[Seq[PostData]]

    def getOne(name: String): Memologio[Post]

    def putOne(data: PostData): Memologio[PostId]

    def remove(name: String): Memologio[Unit]
  }

  val ref: URLayer[Config, Posts]         = RefService.live
  val db: URLayer[DB, Posts]              = DBService.live
  val logging: URLayer[Posts, Posts] = LoggedService.live

  def newId: UIO[PostId] = UIO.effectTotal(PostId(UUID.randomUUID()))
}
