package ru.hardmet.memologio
package infrastructure
package repository.doobie

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import cats.data._
import cats.implicits._
import domain.posts.Post
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.implicits.javatime.JavaTimeLocalDateMeta
import doobie.implicits.javatime.JavaTimeLocalDateTimeMeta
import doobie.util.update.Update
import fs2.Stream
import repository.PostRepository
import DoobiePagination._
import cats.effect.Bracket

// TODO add update many just for fun
class DoobiePostRepositoryInterpreter[F[_]: Bracket[?[_], Throwable]](val xa: Transactor[F])
  extends PostRepository[F, UUID] {

  import Statements._

  override def create(post: Post.Data): F[Post.Existing[UUID]] =
    Insert.insert(post)
      .withUniqueGeneratedKeys[UUID]("id").map(id => Post.Existing(id, post)).transact(xa)

  override def update(post: Post.Existing[UUID]): F[Post.Existing[UUID]] =
    UpdatePosts.one(post).run.as(post).transact(xa)

  override def get(id: UUID): F[Option[Post.Existing[UUID]]] =
    Select.one(id).to[Vector].map(_.headOption).transact(xa)

  override def getListByIds(ids: Vector[UUID]): F[Vector[Post.Existing[UUID]]] =
    Select.many(NonEmptyList.fromListUnsafe(ids.toList)).to[Vector].transact(xa)


  override def findByPublishedDate(published: LocalDate): F[Vector[Post.Existing[UUID]]] =
    Select.byPublishedDate(published).to[Vector].transact(xa)

  override def findByPublishedDateTime(published: LocalDateTime): F[Vector[Post.Existing[UUID]]] =
    Select.byPublishedDateTime(published).to[Vector].transact(xa)

  override def findWithLikesAbove(likes: Int): F[Vector[Post.Existing[UUID]]] =
    Select.withLikesAbove(likes).to[Vector].transact(xa)

  override def findWithLikesBelow(likes: Int): F[Vector[Post.Existing[UUID]]] =
    Select.withLikesBelow(likes).to[Vector].transact(xa)

  override def list(pageSize: Int, offset: Int): F[Vector[Post.Existing[UUID]]] =
    paginate[Post.Existing[UUID]](pageSize, offset)(Select.all).to[Vector].transact(xa)

  override def listAll(): F[Vector[Post.Existing[UUID]]] = Select.all.to[Vector].transact(xa)

  override def delete(id: UUID): F[Unit] = Delete.one(id).run.void.transact(xa)

  override def deleteMany(posts: Vector[Post.Existing[UUID]]): F[Unit] =
    Delete.many(NonEmptyList.fromListUnsafe(posts.toList)).run.void.transact(xa)

  override def deleteAll(): F[Unit] = Delete.all.run.void.transact(xa)
}

object Statements {

  object Insert {
    def insert(post: Post.Data): Update0 =
      sql"""
            INSERT INTO post (url, published, likes)
            VALUES (${post.url}, ${post.published}, ${post.likes})
            """.update

    // TODO: write route for insert many
    def many(posts: Vector[Post.Data]): Stream[ConnectionIO, Post.Existing[UUID]] =
      Update[Post.Data]("INSERT INTO post (url, published, likes) values (?, ?, ?)")
        .updateManyWithGeneratedKeys[Post.Existing[UUID]]("id", "url", "published", "likes")(posts)
  }

  object Select {

    val all: Query0[Post.Existing[UUID]] =
      sql"""
            SELECT id, url, published, likes
            FROM post
            ORDER BY published DESC
            """.query

    def one(id: UUID): Query0[Post.Existing[UUID]] =
      sql"""
            SELECT id, url, published, likes
            FROM post
            WHERE id = $id
            """.query

    def many(ids: NonEmptyList[UUID]): Query0[Post.Existing[UUID]] =
      (sql"""
            SELECT id, url, published, likes
            FROM post
            WHERE """ ++ Fragments.in(fr"id", ids)
        ).query

    def byPublishedDate(localDate: LocalDate): Query0[Post.Existing[UUID]] =
      sql"""
            SELECT id, url, published, likes
            FROM post
            WHERE published::date = $localDate
            """.query

    def byPublishedDateTime(dateTime: LocalDateTime): Query0[Post.Existing[UUID]] =
      sql"""
            SELECT id, url, published, likes
            FROM post
            WHERE published = $dateTime
            """.query

    def withLikesAbove(likes: Int): Query0[Post.Existing[UUID]] =
      sql"""
            SELECT *
            FROM post
            WHERE likes > $likes
            """.query

    def withLikesBelow(likes: Int): Query0[Post.Existing[UUID]] =
      sql"""
            SELECT *
            FROM post
            WHERE likes < $likes
            """.query
  }

  object UpdatePosts {

    def one(post: Post.Existing[UUID]): Update0 =
      sql"""
            UPDATE post
            SET url = ${post.url}, published = ${post.published}, likes = ${post.likes}
            WHERE id = ${post.id}
            """.update
  }

  object Delete {

    def one(id: UUID): Update0 =
      sql"""
            DELETE
            FROM post
            WHERE id = $id
            """.update

    def many(posts: NonEmptyList[Post.Existing[UUID]]): Update0 =
      (sql"""
            DELETE
            FROM post
            WHERE """ ++ Fragments.in(fr"id", posts.map(_.id))
        ).update

    val all: Update0 =
      sql"""
            DELETE
            FROM post
            """.update
  }
}
