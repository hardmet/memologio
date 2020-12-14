package ru.hardmet.memologio
package services

import java.net.URI
import java.time.LocalDateTime

import scala.util.Try

class PostValidatorEffectLessSpec extends BaseSpec {

  it should "validate url" in {
    val validator = new PostValidatorEffectLess()

    forAll { (validURI: ValidURI) =>
      val uri = validURI.v
      whenever(uri.trim.nonEmpty && Try(URI.create(uri.trim)).isSuccess) {
        validator.isValidURI(uri) shouldBe Right[String, String](uri)
      }
    }

    forAll { (invalidURL: InvalidURI) =>
      val uri = invalidURL.v
      validator.isValidURI(uri) shouldBe Left[String, String](
        s"uri: '$uri' does not match the URI format."
      )
    }
  }

  it should "validate published" in {
    val validator = new PostValidatorEffectLess()

    forAll { (published : LocalDateTime) =>
      whenever(published.isBefore(LocalDateTime.now())) {
        validator.isValidPublished(published) shouldBe Right(published)
      }
    }

    forAll { (published : LocalDateTime) =>
      whenever(published.isAfter(LocalDateTime.now())) {
        validator.isValidPublished(published) shouldBe Left("Published date can't be after processing time")
      }
    }
  }
}
