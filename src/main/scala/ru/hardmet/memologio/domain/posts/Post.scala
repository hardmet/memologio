package ru.hardmet.memologio
package domain
package posts

import java.time.LocalDateTime
import java.util.UUID

//import derevo.circe.codec
//import derevo.derive
//import ru.tinkoff.tschema.param.HttpParam
//import ru.tinkoff.tschema.swagger.{AsOpenApiParam, Swagger}
//import tofu.logging.derivation.loggable


sealed abstract class Post[+PostId] extends Product with Serializable {
  protected type ThisType <: Post[PostId]

//  import Post._
//  todo use or remove
//  final def fold[B](ifExisting: (PostId, Data) => B, ifData: (String, LocalDateTime, Int) => B): B =
//    this match {
//      case Existing(id, data)          => ifExisting(id, data)
//      case Data(url, published, likes) => ifData(url, published, likes)
//    }

  def url: String

  def withUpdatedURL(newURL: String): ThisType

  def published: LocalDateTime

  def withUpdatedPublished(newPublished: LocalDateTime): ThisType

  def likes: Int

  def withUpdatedLikes(newLikes: Int): ThisType
}

object Post {

//  @derive(codec, Swagger, loggable) TODO fix deriving
  final case class Existing[PostId](id: PostId, data: Data)
    extends Post[PostId] {
    override protected type ThisType = Existing[PostId]

    override def url: String = data.url

    override def withUpdatedURL(newURL: String): ThisType = copy(data = data.withUpdatedURL(newURL))

    override def published: LocalDateTime = data.published

    override def withUpdatedPublished(newPublished: LocalDateTime): ThisType =
      copy(data = data.withUpdatedPublished(newPublished))

    override def likes: Int = data.likes

    override def withUpdatedLikes(newLikes: Int): ThisType =
      copy(data = data.withUpdatedLikes(newLikes))
  }

//  @derive(codec, Swagger, config, loggable) TODO fix deriving
  final case class Data(url: String, published: LocalDateTime, likes: Int = 0) extends Post[Nothing] {
    override protected type ThisType = Data

    override def withUpdatedURL(newURL: String): ThisType = copy(url = newURL)

    override def withUpdatedPublished(newPublished: LocalDateTime): ThisType = copy(published = newPublished)

    override def withUpdatedLikes(newLikes: Int): ThisType = copy(likes = newLikes)
  }
}

//@derive(codec, Swagger, loggable, HttpParam, AsOpenApiParam)
final case class PostId(uuid: UUID)
