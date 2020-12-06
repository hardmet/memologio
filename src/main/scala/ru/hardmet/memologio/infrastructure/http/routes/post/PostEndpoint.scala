package ru.hardmet.memologio
package infrastructure
package http
package routes
package post

import java.net.URI
import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter

import util.{DateParser, Parse}
import cats.effect.Sync
import cats.syntax.all._
import domain.posts.Post
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.headers.Location
import PostEndpoint._
import cats.data.EitherNec
import services.PostService

import scala.util.chaining.scalaUtilChainingOps

// TODO write route pagination
// TODO write route for queries with likes param: findWithLikesAbove
// TODO write route for find by ids
class PostEndpoint[F[_]: Sync, PostId](override val service: PostService[F, PostId], responsePattern: DateTimeFormatter)
  extends Endpoint[F] {

  val dateParser: DateParser = DateParser(PublishedDatePattern, PublishedDateTimePattern)

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
    service
      .createOne(payload.url, payload.published, payload.likes)
      .flatMap{ errorsOrCreatePost: EitherNec[String, Post.Existing[PostId]] =>
        errorsOrCreatePost.fold(
          errors => BadRequest(errors.asJson),
          post => Created(response.Post(responsePattern)(post).asJson)
        )
      }


  private def update(id: String)(update: request.Post.Update): F[Response[F]] = ???
//    update.fold(updateURL(id), updatePublished(id), updateLikes(id), updateAllFields(id))

  private def updateURL(id: String)(url: String): F[Response[F]] = ???
//    withIdPrompt(id) { id =>
//      withReadOne(id) { post =>
//        service
//          .updateOne(post.withUpdatedURL(url))
//          .map(response.Post(responsePattern))
//          .map(_.asJson)
//          .flatMap(Ok(_))
//      }
//    }

  private def updatePublished(id: String)(published: String): F[Response[F]] = ???
//    withIdPrompt(id) { id =>
//      withPublishedDateTimePrompt(published.trim) { published =>
//        withReadOne(id) { post =>
//          service
//            .updateOne(post.withUpdatedPublished(published))
//            .map(response.Post(responsePattern))
//            .map(_.asJson)
//            .flatMap(Ok(_))
//        }
//      }
//    }

  private def updateLikes(id: String)(likes: Int): F[Response[F]] = ???
//    withIdPrompt(id) { id =>
//      withLikesPrompt(likes) { likes =>
//        withReadOne(id) { post =>
//          service
//            .updateOne(post.withUpdatedLikes(likes))
//            .map(response.Post(responsePattern))
//            .map(_.asJson)
//            .flatMap(Ok(_))
//        }
//      }
//    }

  private def updateAllFields(id: String)(url: String, published: String, likes: Int): F[Response[F]] = ???
//    (
//      toId(id.trim).toEitherNec,
//      validateURL(url.trim).toEitherNec,
//      nonEmptyCheck(published.trim)("published").flatMap(localDateTimeParser).toEitherNec,
//      parseLikes(likes).toEitherNec
//      )
//      .parTupled
//      .fold(
//        _.asJson.pipe(BadRequest(_)),
//        Function.tupled(updateCheckedPost)
//      )

  private def updateCheckedPost(id: PostId, url: String, published: LocalDateTime, likes: Int): F[Response[F]] = ???
//    withReadOne(id) { post =>
//      service
//        .updateOne(
//          post
//            .withUpdatedURL(url)
//            .withUpdatedPublished(published)
//            .withUpdatedLikes(likes)
//        )
//        .map(response.Post(responsePattern))
//        .map(_.asJson)
//        .flatMap(Ok(_))
//    }

  private def showAll(): F[Response[F]] = ???
//    service.readAll.flatMap { posts =>
//      posts
//        .sortBy(_.published)
//        .reverse
//        .map(response.Post(responsePattern))
//        .asJson
//        .pipe(Ok(_))
//    }

  private def searchById(id: String): F[Response[F]] = ???
//    withIdPrompt(id) { id =>
//      withReadOne(id) { post =>
//        post
//          .pipe(response.Post(responsePattern))
//          .pipe(_.asJson)
//          .pipe(Ok(_))
//      }
//    }

  private def searchByPublished(published: String): F[Response[F]] = ???
//    parsePublished(published.trim)
//      .fold(
//        error => BadRequest(error),
//        dateOrDateTime =>
//          dateOrDateTime.fold(
//            service.readManyByPublishedDate,
//            service.readManyByPublishedDateTime
//          ).flatMap{ posts =>
//            posts
//              .map(response.Post(responsePattern))
//              .asJson
//              .pipe(Ok(_))
//          }
//      )

  private def parsePublished(published: String): Either[String, Either[LocalDate, LocalDateTime]] = ???
//    nonEmptyCheck(published)("published").flatMap(parseNonEmptyDate)

//  private def  parseNonEmptyDate(nonEmptyDate: String): Either[String, Either[LocalDate, LocalDateTime]] =
//    dateParser.parseLocalDateTime(nonEmptyDate)
//      .map(Right.apply)
//      .leftFlatMap(dateTimeParsingError =>
//        dateParser.parseLocalDate(nonEmptyDate)(dateTimeParsingError)
//          .map(localDate => Left(localDate))
//      )

  private def delete(id: String): F[Response[F]] = ???
//    withIdPrompt(id) { id =>
//      withReadOne(id) { post =>
//        service.deleteOne(post) >>
//          NoContent()
//      }
//    }

  private def withIdPrompt(id: String)(onValidId: PostId => F[Response[F]]): F[Response[F]] =
    toId(id).fold(BadRequest(_), onValidId)

  private def toId(userInput: String): Either[String, PostId] = ???
//    nonEmptyCheck(userInput)("uuid")
//      .flatMap { nonEmptyInput =>
//        parse(nonEmptyInput)
//      }

  def validateURL(url: String): Either[String, String] = ???
//    nonEmptyCheck(url)("url").flatMap { url =>
//      eitherCatchNFAndLeftMap{
//        URI.create(url)
//        url
//      }(_ => s"$url does not match the URI format.")
//    }

  private def withPublishedDateTimePrompt(published: String)
                                         (onSuccess: LocalDateTime => F[Response[F]]): F[Response[F]] = ???
//    nonEmptyCheck(published)("published").fold(
//      BadRequest(_),
//      published => localDateTimeParser(published).fold(BadRequest(_), onSuccess)
//    )

  private def withLikesPrompt(likes: Int)(onSuccess: Int => F[Response[F]]): F[Response[F]] = ???
//    parseLikes(likes).fold(BadRequest(_), onSuccess)

  private def parseLikes(likes: Int): Either[String, Int] = ???
//    Right(likes)
//      .filterOrElse(_ >= 0, s"likes value: $likes should be more or equals to zero")

  private def withReadOne(id: PostId)(onFound: Post.Existing[PostId] => F[Response[F]]): F[Response[F]] = ???
//    service
//      .readOneById(id)
//      .flatMap(_.fold(displayNoPostsFoundMessage)(onFound))

  private val displayNoPostsFoundMessage: F[Response[F]] = Ok("No posts found!")

  private def deleteAll(): F[Response[F]] = ???
//    service.deleteAll >> NoContent()

  private def eitherCatchNFAndLeftMap[T](blockCanThrows: => T)(handleError: Throwable => String): Either[String, T] =
    Either
      .catchNonFatal(blockCanThrows)
      .leftMap(handleError)
}

object PostEndpoint {

  private val PublishedDatePattern: String = "yyyy-MM-dd"

  private val PublishedDateTimePattern: String = "yyyy-MM-dd'T'HH:mm:ssZ"

  private val BaseURI: Location = Location(Uri.unsafeFromString("http://localhost:8888/api/posts")) // TODO avoid full url
}
