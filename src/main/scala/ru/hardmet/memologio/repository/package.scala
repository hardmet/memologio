package ru.hardmet.memologio

import zio.Has

package object repository {
  type DB = Has[DB.Service]
}
