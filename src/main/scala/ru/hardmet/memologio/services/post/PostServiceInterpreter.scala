package ru.hardmet.memologio
package services
package post

import cats.Monad
import cats.data.{EitherNec, NonEmptyChain}
import cats.instances.vector._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.show._
import cats.syntax.traverse._
import infrastructure.repository.PostRepository
import services.post.domain.{Data, Existing, Post, PostId}
import tofu.logging.Logging
import tofu.syntax.logging._

import scala.util.chaining.scalaUtilChainingOps

class PostServiceInterpreter[F[_] : Monad : Logging](parser: PostParser[F],
                                                     validator: PostValidator[F],
                                                     repository: PostRepository[F])
  extends PostService[F] {

  override def createOne(url: String, published: String, likes: Int): F[EitherNec[String, Existing]] =
    for {
      _ <- debug"creating post url: $url, published: $published, likes: $likes"
      errorsChainOrPostData <- parseAndValidatePost(url, published, likes)
      post <- errorsChainOrPostData.traverse(repository.create)
      _ <- post.fold(
        errors => debug"post has not created, reasons: ${errors.show}",
        p => debug"post ${p.show} created"
      )
    } yield post

  override def createMany(posts: Vector[(String, String, Int)]): F[Vector[EitherNec[String, Existing]]] =
    for {
      _ <- debug"createMany posts:\n${getRawPostsLine(posts)}"
      vectorErrorsNecOrPostsData <- posts.traverse(Function.tupled(parseAndValidatePost))
      (errors, validPosts) = vectorErrorsNecOrPostsData.partition(_.isLeft)
      createdPosts <- validPosts.collect { case Right(post) => post }.pipe(writeMany)
      _ <- debug"posts created:\n${createdPosts.mkString(",\n")}"
      liftedPosts = createdPosts.map(Right[NonEmptyChain[String], Existing])
      _ <- debug"posts has not created reasons:\n${necStringToString(errors)}"
      liftedErrors = errors.collect {
        case Left(errorsNec) => Left[NonEmptyChain[String], Existing](errorsNec)
      }
    } yield liftedPosts ++ liftedErrors

  private def getRawPostsLine(posts: Iterable[(String, String, Int)]): String =
    posts.map {
      case (url, published, likes) => s"url: $url, published: $published, likes: $likes"
    }.mkString(",\n")

  private def parseAndValidatePost(url: String, published: String, likes: Int): F[EitherNec[String, Data]] =
    for {
      parsedPublished <- parser.parsePublished(published)
      postData <- validator.validatePostWithUnreliablyPublished(url, likes)(parsedPublished)
    } yield postData

  private def necStringToString[T](eitherNECs: Vector[EitherNec[String, T]]): String =
    eitherNECs.collect {
      case Left(errors) => errors.show.mkString(", ")
    }.mkString(",\n")

  private def writeMany[T <: Post](posts: Vector[T]): F[Vector[Existing]] =
    posts.traverse {
      case data: Data => repository.create(data)
      case post: Existing => repository.update(post)
    }

  override def readOneById(id: String): F[Either[String, Existing]] =
    for {
      _ <- debug"read post by id $id"
      parsedId <- parser.parseId(id)
      parsingErrorOrOptionPost <- parsedId.traverse(repository.get)
      post = parsingErrorOrOptionPost.flatMap(post =>
        post.toRight(s"Post with id ${parsedId.map(_.toString).getOrElse("can't be parsed")} does not exist")
      )
      _ <- post.fold(
        error => debug"post not found, reason: $error",
        p => debug"found post $p"
      )
    } yield post

  override def readManyByIds(ids: Vector[String]): F[Vector[Either[String, Existing]]] =
    for {
      _ <- debug"read many posts by ids: ${ids.mkString(", ")}"
      vectorErrorNecOrId <- ids.traverse(parser.parseId)
      (errors, validIds) = vectorErrorNecOrId.partition(_.isLeft)
      posts <- validIds.collect { case Right(post) => post }.pipe(repository.getListByIds)
      _ <- debug"found posts:\n${posts.mkString(",\n")}"
      _ <- logFoundErrorsOrPosts(ids, posts.map(_.id.toString).toSet, errors)
      liftedPosts = posts.map(Right[String, Existing])
      liftedErrors = errors.collect {
        case Left(errorsNec) => Left[String, Existing](errorsNec)
      }
    } yield liftedPosts ++ liftedErrors

  // TODO refactoring, get rid of Either[LocalDate, LocalDateTime]
  override def readManyByPublished(published: String): F[Either[String, Vector[Existing]]] =
    for {
      _ <- debug"read post by published date: $published"
      parsedPublished <- parser.parsePublishedDateOrDateTime(published)
      validatedPublished <- parsedPublished.flatTraverse(validator.validatePublishedDateOrDateTime)
      posts <- validatedPublished.traverse(
        dateOrDateTime =>
          dateOrDateTime.fold(
            repository.findByPublishedDate,
            repository.findByPublishedDateTime
          )
      )
      _ <- posts.fold(
        error => debug"posts not found, reason: $error",
        ps => debug"found posts:\n${ps.mkString(",\n")}"
      )
    } yield posts

  override def readAll: F[Vector[Existing]] =
    for {
      _ <- debug"read all posts"
      posts <- repository.listAll()
      _ <- debug"posts: ${posts.map(_.data)} found"
    } yield posts

  override def updateOneAllFields(id: String)
                                 (url: String, published: String, likes: Int): F[EitherNec[String, Existing]] =
    for {
      _ <- debug"update url $url fot post id: $id"
      post <- readOneById(id)
      errorsChainOrPostData <- parseAndValidatePost(url, published, likes)
      updated <- errorsChainOrPostData.flatTraverse { validPost =>
        post.map(p =>
          p.withUpdatedURL(validPost.url)
            .withUpdatedPublished(validPost.published)
            .withUpdatedLikes(validPost.likes)).toEitherNec
          .traverse(repository.update)
      }
      _ <- updated.fold(
        error => debug"post url update failed, reason: ${error.show.mkString(", ")}",
        p => debug"post updated $p"
      )
    } yield updated

  override def updateURL(id: String)(url: String): F[Either[String, Existing]] =
    for {
      _ <- debug"update url $url fot post id: $id"
      post <- readOneById(id)
      errorOrValidatedURL <- validator.validateURL(url)
      updated <- errorOrValidatedURL.flatTraverse { validURL =>
        post.map(p => p.withUpdatedURL(validURL))
          .traverse(repository.update)
      }
      _ <- updated.fold(
        error => debug"post url update failed, reason: $error",
        p => debug"post updated $p"
      )
    } yield updated

  override def updatePublished(id: String)(published: String): F[Either[String, Existing]] =
    for {
      _ <- debug"update published $published fot post id: $id"
      post <- readOneById(id)
      errorOrParsedPublished <- parser.parsePublished(published)
      errorOrValidatedPublished <- errorOrParsedPublished.flatTraverse(validator.validatePublished)
      updated <- errorOrValidatedPublished.flatTraverse { validPublished =>
        post.map(p => p.withUpdatedPublished(validPublished))
          .traverse(repository.update)
      }
      _ <- updated.fold(
        error => debug"post published update failed, reason: $error",
        p => debug"post updated $p"
      )
    } yield updated

  override def updateLikes(id: String)(likes: Int): F[Either[String, Existing]] =
    for {
      _ <- debug"update url $likes fot post id: $id"
      post <- readOneById(id)
      errorOrValidatedLikes <- validator.validateLikes(likes)
      updated <- errorOrValidatedLikes.flatTraverse { validLikes =>
        post.map(p => p.withUpdatedLikes(validLikes))
          .traverse(repository.update)
      }
      _ <- updated.fold(
        error => debug"post likes update failed, reason: $error",
        p => debug"post updated $p"
      )
    } yield updated

  // TODO implement and use updateMany in repository
  override def updateMany(posts: Vector[Existing]): F[Vector[EitherNec[String, Existing]]] =
    for {
      vectorErrorNecOrValidPosts <- posts.traverse(post => validator.validatePost(post.data))
      errorNecOrVectorValidPosts = vectorErrorNecOrValidPosts.sequence
      errorNecOrVectorWrittenPosts <- errorNecOrVectorValidPosts.traverse(writeMany)
    } yield errorNecOrVectorWrittenPosts.sequence

  override def deleteOne(id: String): F[Either[String, Unit]] =
    for {
      _ <- debug"delete post with id $id"
      errorOrId <- parser.parseId(id)
      errorOrDeletedUnit <- errorOrId.traverse(repository.delete)
      _ <- errorOrDeletedUnit.fold(
        error => debug"post $id has not been deleted, reason: $error",
        _ => debug"post $id deleted"
      )
    } yield errorOrDeletedUnit

  override def safeDeleteMany(ids: Vector[String]): F[Vector[Either[String, Unit]]] =
    for {
      _ <- warn"delete posts by ids: ${ids.mkString(",")}"
      vectorErrorOrPosts <- readManyByIds(ids)
      (errors, posts) = vectorErrorOrPosts.partition(_.isLeft)
      flattenPosts = posts.collect { case Right(post) => post }
      _ <- repository.deleteMany(flattenPosts)
      _ <- debug"posts deleted:\n${flattenPosts.mkString(",\n")}"
      _ <- debug"posts has not deleted reasons:\n${necStringToString(errors.map(_.toEitherNec))}"
      liftedErrors = errors.collect { case Left(errors) => Left[String, Unit](errors) }
      liftedPosts = flattenPosts.map(_ => Right[String, Unit](()))
    } yield liftedPosts ++ liftedErrors

  override def deleteAll(): F[Unit] =
    for {
      _ <- warn"delete all posts"
      res <- repository.deleteAll()
      _ <- warn"all posts deleted"
    } yield res


  private def logFoundErrorsOrPosts(found: Vector[String],
                                    keys: Set[String],
                                    errors: Vector[Either[String, PostId]]): F[Unit] =
    debug"""for keys ${found.filter(fk => !keys.contains(fk))} posts not found, errors:
           ${errors.collect { case Left(e) => e }.mkString(",\n")}
           """
}

object PostServiceInterpreter {
  def apply[F[_] : Monad](parser: PostParser[F],
                          validator: PostValidator[F],
                          postRepository: PostRepository[F])
                         (log: Logging[F]): PostServiceInterpreter[F] = {
    implicit val logging: Logging[F] = log
    new PostServiceInterpreter(parser, validator, postRepository)
  }
}
