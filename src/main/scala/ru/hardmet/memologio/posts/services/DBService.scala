package ru.hardmet.memologio.posts
package services


import java.sql.SQLIntegrityConstraintViolationException

import doobie.h2.implicits._
import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.util.update.Update0
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException
import ru.hardmet.memologio.Memologio
import ru.hardmet.memologio.posts.{Post, Posts}
import ru.hardmet.memologio.db.DB
import ru.hardmet.memologio._
import ru.hardmet.memologio.posts.services.PostService.Service
import tofu.logging.Logging
import ru.hardmet.memologio.posts.services.DBService._
import zio._
import tofu.syntax.logging._

import scala.collection.compat.immutable.ArraySeq

class DBService(xa: Transactor[Task]) extends Service {
  def getData: Memologio[Seq[PostData]] =
    SQL.getData.stream.compile.to[ArraySeq].transact(xa)

  def getOne(name: String): Memologio[Post] = ???

  def putOne(data: PostData): Memologio[PostId] =
    PostService.newId
      .flatMap(id => SQL.putOne(data, id).run.transact(xa) as id)
      .catchSome {
        case _: SQLIntegrityConstraintViolationException => ZIO.fail(AlreadyExists(data.name))
      }

  def remove(name: String): Memologio[Unit] =
    SQL.remove(name).run.transact(xa).unit
}

object DBService {
  val live: URLayer[DB, Posts] =
    ZLayer.fromEffect(ZIO.access[DB](r => new DBService(r.get.transactor)))

  object SQL {
    val getData: Query0[PostData] = sql"SELECT name, description FROM category".query

    def putOne(data: PostData, id: PostId): Update0 =
      sql"INSERT INTO category (id, name, description) VALUES (${id.uuid}, ${data.name}, ${data.description})".update

    def remove(name: String): Update0 =
      sql"DELETE FROM category where name = $name".update
  }
}