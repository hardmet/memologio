package ru.hardmet.memologio
package services

import cats.data.EitherNec
import domain.posts.Post

trait PostService[F[_], PostId] extends Service[F] {

  def createOne(url: String, published: String, likes: Int): F[EitherNec[String, Post.Existing[PostId]]]
  def createMany(posts: Vector[(String, String, Int)]): F[Vector[EitherNec[String, Post.Existing[PostId]]]]

  def readOneById(id: String): F[Either[String, Post.Existing[PostId]]]
  def readManyByIds(ids: Vector[String]): F[Vector[Either[String, Post.Existing[PostId]]]]
  def readManyByPublished(published: String): F[Either[String, Vector[Post.Existing[PostId]]]]
  def readAll: F[Vector[Post.Existing[PostId]]]

  def updateOneAllFields(id: String)
                        (url: String, published: String, likes: Int): F[EitherNec[String, Post.Existing[PostId]]]
  def updateURL(id: String)(url: String): F[Either[String, Post.Existing[PostId]]]
  def updatePublished(id: String)(published: String): F[Either[String, Post.Existing[PostId]]]
  def updateLikes(id: String)(likes: Int): F[Either[String, Post.Existing[PostId]]]

  def updateMany(posts: Vector[Post.Existing[PostId]]): F[Vector[EitherNec[String, Post.Existing[PostId]]]]

  def deleteOne(id: String): F[Either[String, Unit]]
  def safeDeleteMany(ids: Vector[String]): F[Vector[Either[String, Unit]]]
  def deleteAll(): F[Unit]
}
