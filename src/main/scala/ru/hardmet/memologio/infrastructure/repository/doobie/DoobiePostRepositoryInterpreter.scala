package ru.hardmet.memologio
package infrastructure
package repository.doobie

import DoobiePagination._
import cats.data._
import cats.effect.Bracket
import cats.syntax.functor._
import doobie._
import doobie.implicits._
import doobie.implicits.javatime._
import doobie.postgres.implicits._
import doobie.util.update.Update
import fs2.Stream
import repository.PostRepository
import services.post.domain.{Data, Existing, PostId}

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

// TODO add update many just for fun
class DoobiePostRepositoryInterpreter[F[_] : Bracket[*[_], Throwable]](val xa: Transactor[F])
  extends PostRepository[F] {

  import Statements._

  override def create(post: Data): F[Existing] =
    Insert.insert(post)
      .withUniqueGeneratedKeys[UUID]("id").map(id => Existing(PostId(id), post)).transact(xa)

  override def update(post: Existing): F[Existing] =
    UpdatePosts.one(post).run.as(post).transact(xa)

  override def get(id: PostId): F[Option[Existing]] =
    Select.one(id).to[Vector].map(_.headOption).transact(xa)

  override def getListByIds(ids: Vector[PostId]): F[Vector[Existing]] =
    Select.many(NonEmptyList.fromListUnsafe(ids.toList)).to[Vector].transact(xa)


  override def findByPublishedDate(published: LocalDate): F[Vector[Existing]] =
    Select.byPublishedDate(published).to[Vector].transact(xa)

  override def findByPublishedDateTime(published: LocalDateTime): F[Vector[Existing]] =
    Select.byPublishedDateTime(published).to[Vector].transact(xa)

  override def findWithLikesAbove(likes: Int): F[Vector[Existing]] =
    Select.withLikesAbove(likes).to[Vector].transact(xa)

  override def findWithLikesBelow(likes: Int): F[Vector[Existing]] =
    Select.withLikesBelow(likes).to[Vector].transact(xa)

  override def list(pageSize: Int, offset: Int): F[Vector[Existing]] =
    paginate[Existing](pageSize, offset)(Select.all).to[Vector].transact(xa)

  override def listAll(): F[Vector[Existing]] = Select.all.to[Vector].transact(xa)

  override def delete(id: PostId): F[Unit] = Delete.one(id).run.void.transact(xa)

  override def deleteMany(posts: Vector[Existing]): F[Unit] =
    Delete.many(NonEmptyList.fromListUnsafe(posts.toList)).run.void.transact(xa)

  override def deleteAll(): F[Unit] = Delete.all.run.void.transact(xa)
}

object Statements {

  object Insert {
    def insert(post: Data): Update0 =
      sql"""
            INSERT INTO post (url, published, likes)
            VALUES (${post.url}, ${post.published}, ${post.likes})
            """.update

    // TODO: write route for insert many
    def many(posts: Vector[Data]): Stream[ConnectionIO, Existing] =
      Update[Data]("INSERT INTO post (url, published, likes) values (?, ?, ?)")
        .updateManyWithGeneratedKeys[Existing]("id", "url", "published", "likes")(posts)
  }

  object Select {

    val all: Query0[Existing] =
      sql"""
            SELECT id, url, published, likes
            FROM post
            ORDER BY published DESC
            """.query

    def one(id: PostId): Query0[Existing] =
      sql"""
            SELECT id, url, published, likes
            FROM post
            WHERE id = $id
            """.query

    def many(ids: NonEmptyList[PostId]): Query0[Existing] =
      (sql"""
            SELECT id, url, published, likes
            FROM post
            WHERE """ ++ Fragments.in(fr"id", ids)
        ).query

    def byPublishedDate(localDate: LocalDate): Query0[Existing] =
      sql"""
            SELECT id, url, published, likes
            FROM post
            WHERE published::date = $localDate
            """.query

    def byPublishedDateTime(dateTime: LocalDateTime): Query0[Existing] =
      sql"""
            SELECT id, url, published, likes
            FROM post
            WHERE published = $dateTime
            """.query

    def withLikesAbove(likes: Int): Query0[Existing] =
      sql"""
            SELECT *
            FROM post
            WHERE likes > $likes
            """.query

    def withLikesBelow(likes: Int): Query0[Existing] =
      sql"""
            SELECT *
            FROM post
            WHERE likes < $likes
            """.query
  }

  object UpdatePosts {

    def one(post: Existing): Update0 =
      sql"""
            UPDATE post
            SET url = ${post.url}, published = ${post.published}, likes = ${post.likes}
            WHERE id = ${post.id}
            """.update
  }

  object Delete {

    def one(id: PostId): Update0 =
      sql"""
            DELETE
            FROM post
            WHERE id = $id
            """.update

    def many(posts: NonEmptyList[Existing]): Update0 =
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
