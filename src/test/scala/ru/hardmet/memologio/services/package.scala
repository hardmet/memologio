package ru.hardmet.memologio

import cats.data.{EitherNec, NonEmptyChain}
import org.scalacheck.{Arbitrary, Gen}
import tofu.logging.Loggable

import java.time.{LocalDate, LocalDateTime}

package object services {

  class TestURI(val value: String)

  case class ValidURI(override val value: String) extends TestURI(value)

  case class InvalidURI(override val value: String) extends TestURI(value)

  case class PostData(url: EitherNec[String, String],
                      published: EitherNec[String, LocalDateTime],
                      likes: EitherNec[String, Int])

  final implicit val arbitraryForLDT: Arbitrary[LocalDateTime] =
    Arbitrary {
      Gen.calendar.map { calendar =>
        LocalDateTime.ofInstant(
          calendar.toInstant,
          calendar.getTimeZone.toZoneId
        )
      }
    }

  final implicit val arbitraryForLD: Arbitrary[LocalDate] =
    Arbitrary {
      Gen.calendar.map { calendar =>
        LocalDateTime.ofInstant(
          calendar.toInstant,
          calendar.getTimeZone.toZoneId
        ).toLocalDate
      }
    }

  final implicit val ValidURIArbitrary: Arbitrary[ValidURI] =
    Arbitrary {
      for {
        protocol <- Gen.oneOf("http", "https", "ftp", "file")
        domain <- Gen.alphaNumStr
        tld <- Gen.oneOf("com", "io")
        path <- Arbitrary.arbitrary[String]
      } yield ValidURI(s"$protocol://$domain.$tld/$path")
    }

  final implicit val InvalidURIArbitrary: Arbitrary[InvalidURI] =
    Arbitrary {
      for {
        invalidChar <- Gen.oneOf("[", "]", "{", "}", "|", "(", ")")
        protocol <- Gen.oneOf("http", "https", "ftp", "file")
        domain <- Gen.alphaNumStr
        tld <- Gen.oneOf("com", "io")
        path <- Arbitrary.arbitrary[String]
      } yield InvalidURI(s"$invalidChar$protocol://$domain.$tld/$path")
    }

  implicit val PostDataArbitrary: Arbitrary[PostData] =
    Arbitrary {
      for {
        validURI <- ValidURIArbitrary.arbitrary
        invalidURI <- InvalidURIArbitrary.arbitrary
        url <- Gen.oneOf(
          Right[NonEmptyChain[String], String](validURI.value),
          Left[NonEmptyChain[String], String](NonEmptyChain(s"invalid url ${invalidURI.value}"))
        )
        published <- Gen.oneOf(
          Right[NonEmptyChain[String], LocalDateTime](LocalDateTime.now()),
          Left[NonEmptyChain[String], LocalDateTime](NonEmptyChain(s"published is empty", "published is wrong"))
        )
        likes <- Gen.oneOf(
          Right[NonEmptyChain[String], Int](42),
          Left[NonEmptyChain[String], Int](NonEmptyChain(s"likes is wrong"))
        )
      } yield PostData(url, published, likes)
    }

  implicit val likesArbitrary: Arbitrary[Int] = Arbitrary {
    for {
      positiveInt <- Gen.size
      negativeInt <- Gen.size.map(x => -x)
      likes <- Gen.oneOf(positiveInt, negativeInt)
    } yield likes
  }

  implicit val unitLoggable: Loggable[Unit] = Loggable[String].contramap(_ => "()")

}
