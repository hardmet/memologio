package ru.hardmet.memologio

import ru.hardmet.memologio.posts.PostModule
import zio.Has

package object http {
  type HttpClient    = Has[HttpClient.Service]
  type ResourceCache = Has[ResourceCache.Service]
  type ReqInfo       = Has[RequestInfo]

  val modules: List[HttpModule] = List(PostModule)

}
