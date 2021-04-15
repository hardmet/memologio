package ru.hardmet.memologio
package services

import cats.data.EitherNec
import post.domain.{Existing, Url}

trait PostService[F[_]] extends Service[F] {

  def createOne(url: String, published: String, likes: Int): F[EitherNec[String, Existing]]

  def createMany(posts: Vector[(String, String, Int)]): F[Vector[EitherNec[String, Existing]]]

  def readOneById(id: String): F[Either[String, Existing]]

  def readManyByIds(ids: Vector[String]): F[Vector[Either[String, Existing]]]

  def readManyByPublished(published: String): F[Either[String, Vector[Existing]]]

  def readAll: F[Vector[Existing]]

  def updateOneAllFields(id: String)
                        (url: String, published: String, likes: Int): F[EitherNec[String, Existing]]

  def updateURL(id: String)(url: String): F[Either[String, Existing]]

  def updatePublished(id: String)(published: String): F[Either[String, Existing]]

  def updateLikes(id: String)(likes: Int): F[Either[String, Existing]]

  def updateMany(posts: Vector[Existing]): F[Vector[EitherNec[String, Existing]]]

  def deleteOne(id: String): F[Either[String, Unit]]

  def safeDeleteMany(ids: Vector[String]): F[Vector[Either[String, Unit]]]

  def deleteAll(): F[Unit]
}
