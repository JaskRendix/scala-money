/*
 * Copyright 2014 Alessandro Lacava.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package money

import scala.math.BigDecimal.RoundingMode
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.specs2.mutable.Specification
import org.specs2.ScalaCheck

class MoneySpec extends Specification with ScalaCheck {

  val USD = Currency("USD")
  val EUR = Currency("EUR")
  val GBP = Currency("GBP")
  val JPY = Currency("JPY")

  val conversion: Map[(Currency, Currency), BigDecimal] = Map(
    (EUR, USD) -> BigDecimal("1.13"),
    (EUR, GBP) -> BigDecimal("0.71"),
    (USD, EUR) -> BigDecimal("0.88"),
    (USD, GBP) -> BigDecimal("0.63"),
    (GBP, EUR) -> BigDecimal("1.40"),
    (GBP, USD) -> BigDecimal("1.59"),
    (USD, USD) -> BigDecimal("1.0"),
    (EUR, EUR) -> BigDecimal("1.0"),
    (GBP, GBP) -> BigDecimal("1.0")
  )

  val reasonableBigDecimal: Gen[BigDecimal] =
    Gen.chooseNum(-1e9, 1e9).map(BigDecimal(_))

  val converter: Converter = Converter(conversion)
  given Converter          = converter

  "Money" should {

    "add USD correctly" in {
      (Money(100, USD) + Money(200, USD)) must_== Money(300, USD)
    }

    "add across currencies using conversion" in {
      val result = Money(100, USD) + Money(100, EUR)
      result.currency must_== USD
      result.amount must_== BigDecimal(100) + (BigDecimal(100) * 1.13)
    }

    "subtract across currencies using conversion" in {
      val result = Money(200, USD) - Money(100, EUR)
      result.currency must_== USD
      result.amount must_== BigDecimal(200) - (BigDecimal(100) * 1.13)
    }

    "handle precision correctly with rounding" in {
      (Money(100, USD) / 3).round(2) must_== Money(33.33, USD)
    }

    "throw exception on division by zero" in {
      (Money(100, USD) / 0) must throwA[ArithmeticException]
    }

    "handle negative amounts" in {
      (Money(100, USD) - Money(150, USD)) must_== Money(-50, USD)
    }

    "perform multiplication correctly" in {
      (Money(100, USD) * 23) must_== Money(2300, USD)
    }

    "perform division correctly" in {
      (Money(100, USD) / 4) must_== Money(25, USD)
    }

    "handle comparisons across different currencies" in {
      (Money(100, USD) > Money(90, EUR)) must beFalse
      (Money(100, USD) < Money(90, EUR)) must beTrue
    }

    "support numeric operations" in {
      val sum = Money(100, USD) + Money(200, EUR)
      sum.currency must_== USD
    }

    "convert safely using safeTo" in {
      Money(100, USD).safeTo(EUR) must beRight(Money(88, EUR))
    }

    "return Left(MissingRate) when safeTo has no rate" in {
      Money(100, USD).safeTo(JPY) must beLeft(MissingRate(USD, JPY))
    }

    "throw when using unsafe to() with missing rate" in {
      Money(100, USD).to(JPY) must throwA[RuntimeException]
    }

    "compare safely using safeCompare" in {
      Money(100, USD).safeCompare(Money(50, EUR)) must beRight
    }

    "return Left(MissingRate) for safeCompare with missing rate" in {
      Money(100, USD).safeCompare(Money(50, JPY)) must beLeft(MissingRate(JPY, USD))
    }

    "throw when comparing with missing rate" in {
      Money(100, USD).compare(Money(50, JPY)) must throwA[RuntimeException]
    }

    "support identity conversion" in {
      Money(100, USD).to(USD) must_== Money(100, USD)
    }

    "handle very large numbers" in {
      val big = Money(BigDecimal("1000000000000"), USD)
      (big * 2).amount must_== BigDecimal("2000000000000")
    }

    "handle very small decimal values" in {
      val tiny = Money(BigDecimal("0.0000001"), USD)
      (tiny * 2).amount must_== BigDecimal("0.0000002")
    }

    "format correctly using toString" in {
      Money(123.456789, USD).toString must_== "123.45679 USD"
    }

    "round correctly with different rounding modes" in {
      Money(1.2345, USD).round(3, RoundingMode.HALF_UP) must_== Money(1.235, USD)
      Money(1.2345, USD).round(3, RoundingMode.DOWN) must_== Money(1.234, USD)
    }

    "support === and !== operators" in {
      (Money(100, USD) === Money(100, USD)) must beTrue
      (Money(100, USD) !== Money(100, USD)) must beFalse
    }

    "support === across currencies" in {
      val eurEquivalent = Money(BigDecimal(100) / 1.13, EUR)
      (Money(100, USD) === eurEquivalent) must beTrue
    }

    "support !== across currencies" in {
      val eur = Money(50, EUR)
      (Money(100, USD) !== eur) must beTrue
    }

    "fail gracefully when performing operations with missing rate" in {
      val bad = Money(10, JPY)
      (Money(10, USD) + bad) must throwA[RuntimeException]
      (Money(10, USD) - bad) must throwA[RuntimeException]
      (Money(10, USD) * 2) must_== Money(20, USD) // still fine
    }
  }
  "safeTo" should {

    "convert correctly when rate exists" in {
      Money(100, USD).safeTo(EUR) must beRight(Money(88, EUR))
    }

    "return Left(MissingRate) when rate is missing" in {
      Money(100, USD).safeTo(JPY) must beLeft(MissingRate(USD, JPY))
    }

    "support identity conversion" in {
      Money(123.45, USD).safeTo(USD) must beRight(Money(123.45, USD))
    }

    "convert negative amounts correctly" in {
      Money(-50, USD).safeTo(EUR) must beRight(Money(-44, EUR))
    }

    "convert very large amounts correctly" in {
      val big = Money(BigDecimal("1000000000000"), USD)
      big.safeTo(EUR) must beRight(Money(BigDecimal("880000000000"), EUR))
    }
  }
  "safeCompare" should {

    "compare correctly when rate exists" in {
      Money(100, USD).safeCompare(Money(50, EUR)) must beRight.which(_ > 0)
    }

    "return Left(MissingRate) when rate is missing" in {
      Money(100, USD).safeCompare(Money(50, JPY)) must beLeft(MissingRate(JPY, USD))
    }

    "compare equal values across currencies" in {
      val eurEquivalent = Money(BigDecimal(100) / 1.13, EUR)
      Money(100, USD).safeCompare(eurEquivalent) must beRight(0)
    }

    "compare negative values correctly" in {
      Money(-10, USD).safeCompare(Money(-20, USD)) must beRight.which(_ > 0)
    }

    "compare across currencies with ordering preserved" in {
      Money(100, USD).safeCompare(Money(200, EUR)) must beRight.which(_ < 0)
    }
  }

  "Money property-based tests" should {

    "identity conversion preserves amount" in
      forAll(reasonableBigDecimal) { n =>
        Money(n, USD).safeTo(USD) must beRight(Money(n, USD))
      }

    "safeTo never throws" in
      forAll(reasonableBigDecimal) { n =>
        Money(n, USD).safeTo(EUR).isInstanceOf[Either[MoneyError, Money]]
      }

    "safeCompare never throws" in
      forAll(reasonableBigDecimal, reasonableBigDecimal) { (a, b) =>
        Money(a, USD).safeCompare(Money(b, EUR)).isInstanceOf[Either[MoneyError, Int]]
      }

    "comparison is antisymmetric when currencies match" in
      forAll(reasonableBigDecimal, reasonableBigDecimal) { (a, b) =>
        val x = Money(a, USD)
        val y = Money(b, USD)
        x.compare(y) == -y.compare(x)
      }
  }
}
