package ru.hardmet.memologio

import zio.Has

package object config {
  type Config = Has[MemologioConfig]
}
