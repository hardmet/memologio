package ru.hardmet.memologio
package repository

import java.time.{LocalDate, LocalDateTime}

import ru.hardmet.memologio.domain.posts.Post

trait PostRepository[F[_], PostId] {

  def create(post: Post.Data): F[Post.Existing[PostId]]

  def update(post: Post.Existing[PostId]): F[Post.Existing[PostId]]

  def get(id: PostId): F[Option[Post.Existing[PostId]]]

  def getListByIds(ids: Vector[PostId]): F[Vector[Post.Existing[PostId]]]

  def findByPublishedDate(published: LocalDate): F[Vector[Post.Existing[PostId]]]

  def findByPublishedDateTime(published: LocalDateTime): F[Vector[Post.Existing[PostId]]]

  def findWithLikesAbove(likes: Int): F[Vector[Post.Existing[PostId]]]

  def findWithLikesBelow(likes: Int): F[Vector[Post.Existing[PostId]]]

  def list(pageSize: Int, offset: Int): F[Vector[Post.Existing[PostId]]]

  def listAll(): F[Vector[Post.Existing[PostId]]]

  def delete(id: PostId): F[Unit]
  def deleteMany(posts: Vector[Post.Existing[PostId]]): F[Unit]
  def deleteAll(): F[Unit]
}
