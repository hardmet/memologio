package ru.hardmet.memologio.posts
package services


import java.sql.SQLIntegrityConstraintViolationException
import java.util.UUID

import doobie.implicits._
import doobie.util.{Get, Put}
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.util.update.{Update, Update0}
import ru.hardmet.memologio.{Memologio, _}
import ru.hardmet.memologio.db.DB
import ru.hardmet.memologio.posts.services.DBService._
import ru.hardmet.memologio.posts.services.PostService.Service
import zio._

import scala.collection.compat.immutable.ArraySeq

class DBService(xa: Transactor[Task]) extends Service {
  def getData: Memologio[Seq[PostData]] =
    SQL.getData.stream.compile.to[ArraySeq].transact(xa)

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
    import cats.implicits._

//    implicit val tagGet: Get[TagData] = Get[(Option[UUID], String, UUID)].tmap{ case (id: Option[UUID], name: String, postId: UUID) => TagData(id, name, postId)}
//    implicit val tagPut: Put[TagData] = Put[(Option[UUID], String, UUID)].tcontramap(x => (x.uuid, x.name, x.postId))

//    implicit val uuidGet: Get[UUID] = Get[String].tmap(id => UUID.fromString(id))
//    implicit val uuidPut: Put[UUID] = Put[String].tcontramap(x => x.toString)

    val getData: Query0[PostData] = sql"SELECT source, tags.id, tags.name FROM posts as p left join tags on p.id = post_id".query

    def putOne(data: PostData, id: PostId, tagIds: List[TagId]): doobie.ConnectionIO[Int] = {
      val insertPost = sql"INSERT INTO posts (id, source) VALUES (${id.uuid}, ${data.source})".update.run
      val insertTags = Update[TagData]("insert into tags (id, name, post_id) values (?, ?, ?)")
        .updateMany(data.tags.zip(tagIds).map{ case (tag, tagId) => tag.copy(uuid = Option(tagId.uuid))})
      (insertPost, insertTags).mapN(_ + _)
    }

    def remove(name: String): Update0 =
      sql"DELETE FROM category where name = $name".update
  }
}