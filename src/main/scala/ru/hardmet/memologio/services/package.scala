package ru.hardmet.memologio

import domain.posts.Post

package object services {

  type ValidatedPostUpdate[PostId] = Either[String, Post.Existing[PostId]]
}
