package ru.hardmet.memologio
package services

import java.time.{LocalDate, LocalDateTime}

import cats.Monad
import cats.implicits._
import domain.posts.Post
import infrastructure.repository.PostRepository
import tofu.logging.{Loggable, Logging}
import tofu.syntax.logging.LoggingInterpolator

class PostServiceInterpreter[F[_]: Monad: Logging, PostId](repository: PostRepository[F, PostId])
                                                          (implicit idLoggable: Loggable[PostId])
  extends PostService[F, PostId] {

  override def createOne(post: Post.Data): F[Post.Existing[PostId]] =
    for {
      _ <- debug"creating post $post"
      post <- repository.create(post)
      _ <- debug"post ${post.data} created"
    } yield post

  override def createMany(posts: Vector[Post.Data]): F[Vector[Post.Existing[PostId]]] =
    for {
      _ <- debug"createMany posts $posts"
      posts <- writeMany(posts)
      _ <- debug"posts: ${posts.map(_.data)} created"
    } yield posts

  private def writeMany[T <: Post[PostId]](posts: Vector[T]): F[Vector[Post.Existing[PostId]]] =
    posts.traverse {
      case data: Post.Data => repository.create(data)
      case post: Post.Existing[PostId] => repository.update(post)
    }

  override def readOneById(id: PostId): F[Option[Post.Existing[PostId]]] =
    for {
      _ <- debug"read post by id $id"
      post <- repository.get(id)
      _ <- debug"post: ${post.map(_.data)} found"
    } yield post

  override def readManyByIds(ids: Vector[PostId]): F[Vector[Post.Existing[PostId]]] =
    for {
      _ <- debug"read many posts by ids ${ids.mkString(", ")}"
      posts <- repository.getListByIds(ids)
      _ <- debug"posts: ${posts.map(_.data)} found"
    } yield posts

  override def readManyByPublishedDate(published: LocalDate): F[Vector[Post.Existing[PostId]]] =
    for {
      _ <- debug"read post by published date $published"
      posts <- repository.findByPublishedDate(published)
      _ <- debug"posts: ${posts.map(_.data)} found"
    } yield posts

  override def readManyByPublishedDateTime(published: LocalDateTime): F[Vector[Post.Existing[PostId]]] =
    for {
      _ <- debug"read post by published datetime $published"
      posts <- repository.findByPublishedDateTime(published)
      _ <- debug"posts: ${posts.map(_.data)} found"
    } yield posts

  override def readAll: F[Vector[Post.Existing[PostId]]] =
    for {
      _ <- debug"read all posts"
      posts <- repository.listAll()
      _ <- debug"posts: ${posts.map(_.data)} found"
    } yield posts

  override def updateOne(post: Post.Existing[PostId]): F[Post.Existing[PostId]] =
    for {
      _ <- debug"update post ${post.data}"
      post <- repository.update(post)
      _ <- debug"post: $post updated"
    } yield post

  override def updateMany(posts: Vector[Post.Existing[PostId]]): F[Vector[Post.Existing[PostId]]] =
    for {
      _ <- debug"update some posts: ${posts.map(_.data)}"
      updatedPosts <- writeMany(posts)
      _ <- debug"posts: ${updatedPosts.map(_.data)} found"
    } yield updatedPosts

  override def deleteOne(post: Post.Existing[PostId]): F[Unit] =
    for {
      _ <- debug"delete post ${post.data}"
      res <- repository.delete(post.id)
      _ <- debug"post: ${post.data} deleted"
    } yield res

  override def deleteMany(posts: Vector[Post.Existing[PostId]]): F[Unit] =
    for {
      _ <- warn"delete posts: ${posts.map(_.data)}"
      res <- repository.deleteMany(posts)
      _ <- warn"posts: ${posts.map(_.data)} deleted"
    } yield res

  override def deleteAll(): F[Unit] =
    for {
      _ <- warn"delete all posts"
      res <- repository.deleteAll()
      _ <- warn"all posts deleted"
    } yield res
}

object PostServiceInterpreter {
  def apply[F[_]: Monad, PostId](postRepository: PostRepository[F, PostId])(log: Logging[F])
                                 (implicit idLoggable: Loggable[PostId]): PostServiceInterpreter[F, PostId] = {
    implicit val logging: Logging[F] = log
    new PostServiceInterpreter(postRepository)
  }
}
