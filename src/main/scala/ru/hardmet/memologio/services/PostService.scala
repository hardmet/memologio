package ru.hardmet.memologio
package services

import cats.data.{EitherNec, EitherT, NonEmptyChain}
import domain.posts.Post
import domain.WrongDataForPostError

trait PostService[F[_], PostId] extends Service[F] {

  def createOne(url: String, published: String, likes: Int): F[EitherNec[String, Post.Existing[PostId]]]
  def createMany(posts: Vector[Post.Data]): F[EitherNec[Vector[String], Vector[Post.Existing[PostId]]]]

  def readOneById(id: String): F[Either[String, Post.Existing[PostId]]]
  def readManyByIds(ids: Vector[String]): F[EitherNec[String, Vector[Post.Existing[PostId]]]]
  def readManyByPublishedDate(published: String): F[Either[String, Vector[Post.Existing[PostId]]]]
  def readManyByPublishedDateTime(published: String): F[Either[String, Vector[Post.Existing[PostId]]]]
  def readAll: F[Vector[Post.Existing[PostId]]]

  def updateOne(id: String)
               (url: String, published: String, likes: Int): F[EitherNec[String, Post.Existing[PostId]]]
  def updateMany(posts: Vector[Post.Existing[PostId]]): F[EitherNec[Vector[String], Post.Existing[PostId]]]

  def deleteOne(id: String): F[Either[String, Unit]]
  def deleteMany(ids: Vector[String]): F[EitherNec[String, Unit]]
  def deleteAll(): F[Unit]
}
