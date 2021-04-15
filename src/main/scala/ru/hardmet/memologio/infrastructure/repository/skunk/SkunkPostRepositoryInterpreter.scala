package ru.hardmet.memologio
package infrastructure
package repository.skunk

import SkunkPostRepositoryInterpreter.ChunkSizeInBytes
import cats.effect.{Resource, Sync}
import cats.syntax.functor._
import repository.PostRepository
import ru.hardmet.memologio.services.post.domain.PostId.postId
import ru.hardmet.memologio.services.post.domain.Url.url
import services.post.domain._
import skunk.codec.all._
import skunk.implicits._
import skunk.{Codec, Command, Fragment, Query, Void, ~}

import java.time.{LocalDate, LocalDateTime}

class SkunkPostRepositoryInterpreter[F[_] : Sync](val sessionResource: Resource[F, skunk.Session[F]]) extends PostRepository[F] {

  import PostStatements._

  override def create(post: Data): F[Existing] =
    prepareAndQuery(Insert.one, post)

  override def update(post: Existing): F[Existing] =
    prepareAndQuery(Update.one, post)

  override def get(id: PostId): F[Option[Existing]] =
    prepareQueryAndCompile(Select.one, id)
      .map(_.headOption)

  override def getListByIds(ids: Vector[PostId]): F[Vector[Existing]] =
    prepareQueryAndCompile(Select.many(ids.size), ids.to(List))

  override def findByPublishedDate(published: LocalDate): F[Vector[Existing]] =
    prepareQueryAndCompile(Select.byPublishedDate, published)

  override def findByPublishedDateTime(published: LocalDateTime): F[Vector[Existing]] =
    prepareQueryAndCompile(Select.byPublishedDateTime, published)

  override def findWithLikesAbove(likes: Int): F[Vector[Existing]] =
    prepareQueryAndCompile(Select.withLikesAbove, likes)

  override def findWithLikesBelow(likes: Int): F[Vector[Existing]] =
    prepareQueryAndCompile(Select.withLikesBelow, likes)

  override def list(pageSize: Int, offset: Int): F[Vector[Existing]] =
    prepareQueryAndCompile(Select.allWithPagination, pageSize -> offset)

  override def listAll(): F[Vector[Existing]] =
    sessionResource.use { session =>
      session
        .execute(Select.all)
        .map(_.to(Vector))
    }

  override def delete(id: PostId): F[Unit] =
    sessionResource.use { session =>
      session
        .prepare(Delete.one)
        .use { preparedQuery =>
          preparedQuery
            .execute(id)
            .void
        }
    }


  override def deleteMany(posts: Vector[Existing]): F[Unit] =
    sessionResource.use { session =>
      session
        .prepare(Delete.many(posts.size))
        .use { preparedCommand =>
          preparedCommand
            .execute(posts.to(List).map(_.id))
            .void
        }
    }

  override def deleteAll(): F[Unit] =
    sessionResource.use { session =>
      session
        .execute(Delete.all)
        .void
    }

  private def prepareAndQuery[A, B](query: Query[A, B], statementParam: A): F[B] =
    sessionResource.use { session =>
      session
        .prepare(query)
        .use { preparedQuery =>
          preparedQuery.unique(statementParam)
        }
    }

  private def prepareQueryAndCompile[A, B](query: Query[A, B], param: A, chunkSize: Int = ChunkSizeInBytes): F[Vector[B]] =
    sessionResource.use { session =>
      session
        .prepare(query)
        .use { preparedQuery =>
          preparedQuery
            .stream(param, chunkSize)
            .compile
            .toVector
        }
    }
}

object SkunkPostRepositoryInterpreter {
  private[skunk] val ChunkSizeInBytes: Int = 1024
}

object PostStatements {

  final implicit private class PostDataCompanionOps(private val data: Data.type) {
    val codec: Codec[Data] =
      (url ~ timestamp ~ int4).gimap[Data]
  }

  final implicit private class PostExistingCompanionOps(private val existing: Existing.type) {
    val codec: Codec[Existing] =
      (postId ~ Data.codec).gimap[Existing]
  }

  object Insert {
    val one: Query[Data, Existing] =
      sql"""
               INSERT INTO post (url, published, likes)
               VALUES (${Data.codec})
            RETURNING *
         """.query(Existing.codec)

    // TODO use or remove
    def many(size: Int): Query[List[Data], Existing] =
      sql"""
               INSERT INTO post (url, published, likes)
               VALUES (${Data.codec.list(size)})
            RETURNING *
         """.query(Existing.codec)
  }

  object Select {
    val allFragment: Fragment[Void] =
      sql"""
            SELECT *
              FROM post
              ORDER BY published DESC
         """

    val allWithPagination: Query[(Int, Int), Existing] =
      PaginationQuery.paginate(Select.allFragment)(Existing.codec)

    val all: Query[Void, Existing] = allFragment.query(Existing.codec)

    val one: Query[PostId, Existing] =
      sql"""
            SELECT *
              FROM post
             WHERE id = $postId
         """.query(Existing.codec)


    def many(size: Int): Query[List[PostId], Existing] =
      sql"""
            SELECT *
              FROM post
             WHERE id IN (${postId.list(size)})
         """.query(Existing.codec)

    val byPublishedDate: Query[LocalDate, Existing] =
      sql"""
            SELECT *
              FROM post
             WHERE published::date = $date
         """.query(Existing.codec)

    val byPublishedDateTime: Query[LocalDateTime, Existing] =
      sql"""
            SELECT *
              FROM post
             WHERE published = $timestamp
         """.query(Existing.codec)

    // TODO write route for this functionality
    val withLikesAbove: Query[Int, Existing] =
      sql"""
            SELECT *
              FROM post
             WHERE likes > $int4
         """.query(Existing.codec)

    val withLikesBelow: Query[Int, Existing] =
      sql"""
            SELECT *
              FROM post
             WHERE likes < $int4
         """.query(Existing.codec)
  }

  object Update {
    val one: Query[Existing, Existing] =
      sql"""
               UPDATE post
                  SET url = $url, published = $timestamp, likes = $int4
                WHERE id = $postId
            RETURNING *
         """.query(Existing.codec).contramap(toTwiddle)

    private def toTwiddle(post: Existing): Url ~ LocalDateTime ~ Int ~ PostId =
      post.data.url ~ post.data.published ~ post.data.likes ~ post.id
  }

  object Delete {

    def one: Command[PostId] =
      sql"""
            DELETE
              FROM post
             WHERE id = $postId
         """.command

    def many(size: Int): Command[List[PostId]] =
      sql"""
            DELETE
              FROM post
             WHERE id IN (${postId.list(size)})
         """.command

    val all: Command[Void] =
      sql"""
            DELETE
              FROM post
         """.command
  }
}
