package ru.hardmet.memologio

import ru.hardmet.memologio.entities.Entity
import ru.hardmet.memologio.posts.services.PostService
import zio.Has


package object posts {
  type Post = Entity[PostId, PostData]

  type Posts = Has[PostService.Service]
}
