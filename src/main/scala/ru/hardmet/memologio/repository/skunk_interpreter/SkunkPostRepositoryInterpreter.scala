package ru.hardmet.memologio
package repository
package skunk_interpreter

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import cats.effect.{Bracket, Resource, Sync}
import cats.syntax.all._
import skunk.{Codec, Command, Fragment, Query, Void, ~}
import skunk.codec.all._
import skunk.implicits._
import ru.hardmet.memologio.domain.posts.Post
import ru.hardmet.memologio.repository.skunk_interpreter.SkunkPostRepositoryInterpreter.ChunkSizeInBytes

class SkunkPostRepositoryInterpreter[F[_]: Sync](val sessionResource: Resource[F, skunk.Session[F]]) extends PostRepository[F, UUID] {
  import PostStatements._

  override def create(post: Post[UUID]): F[Post.Existing[UUID]] =
    post match {
      case p: Post.Existing[UUID] => Bracket[F, Throwable].raiseError(
        new RuntimeException(
          s"Failed to create post: ${p.id} because it is already exist."
        )
      )
      case p: Post.Data => insertOne(p)
    }

  private def insertOne(data: Post.Data): F[Post.Existing[UUID]] =
    prepareAndQuery(Insert.one, data)

  override def update(post: Post.Existing[UUID]): F[Post.Existing[UUID]] =
    prepareAndQuery(Update.one, post)

  override def get(id: UUID): F[Option[Post.Existing[UUID]]] =
    prepareQueryAndCompile(Select.one, id)
      .map(_.headOption)

  override def getListByIds(ids: Vector[UUID]): F[Vector[Post.Existing[UUID]]] =
    prepareQueryAndCompile(Select.many(ids.size), ids.to(List))

  override def findByPublishedDate(published: LocalDate): F[Vector[Post.Existing[UUID]]] =
    prepareQueryAndCompile(Select.byPublishedDate, published)

  override def findByPublishedDateTime(published: LocalDateTime): F[Vector[Post.Existing[UUID]]] =
    prepareQueryAndCompile(Select.byPublishedDateTime, published)

  override def findWithLikesAbove(likes: Int): F[Vector[Post.Existing[UUID]]] =
    prepareQueryAndCompile(Select.withLikesAbove, likes)

  override def findWithLikesBelow(likes: Int): F[Vector[Post.Existing[UUID]]] =
    prepareQueryAndCompile(Select.withLikesBelow, likes)

  override def list(pageSize: Int, offset: Int): F[Vector[Post.Existing[UUID]]] =
    prepareQueryAndCompile(Select.allWithPagination, pageSize -> offset)

  override def listAll(): F[Vector[Post.Existing[UUID]]] =
    sessionResource.use { session =>
      session
        .execute(Select.all)
        .map(_.to(Vector))
    }

  override def delete(id: UUID): F[Unit] =
    sessionResource.use { session =>
      session
        .prepare(Delete.one)
        .use { preparedQuery =>
          preparedQuery
            .execute(id)
            .void
        }
    }


  override def deleteMany(posts: Vector[Post.Existing[UUID]]): F[Unit] =
    sessionResource.use{ session =>
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
  private [skunk_interpreter] val ChunkSizeInBytes: Int = 1024
}

object PostStatements {

  final implicit private class TodoDataCompanionOps(private val data: Post.Data.type) {
    val codec: Codec[Post.Data] =
      (text ~ timestamp ~ int4).gimap[Post.Data]
  }

  final implicit private class TodoExistingCompanionOps(private val existing: Post.Existing.type) {
    val codec: Codec[Post.Existing[UUID]] =
      (uuid ~ Post.Data.codec).gimap[Post.Existing[UUID]]
  }

  object Insert {
    val one: Query[Post.Data, Post.Existing[UUID]] =
      sql"""
               INSERT INTO post (url, published, likes)
               VALUES (${Post.Data.codec})
            RETURNING *
         """.query(Post.Existing.codec)

    // TODO use or remove next three
    def many(size: Int): Query[List[Post.Data], Post.Existing[UUID]] =
      sql"""
               INSERT INTO post (url, published, likes)
               VALUES (${Post.Data.codec.list(size)})
            RETURNING *
         """.query(Post.Existing.codec)

    object WithUUID {
      val one: Command[Post.Existing[UUID]] =
        sql"""
              INSERT INTO post
              VALUES (${Post.Existing.codec})
           """.command

      def many(size: Int): Command[List[Post.Existing[UUID]]] =
        sql"""
              INSERT INTO post
              VALUES (${Post.Existing.codec.list(size)})
           """.command
    }
  }

  object Select {
    val allFragment: Fragment[Void] =
      sql"""
            SELECT *
              FROM post
              ORDER BY published DESC
         """

    val allWithPagination: Query[(Int, Int), Post.Existing[UUID]] =
      PaginationQuery.paginate(Select.allFragment)(Post.Existing.codec)

    val all: Query[Void, Post.Existing[UUID]] = allFragment.query(Post.Existing.codec)

    val one: Query[UUID, Post.Existing[UUID]] =
      sql"""
            SELECT *
              FROM post
             WHERE id ~ $uuid
         """.query(Post.Existing.codec)


    def many(size: Int): Query[List[UUID], Post.Existing[UUID]] =
      sql"""
            SELECT *
              FROM post
             WHERE id IN (${uuid.list(size)})
         """.query(Post.Existing.codec)

    val byPublishedDate: Query[LocalDate, Post.Existing[UUID]] =
      sql"""
            SELECT *
              FROM todo
             WHERE published::date ~ $date
         """.query(Post.Existing.codec)

    val byPublishedDateTime: Query[LocalDateTime, Post.Existing[UUID]] =
      sql"""
            SELECT *
              FROM todo
             WHERE published ~ $timestamp
         """.query(Post.Existing.codec)

    val withLikesAbove: Query[Int, Post.Existing[UUID]] =
      sql"""
            SELECT *
              FROM post
             WHERE likes > $int4
         """.query(Post.Existing.codec)

    val withLikesBelow: Query[Int, Post.Existing[UUID]] =
      sql"""
            SELECT *
              FROM post
             WHERE likes < $int4
         """.query(Post.Existing.codec)
  }

  object Update {
    val one: Query[Post.Existing[UUID], Post.Existing[UUID]] =
      sql"""
               UPDATE post
                  SET url = $text, published = $timestamp, likes = $int4
                WHERE id = $uuid
            RETURNING *
         """.query(Post.Existing.codec).contramap(toTwiddle)

    private def toTwiddle(post: Post.Existing[UUID]): String ~ LocalDateTime ~ Int ~ UUID =
      post.data.url ~ post.data.published ~ post.data.likes ~ post.id
  }

  object Delete {

    def one: Command[UUID] =
      sql"""
            DELETE
              FROM post
             WHERE id = $uuid
         """.command

    // TODO use or remove
    def many(size: Int): Command[List[UUID]] =
      sql"""
            DELETE
              FROM post
             WHERE id IN (${uuid.list(size)})
         """.command

    val all: Command[Void] =
      sql"""
            DELETE
              FROM post
         """.command
  }
}
