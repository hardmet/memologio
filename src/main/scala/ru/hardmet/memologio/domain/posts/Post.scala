package ru.hardmet.memologio
package domain
package posts

import java.time.LocalDateTime
import java.util.UUID

sealed abstract class Post[+PostId] extends Product with Serializable {
  protected type ThisType <: Post[PostId]

  def url: String

  def withUpdatedURL(newURL: String): ThisType

  def published: LocalDateTime

  def withUpdatedPublished(newPublished: LocalDateTime): ThisType

  def likes: Int

  def withUpdatedLikes(newLikes: Int): ThisType
}

object Post {

  final case class Existing[PostId](id: PostId, data: Data) extends Post[PostId] {
    override protected type ThisType = Existing[PostId]

    override def url: String = data.url

    override def withUpdatedURL(newURL: String): ThisType = copy(data = data.withUpdatedURL(newURL))

    override def published: LocalDateTime = data.published

    override def withUpdatedPublished(newPublished: LocalDateTime): ThisType =
      copy(data = data.withUpdatedPublished(newPublished))

    override def likes: Int = data.likes

    override def withUpdatedLikes(newLikes: Int): ThisType = copy(data = data.withUpdatedLikes(newLikes))
  }

  final case class Data(url: String, published: LocalDateTime, likes: Int = 0) extends Post[Nothing] {
    override protected type ThisType = Data

    override def withUpdatedURL(newURL: String): ThisType = copy(url = newURL)

    override def withUpdatedPublished(newPublished: LocalDateTime): ThisType = copy(published = newPublished)

    override def withUpdatedLikes(newLikes: Int): ThisType = copy(likes = newLikes)
  }
}

final case class PostId(uuid: UUID)
