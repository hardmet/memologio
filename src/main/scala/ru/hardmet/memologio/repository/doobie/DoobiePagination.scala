package ru.hardmet.memologio
package repository.doobie

import doobie._
import doobie.implicits._

trait DoobiePagination {
  def limit[A: Read](lim: Int)(q: Query0[A]): Query0[A] =
    (q.toFragment ++ fr"LIMIT $lim").query

  def paginate[A: Read](lim: Int, offset: Int)(q: Query0[A]): Query0[A] =
    (q.toFragment ++ fr"LIMIT $lim OFFSET $offset").query
}

object DoobiePagination extends DoobiePagination
