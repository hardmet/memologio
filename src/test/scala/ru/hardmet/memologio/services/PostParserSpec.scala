package ru.hardmet.memologio
package services

import cats.effect.IO
import post.domain.PostId

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PostParserSpec extends BaseSpec {

  private type F[+A] = IO[A]

  it should "parse id as uuid" in {
    val postParser = PostParser[F]
    forAll { (postId: PostId) =>
      postParser.parseId(postId.value.toString).map {
        parsedId =>
          parsedId shouldBe Right(postId)
      }.unsafeRunSync()
    }

    forAll { (invalidUUID: InvalidUUID) =>
      whenever(invalidUUID.value.trim.isEmpty) {
        postParser.parseId(invalidUUID.value).map {
          parsedId =>
            parsedId shouldBe Left("input postId can not be empty or contains only spaces")
        }.unsafeRunSync()
      }
    }

    forAll { (invalidUUID: InvalidUUID) =>
      whenever(invalidUUID.value.trim.nonEmpty) {
        postParser.parseId(invalidUUID.value).map {
          parsedId =>
            parsedId.isLeft shouldBe true
        }.unsafeRunSync()
      }
    }
  }

  // TODO debug this test on input Message: Right(0001-12-30T23:30:16.059) was not equal to Right(0000-12-30T23:30:16.059
  ignore should "parse published date" in {
    val dateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSS"
    val postParser = PostParser[F]
    forAll { (dateTime: LocalDateTime) =>
      val validDateStr = dateTime.format(DateTimeFormatter.ofPattern(dateTimePattern))
      postParser.parsePublished(validDateStr).map {
        parsedId =>
          parsedId shouldBe Right(dateTime)
      }.unsafeRunSync()
    }

    forAll { (dateTime: LocalDateTime) =>
      val validDateStr = dateTime.format(DateTimeFormatter.ofPattern(dateTimePattern))
      val invalidDateStr = s" 5 $validDateStr     13"
      postParser.parsePublished(invalidDateStr).map {
        parsedId =>
          parsedId shouldBe Left(
            s"input date: ${invalidDateStr.trim} does not match the required format '$dateTimePattern'"
          )
      }.unsafeRunSync()
    }
  }
}
