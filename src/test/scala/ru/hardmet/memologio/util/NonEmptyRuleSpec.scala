package ru.hardmet.memologio
package util

import cats.Id

class NonEmptyRuleSpec extends BaseSpec {
  private type F[A] = Id[A]

  it should "non empty should check for emptiness" in {
    val nonEmptyRule: NonEmptyRuleInterpreter[F] = new NonEmptyRuleInterpreter[F]

    forAll { (field: String) =>
      nonEmptyRule.nonEmpty(null)(field) shouldBe Left[String, String](s"input $field can not be null")
    }
    forAll { (field: String) =>
      nonEmptyRule.nonEmpty("")(field) shouldBe Left[String, String](
        s"input $field can not be empty or contains only spaces"
      )
    }
    forAll { (input: String, field: String) =>
      whenever(input.nonEmpty) {
        nonEmptyRule.nonEmpty(input)(field) shouldBe Right[String, String](input)
      }
    }
  }
}
