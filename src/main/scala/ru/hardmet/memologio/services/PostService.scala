package ru.hardmet.memologio.services

import ru.hardmet.memologio.repository.PostRepository

class PostService[F[_], PostId](val repository: PostRepository[F, PostId]) {

}

object PostService {
  def apply[F[_], PostId](postRepository: PostRepository[F, PostId]): PostService[F, PostId] =
    new PostService[F, PostId](postRepository)
}
