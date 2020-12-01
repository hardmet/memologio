package ru.hardmet.memologio
package http
package routes
package post

import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import cats.effect.Sync
import cats.syntax.all._
import domain.posts.Post
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import services.PostService

import scala.util.chaining.scalaUtilChainingOps

// TODO: coordinate routes with services
class PostEndpoint[F[_]: Sync, PostId](override val service: PostService[F, PostId], pattern: DateTimeFormatter)
                                      (implicit parse: Parse[String, PostId])
  extends Endpoint[F] {

  override def routMapper: PartialFunction[Request[F], F[Response[F]]] = {
    case r @ POST -> Root =>
      r.as[request.Post.Create].flatMap(create)

    case r @ PUT -> Root / id =>
      r.as[request.Post.Update].flatMap(update(id))

    case GET -> Root :? Published(published) => searchByPublishedDate(published)
    case GET -> Root                   => showAll
    case GET -> Root / id              => searchById(id)

    case DELETE -> Root      => deleteAll
    case DELETE -> Root / id => delete(id)
  }

  object Published extends QueryParamDecoderMatcher[String]("published")

  private def create(payload: request.Post.Create): F[Response[F]] =
    withPublishedPrompt(payload.published) { published =>
      withURLPrompt(payload.url) { url =>
        service
          .createOne(Post.Data(url, published))
          .map(response.Post(pattern))
          .map(_.asJson)
          .flatMap(Created(_))
      }
    }


  private def update(id: String)(update: request.Post.Update): F[Response[F]] =
    update.fold(updateURL(id), updatePublished(id), updateLikes(id), updateAllFields(id))

  private def updateURL(id: String)(url: String): F[Response[F]] =
    withIdPrompt(id) { id =>
      withReadOne(id) { post =>
        service
          .updateOne(post.withUpdatedURL(url))
          .map(response.Post(pattern))
          .map(_.asJson)
          .flatMap(Ok(_))
      }
    }

  private def updatePublished(id: String)(published: String): F[Response[F]] =
    withIdPrompt(id) { id =>
      withPublishedPrompt(published) { published =>
        withReadOne(id) { post =>
          service
            .updateOne(post.withUpdatedPublished(published))
            .map(response.Post(pattern))
            .map(_.asJson)
            .flatMap(Ok(_))
        }
      }
    }

  private def updateLikes(id: String)(likes: Int): F[Response[F]] =
    withIdPrompt(id) { id =>
      withLikesPrompt(likes) { likes =>
        withReadOne(id) { post =>
          service
            .updateOne(post.withUpdatedLikes(likes))
            .map(response.Post(pattern))
            .map(_.asJson)
            .flatMap(Ok(_))
        }
      }
    }

  private def updateAllFields(id: String)(url: String, published: String, likes: Int): F[Response[F]] =
    (
      toId(id).toEitherNec,
      toLocalDateTime(published).toEitherNec,
      parseLikes(likes).toEitherNec
      ).parTupled
      .fold(
        _.asJson.pipe(BadRequest(_)),
        Function.tupled(happyPath(url))
      )

  private def happyPath(url: String)(id: PostId, published: LocalDateTime, likes: Int): F[Response[F]] =
    withReadOne(id) { post =>
      service
        .updateOne(
          post
            .withUpdatedURL(url)
            .withUpdatedPublished(published)
            .withUpdatedLikes(likes)
        )
        .map(response.Post(pattern))
        .map(_.asJson)
        .flatMap(Ok(_))
    }

  private val showAll: F[Response[F]] =
    service.readAll.flatMap { posts =>
      posts
        .sortBy(_.published)
        .reverse
        .map(response.Post(pattern))
        .asJson
        .pipe(Ok(_))
    }

  private def searchById(id: String): F[Response[F]] =
    withIdPrompt(id) { id =>
      withReadOne(id) { post =>
        post
          .pipe(response.Post(pattern))
          .pipe(_.asJson)
          .pipe(Ok(_))
      }
    }

  private def searchByPublishedDate(published: String): F[Response[F]] =
    withPublishedPrompt(published) { published =>
      service
        .readManyByPublishedDate(published.toLocalDate)
        .flatMap { posts =>
          posts
            .map(response.Post(pattern))
            .asJson
            .pipe(Ok(_))
        }
    }

  private def delete(id: String): F[Response[F]] =
    withIdPrompt(id) { id =>
      withReadOne(id) { post =>
        service.deleteOne(post) >>
          NoContent()
      }
    }

  private def withIdPrompt(id: String)(onValidId: PostId => F[Response[F]]): F[Response[F]] =
    toId(id).fold(BadRequest(_), onValidId)

  private def toId(userInput: String): Either[String, PostId] =
    parse(userInput).leftMap(_.getMessage)

  private def withURLPrompt(url: String)(onSuccess: String => F[Response[F]]): F[Response[F]] = {
    val trimUrl = url.trim
    nonEmptyCheck(trimUrl)("url")
      .flatMap{ url =>
        parseURL(url)
      }
      .fold(BadRequest(_), onSuccess)
  }

  def parseURL(url: String): Either[String, String] =
    Either
      .catchNonFatal{
        URI.create(url)
        url
      }
      .leftMap(_ => s"$url does not match the URI format.")

  private def withPublishedPrompt(published: String)(onSuccess: LocalDateTime => F[Response[F]]): F[Response[F]] =
    toLocalDateTime(published).fold(BadRequest(_), onSuccess)

  private def withLikesPrompt(likes: Int)(onSuccess: Int => F[Response[F]]): F[Response[F]] =
    parseLikes(likes).fold(BadRequest(_), onSuccess)

  // TODO trim argument and add non-empty check
  private def toLocalDateTime(input: String): Either[String, LocalDateTime] = {
    Either
      .catchNonFatal(LocalDateTime.parse(input.trim, DateTimeFormatter.ofPattern(PublishedPromptPattern)))
      .leftMap { _ =>
        s"${input.trim} does not match the required format $PublishedPromptPattern."
      }
  }

  private def parseLikes(likes: Int): Either[String, Int] = {
    Either
      .catchNonFatal{
        require(likes >= 0)
        likes
      }
      .leftMap { _ =>
        s"$likes should be more or equals to zero."
      }
  }

  def nonEmptyCheck(s: String)(fieldName: String): Either[String, String] =
    Either
      .catchNonFatal{
        require(!s.isEmpty)
        s
      }
      .leftMap { _ =>
        s"input $fieldName can not be empty or contains only spaces"
      }

  private def withReadOne(id: PostId)(onFound: Post.Existing[PostId] => F[Response[F]]): F[Response[F]] =
    service
      .readOneById(id)
      .flatMap(_.fold(displayNoPostsFoundMessage)(onFound))

  private val displayNoPostsFoundMessage: F[Response[F]] =
    NotFound("No posts found!")

  private val deleteAll: F[Response[F]] =
    service.deleteAll >> NoContent()

  private val PublishedPromptPattern: String = "yyyy-MM-dd HH:mm"
}


