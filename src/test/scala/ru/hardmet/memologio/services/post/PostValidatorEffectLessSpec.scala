package ru.hardmet.memologio
package services
package post

import cats.data.NonEmptyChain
import cats.data.NonEmptyChain._
import cats.syntax.foldable._
import domain.Data

import java.net.URI
import java.time.{LocalDate, LocalDateTime}
import scala.util.Try

class PostValidatorEffectLessSpec extends BaseSpec {

  it should "parallel validate post" in {
    val validator = new PostValidatorEffectLess()

    forAll { (postData: PostData) =>
      val expected = (postData.url, postData.published, postData.likes) match {
        // valid post way
        case (Right(url), Right(published), Right(likes)) =>
          Right[NonEmptyChain[String], Data](Data(url, published, likes))
        // errors way
        case _ =>
          val head +: tail = Seq(postData.url, postData.published, postData.likes)
            .map(_.fold(
              errorsNec => errorsNec.toList,
              _ => List.empty[String])
            )
            .reduce(_ ++ _)
          Left[NonEmptyChain[String], Data](
            NonEmptyChain(head, tail: _*)
          )
      }
      validator.parPostValidation(postData.url, postData.published, postData.likes) shouldBe expected
    }

    forAll { (invalidURL: InvalidURI) =>
      val uri = invalidURL.value
      validator.isValidURI(uri) shouldBe Left[String, String](
        s"uri: '$uri' does not match the URI format."
      )
    }
  }

  it should "validate url" in {
    val validator = new PostValidatorEffectLess()

    forAll { (validURI: ValidURI) =>
      val uri = validURI.value
      whenever(uri.trim.nonEmpty && Try(URI.create(uri.trim)).isSuccess) {
        validator.isValidURI(uri) shouldBe Right[String, String](uri)
      }
    }

    forAll { (invalidURL: InvalidURI) =>
      val uri = invalidURL.value
      validator.isValidURI(uri) shouldBe Left[String, String](
        s"uri: '$uri' does not match the URI format."
      )
    }
  }

  it should "validate published" in {
    val validator = new PostValidatorEffectLess()

    forAll { (published: LocalDateTime) =>
      whenever(published.isBefore(LocalDateTime.now())) {
        validator.isValidPublished(published) shouldBe Right(published)
      }
    }

    forAll { (published: LocalDateTime) =>
      whenever(published.isAfter(LocalDateTime.now())) {
        validator.isValidPublished(published) shouldBe Left("Published date can't be after processing time")
      }
    }
  }

  it should "validate published date" in {
    val validator = new PostValidatorEffectLess()

    forAll { (published: LocalDate) =>
      whenever(published.isBefore(LocalDate.now())) {
        validator.isValidPublishedDate(published) shouldBe Right(published)
      }
    }

    forAll { (published: LocalDate) =>
      whenever(published.isAfter(LocalDate.now())) {
        validator.isValidPublishedDate(published) shouldBe Left("Published date can't be after processing date")
      }
    }
  }

  it should "validate likes" in {
    val validator = new PostValidatorEffectLess()
    forAll { (likes: Int) =>
      whenever(likes >= 0) {
        validator.isValidLikes(likes) shouldBe Right(likes)
      }
    }

    forAll { (likes: Int) =>
      whenever(likes < 0) {
        validator.isValidLikes(likes) shouldBe Left(s"likes: $likes should be more or equals to zero")
      }
    }
  }
}
