package ru.hardmet.memologio
package services

import java.time.LocalDateTime

import cats.{Monad, Traverse}
import cats.data.{EitherNec, EitherT, NonEmptyChain}
import cats.data._
import cats.implicits._
import domain.posts.Post
import domain.WrongDataForPostError
import infrastructure.repository.PostRepository
import ru.hardmet.memologio.util.Parse
import tofu.logging.{Loggable, Logging}
import tofu.syntax.logging.LoggingInterpolator
import Parse.parseStringToLocalDateTime

// todo refactoring logs
class PostServiceInterpreter[F[_]: Monad: Logging, PostId](validator: Validator[F, PostId],
                                                           repository: PostRepository[F, PostId])
                                                          (implicit idLoggable: Loggable[PostId])
  extends PostService[F, PostId] {

  override def createOne(url: String, published: String, likes: Int): F[EitherNec[String, Post.Existing[PostId]]] =
    for {
      _ <- debug"creating post url: $url, published: $published, likes: $likes"
      postData <- validator.validatePost(url, parseStringToLocalDateTime(published), likes)
      post <- postData.traverse(repository.create)
      _ <- post.fold(
        errors => debug"post has not created, reasons: ${errors.show}",
        p => debug"post $p created"
      )
    } yield post

  override def createMany(posts: Vector[Post.Data]): F[EitherNec[Vector[String], Vector[Post.Existing[PostId]]]] = ???
//    for {
//      _ <- debug"createMany posts $posts"
//      posts <- writeMany(posts)
//      _ <- debug"posts: ${posts.map(_.data)} created"
//    } yield posts

  private def writeMany[T <: Post[PostId]](posts: Vector[T]): F[Vector[Post.Existing[PostId]]] =
    posts.traverse {
      case data: Post.Data => repository.create(data)
      case post: Post.Existing[PostId] => repository.update(post)
    }

  override def readOneById(id: String): F[Either[String, Post.Existing[PostId]]] = ???
//    for {
//      _ <- debug"read post by id $id"
//      post <- repository.get(id)
//      _ <- debug"post: ${post.map(_.data)} found"
//    } yield post

  override def readManyByIds(ids: Vector[String]): F[EitherNec[String, Vector[Post.Existing[PostId]]]] = ???
//    for {
//      _ <- debug"read many posts by ids ${ids.mkString(", ")}"
//      posts <- repository.getListByIds(ids)
//      _ <- debug"posts: ${posts.map(_.data)} found"
//    } yield posts

  override def readManyByPublishedDate(published: String): F[Either[String, Vector[Post.Existing[PostId]]]] = ???
//    for {
//      _ <- debug"read post by published date $published"
//      posts <- repository.findByPublishedDate(published)
//      _ <- debug"posts: ${posts.map(_.data)} found"
//    } yield posts

  override def readManyByPublishedDateTime(published: String): F[Either[String, Vector[Post.Existing[PostId]]]] = ???
//    for {
//      _ <- debug"read post by published datetime $published"
//      posts <- repository.findByPublishedDateTime(published)
//      _ <- debug"posts: ${posts.map(_.data)} found"
//    } yield posts

  override def readAll: F[Vector[Post.Existing[PostId]]] = ???
//    for {
//      _ <- debug"read all posts"
//      posts <- repository.listAll()
//      _ <- debug"posts: ${posts.map(_.data)} found"
//    } yield posts

  override def updateOne(id: String)
                        (url: String, published: String, likes: Int): F[EitherNec[String, Post.Existing[PostId]]] = ???
//    for {
//      _ <- debug"update post ${post.data}"
//      post <- repository.update(post)
//      _ <- debug"post: $post updated"
//    } yield post

  override def updateMany(posts: Vector[Post.Existing[PostId]]): F[EitherNec[Vector[String], Post.Existing[PostId]]] = ???
//    for {
//      _ <- debug"update some posts: ${posts.map(_.data)}"
//      updatedPosts <- writeMany(posts)
//      _ <- debug"posts: ${updatedPosts.map(_.data)} found"
//    } yield updatedPosts

  override def deleteOne(id: String): F[Either[String, Unit]] = ???
//    for {
//      _ <- debug"delete post with id $id"
//      res <- repository.delete(post.id)
//      _ <- debug"post $id deleted"
//    } yield res

  override def deleteMany(ids: Vector[String]): F[EitherNec[String, Unit]] = ???
//    for {
//      _ <- warn"delete posts: ${posts.map(_.data)}"
//      res <- repository.deleteMany(posts)
//      _ <- warn"posts: ${posts.map(_.data)} deleted"
//    } yield res

  override def deleteAll(): F[Unit] = ???
//    for {
//      _ <- warn"delete all posts"
//      res <- repository.deleteAll()
//      _ <- warn"all posts deleted"
//    } yield res
}

object PostServiceInterpreter {
  def apply[F[_]: Monad, PostId](validator: Validator[F, PostId], postRepository: PostRepository[F, PostId])
                                (log: Logging[F])
                                (implicit idLoggable: Loggable[PostId]): PostServiceInterpreter[F, PostId] = {
    implicit val logging: Logging[F] = log
    new PostServiceInterpreter(validator, postRepository)
  }
}
