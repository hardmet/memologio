package ru.hardmet.memologio.resources

import cats.syntax.semigroupk._
import com.twitter.finagle.http.Response
import ru.hardmet.memologio.MemologioHttp
import ru.hardmet.memologio.http.Resource.{folder, single}

object UI {
  val route: MemologioHttp[Response] =
    single("/", "/ui/index.html") <+>
      single("/favicon.ico", "/ui/favicon.ico") <+>
      folder("/js", "/ui", "text/javascript") <+>
      folder("/css", "/ui")
}
