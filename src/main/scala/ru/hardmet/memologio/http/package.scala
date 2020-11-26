package ru.hardmet.memologio

import zio.Has

package object http {
  type HttpClient    = Has[HttpClient.Service]
  type ResourceCache = Has[ResourceCache.Service]
  type ReqInfo       = Has[RequestInfo]

}
