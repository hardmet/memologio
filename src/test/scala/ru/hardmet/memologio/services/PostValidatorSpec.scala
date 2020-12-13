package ru.hardmet.memologio
package services

import java.net.URI
import java.time.LocalDateTime

import cats._
import org.scalacheck.{Arbitrary, Gen}
import tofu.logging.Loggable

import scala.util.Try
import PostEndpointSpec._

class PostValidatorInterpreterSpec extends BaseSpec {
  private type F[A] = Id[A]

  it should "url should be validated" in {
    val validator: PostValidatorInterpreter[F, Unit] = new PostValidatorInterpreter[F, Unit]()

    forAll { (validURI: ValidURI) =>
      val uri = validURI.v
      whenever(uri.trim.nonEmpty && Try(URI.create(uri.trim)).isSuccess) {
        validator.validateNonEmptyURI(uri) shouldBe Right[String, String](uri)
      }
    }

    forAll { (invalidURL: InvalidURI) =>
      val uri = invalidURL.v
      validator.validateNonEmptyURI(uri) shouldBe Left[String, String](
        s"uri: '$uri' does not match the URI format."
      )
    }
  }
}

object PostEndpointSpec {
  case class ValidURI(v: String)
  case class InvalidURI(v: String)

  final implicit protected val arbitraryForLDT: Arbitrary[LocalDateTime] =
    Arbitrary {
      Gen.calendar.map { calendar =>
        LocalDateTime.ofInstant(
          calendar.toInstant,
          calendar.getTimeZone.toZoneId
        )
      }
    }

  implicit val ValidURIArbitrary: Arbitrary[ValidURI] =
    Arbitrary {
      for {
        protocol <- Gen.oneOf("http", "https", "ftp", "file")
        domain   <- Gen.alphaNumStr
        tld      <- Gen.oneOf("com", "io")
        path     <- Arbitrary.arbitrary[String]
      } yield ValidURI(s"$protocol://$domain.$tld/$path")
    }

  implicit val InvalidURIArbitrary: Arbitrary[InvalidURI] =
    Arbitrary {
      for {
        invalidChar <- Gen.oneOf("[", "]", "{", "}", "|", "(", ")")
        protocol <- Gen.oneOf("http", "https", "ftp", "file")
        domain   <- Gen.alphaNumStr
        tld      <- Gen.oneOf("com", "io")
        path     <- Arbitrary.arbitrary[String]
      } yield InvalidURI(s"$invalidChar$protocol://$domain.$tld/$path")
    }

  implicit val unitLoggable: Loggable[Unit] = Loggable[String].contramap(_ => "()")
}
