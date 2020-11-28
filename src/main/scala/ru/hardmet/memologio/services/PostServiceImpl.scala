package ru.hardmet.memologio
package services

import ru.hardmet.memologio.domain.posts.Post
import ru.hardmet.memologio.repository.PostRepository

//todo implement
class PostServiceImpl[F[_], PostId](postRepository: PostRepository[F, PostId]) extends PostService[F, PostId] {
  override def createOne(post: Post.Data): F[Post.Existing[PostId]] = ???

  override def createMany(posts: Vector[Post.Data]): F[Vector[Post.Existing[PostId]]] = ???

  override def readOneById(id: PostId): F[Option[Post.Existing[PostId]]] = ???

  override def readManyById(ids: Vector[PostId]): F[Vector[Post.Existing[PostId]]] = ???

  override def readManyByURL(url: String): F[Vector[Post.Existing[PostId]]] = ???

  override def readAll: F[Vector[Post.Existing[PostId]]] = ???

  override def updateOne(post: Post.Existing[PostId]): F[Post.Existing[PostId]] = ???

  override def updateMany(posts: Vector[Post.Existing[PostId]]): F[Vector[Post.Existing[PostId]]] = ???

  override def deleteOne(post: Post.Existing[PostId]): F[Unit] = ???

  override def deleteMany(posts: Vector[Post.Existing[PostId]]): F[Unit] = ???

  override def deleteAll(): F[Unit] = ???
}
