package ru.hardmet.memologio

import org.scalacheck.ScalacheckShapeless
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, GivenWhenThen}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

trait BaseSpec extends AnyFlatSpec
  with should.Matchers
  with GivenWhenThen
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with ScalaCheckPropertyChecks
  with ScalacheckShapeless {

  final protected type Arbitrary[A] =
    org.scalacheck.Arbitrary[A]

  final protected val Arbitrary =
    org.scalacheck.Arbitrary

  final protected type Assertion =
    org.scalatest.compatible.Assertion

  final protected type Gen[+A] =
    org.scalacheck.Gen[A]

  final protected val Gen =
    org.scalacheck.Gen
}
