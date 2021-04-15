package ru.hardmet.memologio
package services
package post

import cats.Show
import cats.syntax.either._
import derevo.derive
import doobie.{Get, Put}
import io.estatico.newtype.macros.newtype
import skunk.Codec
import skunk.data._
import tofu.logging.Loggable
import tofu.logging.derivation.loggable
import doobie.postgres.implicits._

import java.time.LocalDateTime
import java.util.UUID


object domain {

  @newtype final case class PostId(value: UUID)

  object PostId {
    implicit val show: Show[PostId] = _.value.toString
    implicit val loggable: Loggable[PostId] = Loggable.show

    implicit val get: Get[PostId] = deriving
    implicit val put: Put[PostId] = deriving

    val postId: Codec[PostId] =
      Codec.simple[PostId](
        u => u.value.toString,
        s => Either.catchOnly[IllegalArgumentException](PostId(UUID.fromString(s))).leftMap(_.getMessage),
        Type.uuid
      )
  }

  @newtype final case class Url(value: String)

  object Url {
    implicit val show: Show[Url] = _.value
    implicit val loggable: Loggable[Url] = Loggable.show

    implicit val get: Get[Url] = deriving
    implicit val put: Put[Url] = deriving

    val url: Codec[Url] =
      Codec.simple[Url](
        u => u.value,
        s => Either.catchOnly[IllegalArgumentException](Url(s)).leftMap(_.getMessage),
        Type.varchar(512)
      )
  }


  sealed abstract class Post extends Product with Serializable {
    protected type ThisType <: Post

    def url: Url

    def withUpdatedURL(newURL: Url): ThisType

    def published: LocalDateTime

    def withUpdatedPublished(newPublished: LocalDateTime): ThisType

    def likes: Int

    def withUpdatedLikes(newLikes: Int): ThisType
  }


  @derive(loggable)
  final case class Existing(id: PostId, data: Data) extends Post {
    override protected type ThisType = Existing

    override def url: Url = data.url

    override def withUpdatedURL(newURL: Url): ThisType = copy(data = data.withUpdatedURL(newURL))

    override def published: LocalDateTime = data.published

    override def withUpdatedPublished(newPublished: LocalDateTime): ThisType =
      copy(data = data.withUpdatedPublished(newPublished))

    override def likes: Int = data.likes

    override def withUpdatedLikes(newLikes: Int): ThisType = copy(data = data.withUpdatedLikes(newLikes))
  }

  @derive(loggable)
  final case class Data(url: Url, published: LocalDateTime, likes: Int = 0) extends Post {
    override protected type ThisType = Data

    override def withUpdatedURL(newURL: Url): ThisType = copy(url = newURL)

    override def withUpdatedPublished(newPublished: LocalDateTime): ThisType = copy(published = newPublished)

    override def withUpdatedLikes(newLikes: Int): ThisType = copy(likes = newLikes)
  }
}
