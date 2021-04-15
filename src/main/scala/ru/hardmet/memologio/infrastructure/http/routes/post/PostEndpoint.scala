package ru.hardmet.memologio
package infrastructure
package http
package routes
package post

import PostEndpoint._
import cats.data.EitherNec
import cats.effect.Sync
import cats.syntax.flatMap._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.headers.Location
import services.PostService
import services.post.domain.Existing

import java.time.format.DateTimeFormatter
import scala.util.chaining.scalaUtilChainingOps

// TODO write route pagination
// TODO write route for queries with likes param: findWithLikesAbove
// TODO write route for find by ids
class PostEndpoint[F[_] : Sync](override val service: PostService[F],
                                responsePattern: DateTimeFormatter) extends Endpoint[F] {

  //format:off
  override def routMapper: PartialFunction[Request[F], F[Response[F]]] = {
    case r@POST -> Root => r.as[request.Post.Create].flatMap(create)

    case r@PUT -> Root / id => r.as[request.Post.Update].flatMap(update(id))

    case GET -> Root :? Published(published) => searchByPublished(published)
    case r@GET -> Root if r.params.isEmpty => showAll()
    case _@GET -> Root => Status.PermanentRedirect.apply(BaseURI)
    case GET -> Root / id => searchById(id)

    case DELETE -> Root => deleteAll()
    case DELETE -> Root / id => delete(id)
  }
  //format:on

  object Published extends QueryParamDecoderMatcher[String]("published")

  private def create(payload: request.Post.Create): F[Response[F]] =
    service
      .createOne(payload.url, payload.published, payload.likes)
      .flatMap { errorsOrCreatePost: EitherNec[String, Existing] =>
        errorsOrCreatePost.fold(
          errors => BadRequest(errors.asJson),
          post => Created(response.Post(responsePattern)(post).asJson)
        )
      }

  private def update(id: String)(update: request.Post.Update): F[Response[F]] =
    update.fold[F[Response[F]]](
      url => service.updateURL(id)(url).pipe(processUpdateResult),
      published => service.updatePublished(id)(published).pipe(processUpdateResult),
      likes => service.updateLikes(id)(likes).pipe(processUpdateResult),
      (url, published, likes) =>
        service.updateOneAllFields(id)(url, published, likes)
          .pipe(
            result =>
              result.flatMap { errorOrUpdated =>
                errorOrUpdated.fold(
                  errors => BadRequest(errors.asJson),
                  updatedPost =>
                    response.Post(responsePattern)(updatedPost)
                      .asJson
                      .pipe(Ok(_))
                )
              }
          )
    )

  private def processUpdateResult[T](result: F[Either[String, Existing]]): F[Response[F]] =
    result.flatMap { errorOrUpdated =>
      errorOrUpdated.fold(
        BadRequest(_),
        updatedPost =>
          response.Post(responsePattern)(updatedPost)
            .asJson
            .pipe(Ok(_))
      )
    }

  private def showAll(): F[Response[F]] =
    service.readAll.flatMap { posts =>
      posts
        .map(response.Post(responsePattern))
        .asJson
        .pipe(Ok(_))
    }

  private def searchById(id: String): F[Response[F]] =
    service.readOneById(id).flatMap { errorOrPost =>
      errorOrPost.fold(
        BadRequest(_),
        post => Ok(response.Post(responsePattern)(post).asJson)
      )
    }

  private def searchByPublished(published: String): F[Response[F]] =
    service.readManyByPublished(published).flatMap(processSearchResult)

  def processSearchResult(result: Either[String, Vector[Existing]]): F[Response[F]] =
    result.fold(
      BadRequest(_),
      posts =>
        posts.map(response.Post(responsePattern))
          .asJson
          .pipe(Ok(_))
    )

  private def delete(id: String): F[Response[F]] =
    service.deleteOne(id).flatMap(errorOrResult =>
      errorOrResult.fold(
        BadRequest(_),
        _ => NoContent()
      )
    )

  private def deleteAll(): F[Response[F]] =
    service.deleteAll >> NoContent()

  // TODO: remove or use
  private val displayNoPostsFoundMessage: F[Response[F]] = Ok("No posts found!")
}

object PostEndpoint {

  private val BaseURI: Location = Location(Uri.unsafeFromString("http://localhost:8888/api/posts")) // TODO avoid full url
}
