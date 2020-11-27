package ru.hardmet.memologio
package repository
package skunk_interpreter

import skunk.{Codec, Fragment, Query, Void}
import skunk.codec.all.int4
import skunk.implicits.toStringOps

/**
 * Pagination is a convenience to simply add limits and offsets to any query
 */
trait PaginationQuery {
  def limit[A](select: Fragment[Void])(implicit codec: Codec[A]): Query[Int, A] =
    sql"$select LIMIT $int4".query(codec)

  def paginate[A](select: Fragment[Void])(implicit codec: Codec[A]): Query[(Int, Int), A] =
    sql"$select LIMIT $int4 OFFSET $int4".query(codec)
}

object PaginationQuery extends PaginationQuery
