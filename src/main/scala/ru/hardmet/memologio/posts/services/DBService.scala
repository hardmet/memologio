package ru.hardmet.memologio.posts
package services


import java.sql.SQLIntegrityConstraintViolationException
import java.util.UUID

import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.util.update.{Update, Update0}
import ru.hardmet.memologio.db.DB
import ru.hardmet.memologio.posts.services.DBService._
import ru.hardmet.memologio.posts.services.PostService.Service
import ru.hardmet.memologio.{Memologio, _}
import zio._


import doobie.postgres.implicits._
import tofu.logging.Logging
import tofu.syntax.logging._

import scala.collection.compat.immutable.ArraySeq

class DBService(xa: Transactor[Task]) extends Service {
  def getData: Memologio[Seq[PostData]] =
    SQL.getData.stream.compile.to[ArraySeq].transact(xa).map {
      sourceAndTags =>
        sourceAndTags.groupBy { case (source, _) => source }.values.map {
          sourceAndTagsGroup =>
            PostData(
              sourceAndTagsGroup.head._1,
              sourceAndTagsGroup.map(_._2).foldLeft(List.empty[TagData]) {
                case (tags, tagOption) =>
                  tagOption.fold(tags){
                    name => TagData(name) :: tags
                  }
              }
            )
        }.toSeq
    }

  def getOne(name: String): Memologio[Post] = ???

  def putOne(data: PostData): Memologio[PostId] =
      (for {
        postId <- PostService.newId(x => PostId(x))
        tagIds <- UIO.collectAll(data.tags.map(_ => PostService.newId(x => TagId(x))))
        postId <- SQL.putOne(data, postId, tagIds).transact(xa) as postId
      } yield postId)
        .catchSome {
          case _: SQLIntegrityConstraintViolationException => ZIO.fail(AlreadyExists(data.source))
        }

  def remove(name: String): Memologio[Unit] =
    SQL.remove(name).run.transact(xa).unit
}

object DBService {
  val live: URLayer[DB, Posts] =
    ZLayer.fromEffect(ZIO.access[DB](r => new DBService(r.get.transactor)))

  object SQL {
    type TagWrapper = (UUID, String, UUID)

    import cats.implicits._
    import doobie.util._


    val getData: doobie.Query0[(String, Option[String])] =
      sql"SELECT p.source, tags.name FROM posts as p left join tags on p.id = post_id"
        .query[(String, Option[String])]

    def putOne(data: PostData, id: PostId, tagIds: List[TagId]): doobie.ConnectionIO[Int] = {
      val insertPost: doobie.ConnectionIO[Int] = sql"INSERT INTO posts (id, source) VALUES (${id.uuid}, ${data.source})".update.run
      val insertTags: doobie.ConnectionIO[Int] = Update[TagWrapper]("insert into tags (id, name, post_id) values (?, ?, ?)")
        .updateMany(data.tags.zip(tagIds).map{ case (tag, tagId) => (tagId.uuid, tag.name, id.uuid)})
      (insertPost, insertTags).mapN(_ + _)
    }

    def remove(name: String): Update0 =
      sql"DELETE FROM category where name = $name".update
  }
}
