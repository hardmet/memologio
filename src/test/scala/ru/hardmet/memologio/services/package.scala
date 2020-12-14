package ru.hardmet.memologio

import org.scalacheck.{Arbitrary, Gen}
import tofu.logging.Loggable

import java.time.LocalDateTime

package object services {

  case class ValidURI(v: String)
  case class InvalidURI(v: String)

  final implicit val arbitraryForLDT: Arbitrary[LocalDateTime] =
    Arbitrary {
      Gen.calendar.map { calendar =>
        LocalDateTime.ofInstant(
          calendar.toInstant,
          calendar.getTimeZone.toZoneId
        )
      }
    }

  final implicit val ValidURIArbitrary: Arbitrary[ValidURI] =
    Arbitrary {
      for {
        protocol <- Gen.oneOf("http", "https", "ftp", "file")
        domain   <- Gen.alphaNumStr
        tld      <- Gen.oneOf("com", "io")
        path     <- Arbitrary.arbitrary[String]
      } yield ValidURI(s"$protocol://$domain.$tld/$path")
    }

  final implicit val InvalidURIArbitrary: Arbitrary[InvalidURI] =
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
