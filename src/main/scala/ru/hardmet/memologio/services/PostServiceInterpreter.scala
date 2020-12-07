package ru.hardmet.memologio
package services

import cats.Monad
import cats.data.EitherNec
import cats.implicits._
import domain.posts.Post
import infrastructure.repository.PostRepository
import tofu.logging.{Loggable, Logging}
import tofu.syntax.logging.LoggingInterpolator

class PostServiceInterpreter[F[_] : Monad : Logging, PostId](parser: PostParser[F, PostId],
                                                             validator: PostValidator[F, PostId],
                                                             repository: PostRepository[F, PostId])
                                                            (implicit idLoggable: Loggable[PostId])
  extends PostService[F, PostId] {

  override def createOne(url: String, published: String, likes: Int): F[EitherNec[String, Post.Existing[PostId]]] =
    for {
      _ <- debug"creating post url: $url, published: $published, likes: $likes"
      errorsChainOrPostData <- parseAndValidatePost(url: String, published: String, likes: Int)
      post <- errorsChainOrPostData.traverse(repository.create)
      _ <- post.fold(
        errors => debug"post has not created, reasons: ${errors.show}",
        p => debug"post $p created"
      )
    } yield post

  // TODO remove
  //  override def createMany(posts: Vector[(String, String, Int)]): F[Vector[EitherNec[String, Post.Existing[PostId]]]] =
  //    for {
  //      _ <- debug"createMany posts ${getRawPostsLine(posts)}"
  //      postsCreationResult <- posts.traverse(Function.tupled(createOne))
  //      _ = for {
  //        postCreationResult <- postsCreationResult
  //        _ = postCreationResult.fold(
  //          errors => debug"post has not created, reasons:\n${errors.show.mkString("\n")}",
  //          ps => debug"posts: $ps created"
  //        )
  //      } yield ()
  //    } yield postsCreationResult

  override def createMany(posts: Vector[(String, String, Int)]): F[Vector[EitherNec[String, Post.Existing[PostId]]]] =
    for {
      _ <- debug"createMany posts:\n${getRawPostsLine(posts)}"
      vectorErrorsNecOrPostsData <- posts.traverse(Function.tupled(parseAndValidatePost))
      errorsNecOrVectorPostData = vectorErrorsNecOrPostsData.sequence
      createdPosts <- errorsNecOrVectorPostData.traverse(writeMany)
      _ <- debug"posts has not created reasons:\n${necStringToString(vectorErrorsNecOrPostsData)}"
    } yield createdPosts.sequence

  private def getRawPostsLine(posts: Iterable[(String, String, Int)]): String =
    posts.map {
      case (url, published, likes) => s"url: $url, published: $published, likes: $likes"
    }.mkString(",\n")

  private def parseAndValidatePost(url: String, published: String, likes: Int): F[EitherNec[String, Post.Data]] =
    for {
      parsedPublished <- parser.parsePublished(published)
      postData <- parsedPublished.toEitherNec.flatTraverse(published =>
        validator.validatePost(Post.Data(url, published, likes))
      )
    } yield postData

  private def necStringToString[T](eitherNECs: Vector[EitherNec[String, T]]): String =
    eitherNECs.collect {
      case Left(errors) => errors.show.mkString(", ")
    }.mkString(",\n")

  private def writeMany[T <: Post[PostId]](posts: Vector[T]): F[Vector[Post.Existing[PostId]]] =
    posts.traverse {
      case data: Post.Data => repository.create(data)
      case post: Post.Existing[PostId] => repository.update(post)
    }

  override def readOneById(id: String): F[Either[String, Post.Existing[PostId]]] =
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

  override def readManyByIds(ids: Vector[String]): F[Either[String, Vector[Post.Existing[PostId]]]] =
    for {
      _ <- debug"read many posts by ids: ${ids.mkString(", ")}"
      vectorErrorNecOrId <- ids.traverse(parser.parseId)
      necOrVectorIds = vectorErrorNecOrId.sequence
      posts <- necOrVectorIds.traverse(repository.getListByIds)
      _ <- logFoundErrorsOrPosts(posts)
    } yield posts

  override def readManyByPublished(published: String): F[Either[String, Vector[Post.Existing[PostId]]]] =
    for {
      _ <- debug"read post by published date $published"
      parsedPublished <- parser.parsePublishedDateOrDateTime(published)
      validatedPublished <- parsedPublished.flatTraverse(validator.validatePublishedDateOrDateTime)
      posts <- validatedPublished.traverse(
        dateOrDateTime =>
          dateOrDateTime.fold(
            repository.findByPublishedDate,
            repository.findByPublishedDateTime
          )
      )
      _ <- logFoundErrorsOrPosts(posts)
    } yield posts

  override def readAll: F[Vector[Post.Existing[PostId]]] =
    for {
      _ <- debug"read all posts"
      posts <- repository.listAll()
      _ <- debug"posts: ${posts.map(_.data)} found"
    } yield posts

  override def updateOneAllFields(id: String)
                                 (url: String, published: String, likes: Int): F[EitherNec[String, Post.Existing[PostId]]] =
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

  override def updateURL(id: String)(url: String): F[Either[String, Post.Existing[PostId]]] =
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

  override def updatePublished(id: String)(published: String): F[Either[String, Post.Existing[PostId]]] =
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

  override def updateLikes(id: String)(likes: Int): F[Either[String, Post.Existing[PostId]]] =
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
  override def updateMany(posts: Vector[Post.Existing[PostId]]): F[Vector[EitherNec[String, Post.Existing[PostId]]]] =
    for {
      vectorErrorNecOrValidPosts <- posts.traverse(post => validator.validatePost(post.data))
      errorNecOrVectorValidPosts = vectorErrorNecOrValidPosts.sequence
      errorNecOrVectorWrittenPosts <- errorNecOrVectorValidPosts.traverse(writeMany)
    } yield errorNecOrVectorWrittenPosts.sequence

  // TODO remove
  //  posts.traverse(post =>
  //      updateOneAllFields(post.id.toString)(
  //        post.url,
  //        post.published.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
  //        post.likes)
  //    )

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

  override def safeDeleteMany(ids: Vector[String]): F[Either[String, Unit]] =
    for {
      _ <- warn"delete posts by ids: ${ids.mkString(",")}"
      vectorErrorOrPosts <- readManyByIds(ids)
      result <- vectorErrorOrPosts.traverse(repository.deleteMany)
      _ <- result.fold(
        error => warn"posts has not deleted reasons:\n$error",
        _ => warn"posts has been deleted: ${ids.mkString(", ")}"
      )
    } yield result

  override def deleteAll(): F[Unit] =
    for {
      _ <- warn"delete all posts"
      res <- repository.deleteAll()
      _ <- warn"all posts deleted"
    } yield res


  private def logFoundErrorsOrPosts(errorsOrPosts: Either[String, Vector[Post.Existing[PostId]]]): F[Unit] =
    errorsOrPosts.fold(
      error => debug"posts not found, reason: $error",
      ps => debug"found posts:\n${ps.mkString("\n")}"
    )
}

object PostServiceInterpreter {
  def apply[F[_] : Monad, PostId](parser: PostParser[F, PostId],
                                  validator: PostValidator[F, PostId],
                                  postRepository: PostRepository[F, PostId])
                                 (log: Logging[F])
                                 (implicit idLoggable: Loggable[PostId]): PostServiceInterpreter[F, PostId] = {
    implicit val logging: Logging[F] = log
    new PostServiceInterpreter(parser, validator, postRepository)
  }
}
