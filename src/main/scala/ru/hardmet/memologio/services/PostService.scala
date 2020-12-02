package ru.hardmet.memologio
package services

import java.time.{LocalDate, LocalDateTime}

import ru.hardmet.memologio.domain.posts.Post

trait PostService[F[_], PostId] extends Service[F] {

  def createOne(post: Post.Data): F[Post.Existing[PostId]]
  def createMany(posts: Vector[Post.Data]): F[Vector[Post.Existing[PostId]]]

  def readOneById(id: PostId): F[Option[Post.Existing[PostId]]]
  // TODO write route pagination
  // TODO write route for queries with likes param: findWithLikesAbove
  // TODO write route for it
  def readManyByIds(ids: Vector[PostId]): F[Vector[Post.Existing[PostId]]]
  def readManyByPublishedDate(published: LocalDate): F[Vector[Post.Existing[PostId]]]
  def readManyByPublishedDateTime(published: LocalDateTime): F[Vector[Post.Existing[PostId]]]
  def readAll: F[Vector[Post.Existing[PostId]]]

  def updateOne(post: Post.Existing[PostId]): F[Post.Existing[PostId]]
  def updateMany(posts: Vector[Post.Existing[PostId]]): F[Vector[Post.Existing[PostId]]]

  def deleteOne(post: Post.Existing[PostId]): F[Unit]
  def deleteMany(posts: Vector[Post.Existing[PostId]]): F[Unit]
  def deleteAll(): F[Unit]
}
