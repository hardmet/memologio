package ru.hardmet.memologio

import zio.Has

package object db {
  type DB = Has[DB.Service]
}
