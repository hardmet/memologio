package ru.hardmet.memologio
package http
package routes
package post

import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import cats.effect.Sync
import cats.syntax.all._
import domain.posts.Post
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.headers.Location
import services.PostService
import PostEndpoint._

import scala.util.chaining.scalaUtilChainingOps

class PostEndpoint[F[_]: Sync, PostId](override val service: PostService[F, PostId], responsePattern: DateTimeFormatter)
                                      (implicit parse: Parse[String, PostId]) extends Endpoint[F] {

  //format:off
  override def routMapper: PartialFunction[Request[F], F[Response[F]]] = {
    case r @ POST -> Root                    => r.as[request.Post.Create].flatMap(create)

    case r @ PUT -> Root / id                => r.as[request.Post.Update].flatMap(update(id))

    case GET -> Root :? Published(published) => searchByPublished(published)
    case r @ GET -> Root if r.params.isEmpty => showAll()
    case _ @ GET -> Root                     => Status.PermanentRedirect.apply(BaseURI)
    case GET -> Root / id                    => searchById(id)

    case DELETE -> Root                      => deleteAll()
    case DELETE -> Root / id                 => delete(id)
  }
  //format:on

  object Published extends QueryParamDecoderMatcher[String]("published")

  private def create(payload: request.Post.Create): F[Response[F]] =
    (
      parseURL(payload.url.trim).toEitherNec,
      nonEmptyCheck(payload.published.trim)("published").flatMap(toLocalDateTime).toEitherNec,
      parseLikes(payload.likes).toEitherNec
      )
      .parTupled
      .fold(
        _.asJson.pipe(BadRequest(_)),
        Function.tupled(createCheckedPost)
      )

  private def createCheckedPost(url: String, published: LocalDateTime, likes: Int): F[Response[F]] =
    service
      .createOne(Post.Data(url, published, likes))
      .map(response.Post(responsePattern))
      .map(_.asJson)
      .flatMap(Created(_))


  private def update(id: String)(update: request.Post.Update): F[Response[F]] =
    update.fold(updateURL(id), updatePublished(id), updateLikes(id), updateAllFields(id))

  private def updateURL(id: String)(url: String): F[Response[F]] =
    withIdPrompt(id) { id =>
      withReadOne(id) { post =>
        service
          .updateOne(post.withUpdatedURL(url))
          .map(response.Post(responsePattern))
          .map(_.asJson)
          .flatMap(Ok(_))
      }
    }

  private def updatePublished(id: String)(published: String): F[Response[F]] =
    withIdPrompt(id) { id =>
      withPublishedDateTimePrompt(published.trim) { published =>
        withReadOne(id) { post =>
          service
            .updateOne(post.withUpdatedPublished(published))
            .map(response.Post(responsePattern))
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
            .map(response.Post(responsePattern))
            .map(_.asJson)
            .flatMap(Ok(_))
        }
      }
    }

  private def updateAllFields(id: String)(url: String, published: String, likes: Int): F[Response[F]] =
    (
      toId(id.trim).toEitherNec,
      parseURL(url.trim).toEitherNec,
      nonEmptyCheck(published.trim)("published").flatMap(toLocalDateTime).toEitherNec,
      parseLikes(likes).toEitherNec
      )
      .parTupled
      .fold(
        _.asJson.pipe(BadRequest(_)),
        Function.tupled(updateCheckedPost)
      )

  private def updateCheckedPost(id: PostId, url: String, published: LocalDateTime, likes: Int): F[Response[F]] =
    withReadOne(id) { post =>
      service
        .updateOne(
          post
            .withUpdatedURL(url)
            .withUpdatedPublished(published)
            .withUpdatedLikes(likes)
        )
        .map(response.Post(responsePattern))
        .map(_.asJson)
        .flatMap(Ok(_))
    }

  private def showAll(): F[Response[F]] =
    service.readAll.flatMap { posts =>
      posts
        .sortBy(_.published)
        .reverse
        .map(response.Post(responsePattern))
        .asJson
        .pipe(Ok(_))
    }

  private def searchById(id: String): F[Response[F]] =
    withIdPrompt(id) { id =>
      withReadOne(id) { post =>
        post
          .pipe(response.Post(responsePattern))
          .pipe(_.asJson)
          .pipe(Ok(_))
      }
    }

  private def searchByPublished(published: String): F[Response[F]] =
    withPublishedPrompt(published.trim)(
      published => responsePosts(service.readManyByPublishedDate(published)),
      published => responsePosts(service.readManyByPublishedDateTime(published))
    )

  private def responsePosts(getPostsFunc: => F[Vector[Post.Existing[PostId]]]): F[Response[F]] =
    getPostsFunc
      .flatMap { posts =>
        posts
          .map(response.Post(responsePattern))
          .asJson
          .pipe(Ok(_))
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
    nonEmptyCheck(userInput)("uuid")
      .flatMap { nonEmptyInput =>
        parse(nonEmptyInput)
      }

  private def withURLPrompt(url: String)(onSuccess: String => F[Response[F]]): F[Response[F]] = {
    parseURL(url).fold(BadRequest(_), onSuccess)
  }

  def parseURL(url: String): Either[String, String] =
    nonEmptyCheck(url)("url").flatMap { url =>
      eitherCatchNFAndLeftMap{
        URI.create(url)
        url
      }(_ => s"$url does not match the URI format.")
    }

  private def withPublishedPrompt(published: String)
                                 (ifLocalDate: LocalDate => F[Response[F]],
                                  ifLocalDateTime: LocalDateTime => F[Response[F]]): F[Response[F]] =
    nonEmptyCheck(published)("published").fold(BadRequest(_), published =>
      toLocalDateTime(published).fold(
        dateParseError =>
          toLocalDate(published)(s"$dateParseError or does not match the required format $PublishedDatePromptPattern.")
            .fold(BadRequest(_), ifLocalDate),
        ifLocalDateTime
      )
    )

  private def withPublishedDateTimePrompt(published: String)
                                         (onSuccess: LocalDateTime => F[Response[F]]): F[Response[F]] =
    nonEmptyCheck(published)("published").fold(
      BadRequest(_),
      published => toLocalDateTime(published).fold(BadRequest(_), onSuccess)
    )

  private def withLikesPrompt(likes: Int)(onSuccess: Int => F[Response[F]]): F[Response[F]] =
    parseLikes(likes).fold(BadRequest(_), onSuccess)

  private def toLocalDateTime(input: String): Either[String, LocalDateTime] =
    eitherCatchNFAndLeftMap(LocalDateTime.parse(input, PublishedDateTimePromptFormatter))(
      _ => s"$input does not match the required format 'yyyy-MM-ddTHH:mm'"
    )

  private def toLocalDate(input: String)(errorMessage: String = ""): Either[String, LocalDate] =
    eitherCatchNFAndLeftMap(LocalDate.parse(input, DateTimeFormatter.ofPattern(PublishedDatePromptPattern)))(
      _ => s"$errorMessage date: $input does not match the required format $PublishedDatePromptPattern"
    )

  private def parseLikes(likes: Int): Either[String, Int] =
    Right(likes)
      .filterOrElse(_ >= 0, s"likes value: $likes should be more or equals to zero")

  def nonEmptyCheck(s: String)(fieldName: String): Either[String, String] =
    Option(s)
      .toRight(s"input $fieldName can not be null")
      .filterOrElse(!_.isEmpty, s"input $fieldName can not be empty or contains only spaces")

  private def withReadOne(id: PostId)(onFound: Post.Existing[PostId] => F[Response[F]]): F[Response[F]] =
    service
      .readOneById(id)
      .flatMap(_.fold(displayNoPostsFoundMessage)(onFound))

  private val displayNoPostsFoundMessage: F[Response[F]] = Ok("No posts found!")

  private def deleteAll(): F[Response[F]] = service.deleteAll >> NoContent()

  private def eitherCatchNFAndLeftMap[T](blockCanThrows: => T)(handleError: Throwable => String): Either[String, T] =
    Either
      .catchNonFatal(blockCanThrows)
      .leftMap(handleError)
}

object PostEndpoint {
  private val BaseURI: Location = Location(Uri.unsafeFromString("http://localhost:8888/api/posts"))

  private val PublishedDateTimePromptFormatter: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

  private val PublishedDatePromptPattern: String = "yyyy-MM-dd"
}
