package ru.hardmet.memologio
package infrastructure
package repository

import services.post.domain.{Data, Existing, PostId}

import java.time.{LocalDate, LocalDateTime}

trait PostRepository[F[_]] {

  def create(post: Data): F[Existing]

  def update(post: Existing): F[Existing]

  def get(id: PostId): F[Option[Existing]]

  def getListByIds(ids: Vector[PostId]): F[Vector[Existing]]

  def findByPublishedDate(published: LocalDate): F[Vector[Existing]]

  def findByPublishedDateTime(published: LocalDateTime): F[Vector[Existing]]

  def findWithLikesAbove(likes: Int): F[Vector[Existing]]

  def findWithLikesBelow(likes: Int): F[Vector[Existing]]

  def list(pageSize: Int, offset: Int): F[Vector[Existing]]

  def listAll(): F[Vector[Existing]]

  def delete(id: PostId): F[Unit]

  def deleteMany(posts: Vector[Existing]): F[Unit]

  def deleteAll(): F[Unit]
}
