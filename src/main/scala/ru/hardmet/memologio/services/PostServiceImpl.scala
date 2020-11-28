package ru.hardmet.memologio
package services

import java.time.LocalDate

import cats.Applicative
import cats.implicits.toTraverseOps
import ru.hardmet.memologio.domain.posts.Post
import ru.hardmet.memologio.repository.PostRepository

class PostServiceImpl[F[_]: Applicative, PostId](postRepository: PostRepository[F, PostId]) extends PostService[F, PostId] {

  override def createOne(post: Post.Data): F[Post.Existing[PostId]] = postRepository.create(post)

  override def createMany(posts: Vector[Post.Data]): F[Vector[Post.Existing[PostId]]] = writeMany(posts)

  private def writeMany[T <: Post[PostId]](posts: Vector[T]): F[Vector[Post.Existing[PostId]]] =
    posts.traverse {
      case data: Post.Data => postRepository.create(data)
      case post: Post.Existing[PostId] => postRepository.update(post)
    }

  override def readOneById(id: PostId): F[Option[Post.Existing[PostId]]] = postRepository.get(id)

  override def readManyById(ids: Vector[PostId]): F[Vector[Post.Existing[PostId]]] = postRepository.getListByIds(ids)

  override def readManyByPublishedDate(published: LocalDate): F[Vector[Post.Existing[PostId]]] =
    postRepository.findByPublishedDate(published)

  override def readAll: F[Vector[Post.Existing[PostId]]] = postRepository.listAll()

  override def updateOne(post: Post.Existing[PostId]): F[Post.Existing[PostId]] = postRepository.update(post)

  // todo optimize delegation to update one by one
  override def updateMany(posts: Vector[Post.Existing[PostId]]): F[Vector[Post.Existing[PostId]]] = writeMany(posts)

  override def deleteOne(post: Post.Existing[PostId]): F[Unit] = postRepository.delete(post.id)

  override def deleteMany(posts: Vector[Post.Existing[PostId]]): F[Unit] = postRepository.deleteMany(posts)

  override def deleteAll(): F[Unit] = postRepository.deleteAll()
}
