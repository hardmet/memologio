package ru.hardmet.memologio
package services

import java.time.LocalDate

import ru.hardmet.memologio.domain.posts.Post

trait PostService[F[_], PostId] extends Service[F] {

  def createOne(post: Post.Data): F[Post.Existing[PostId]]
  def createMany(posts: Vector[Post.Data]): F[Vector[Post.Existing[PostId]]]

  def readOneById(id: PostId): F[Option[Post.Existing[PostId]]]
  def readManyById(ids: Vector[PostId]): F[Vector[Post.Existing[PostId]]]
  def readManyByPublishedDate(publishedDate: LocalDate): F[Vector[Post.Existing[PostId]]]
  def readAll: F[Vector[Post.Existing[PostId]]]

  def updateOne(post: Post.Existing[PostId]): F[Post.Existing[PostId]]
  def updateMany(posts: Vector[Post.Existing[PostId]]): F[Vector[Post.Existing[PostId]]]

  def deleteOne(post: Post.Existing[PostId]): F[Unit]
  def deleteMany(posts: Vector[Post.Existing[PostId]]): F[Unit]
  def deleteAll(): F[Unit]
}
