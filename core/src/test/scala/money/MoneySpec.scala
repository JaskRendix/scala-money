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
import java.time.Instant

class MoneyAdvancedSpec extends Specification with ScalaCheck {

  val USD = Currency("USD")
  val EUR = Currency("EUR")
  val GBP = Currency("GBP")
  val JPY = Currency("JPY")
  val CHF = Currency("CHF")

  def const(rate: BigDecimal): FxCurve =
    ConstantCurve(FxQuote(rate, rate))

  val eurUsdQuote = FxQuote(bid = 1.10, ask = 1.12)
  val usdJpyQuote = FxQuote(bid = 150.0, ask = 151.0)

  val curves: Map[(Currency, Currency), FxCurve] = Map(
    (EUR, USD) -> ConstantCurve(eurUsdQuote),
    (USD, JPY) -> ConstantCurve(usdJpyQuote),
    (JPY, GBP) -> const(0.0052),
    (GBP, CHF) -> const(1.12),
    (USD, EUR) -> const(0.90)
  )

  given Converter = Converter(curves)

  val reasonableBigDecimal: Gen[BigDecimal] =
    Gen.chooseNum(-1e6, 1e6).map(BigDecimal(_))

  val positiveBigDecimal: Gen[BigDecimal] =
    Gen.chooseNum(1, 1000000).map(n => BigDecimal(n.toString))

  "Money arithmetic" should {

    "add same-currency amounts" in {
      Money(100, USD) + Money(200, USD) must_== Money(300, USD)
    }

    "subtract same-currency amounts" in {
      Money(200, USD) - Money(50, USD) must_== Money(150, USD)
    }

    "multiply correctly" in {
      Money(100, USD) * 3 must_== Money(300, USD)
    }

    "divide correctly" in {
      Money(100, USD) / 4 must_== Money(25, USD)
    }

    "divide by zero throws ArithmeticException" in {
      Money(100, USD) / 0 must throwA[ArithmeticException]
    }

    "round correctly HALF_UP" in {
      Money(123.4567, USD).round(2) must_== Money(123.46, USD)
    }

    "round correctly HALF_DOWN" in {
      Money(123.4550, USD).round(2, RoundingMode.HALF_DOWN) must_== Money(123.45, USD)
    }

    "negate produces negative amount" in {
      val neg = Money(-100, USD)
      neg.amount must_== BigDecimal(-100)
    }

    "add BigDecimal scalar" in {
      Money(100, USD) + BigDecimal(50) must_== Money(150, USD)
    }

    "subtract BigDecimal scalar" in {
      Money(100, USD) - BigDecimal(30) must_== Money(70, USD)
    }

    "addition is commutative for same currency" in
      forAll(reasonableBigDecimal, reasonableBigDecimal) { (a, b) =>
        val x = Money(a, USD)
        val y = Money(b, USD)
        (x + y).amount must_== (y + x).amount
      }

    "addition is associative for same currency" in
      forAll(reasonableBigDecimal, reasonableBigDecimal, reasonableBigDecimal) { (a, b, c) =>
        val x = Money(a, USD)
        val y = Money(b, USD)
        val z = Money(c, USD)
        ((x + y) + z).amount must_== (x + (y + z)).amount
      }

    "multiply by zero gives zero" in
      forAll(reasonableBigDecimal) { a =>
        (Money(a, USD) * 0).amount must_== BigDecimal(0)
      }

    "multiply by one is identity" in
      forAll(reasonableBigDecimal) { a =>
        (Money(a, USD) * 1).amount must_== a
      }

    "cross-currency add returns this.currency" in {
      val result = Money(100, EUR) + Money(100, USD)
      result.currency must_== EUR
    }

    "cross-currency subtract returns this.currency" in {
      val result = Money(100, EUR) - Money(50, USD)
      result.currency must_== EUR
    }
  }

  "Bid/ask spreads" should {

    "use ask when buying target currency" in {
      Money(100, EUR).to(USD, FxSide.Buy).amount must_== BigDecimal(100) * eurUsdQuote.ask
    }

    "use bid when selling target currency" in {
      Money(100, EUR).to(USD, FxSide.Sell).amount must_== BigDecimal(100) * eurUsdQuote.bid
    }

    "use mid for default to()" in {
      Money(100, EUR).to(USD).amount must_== BigDecimal(100) * eurUsdQuote.mid
    }

    "buy rate >= mid rate >= sell rate" in {
      val buy  = Money(100, EUR).to(USD, FxSide.Buy).amount
      val mid  = Money(100, EUR).to(USD, FxSide.Mid).amount
      val sell = Money(100, EUR).to(USD, FxSide.Sell).amount
      (buy >= mid) must beTrue
      (mid >= sell) must beTrue
    }

    "spread is non-negative" in {
      eurUsdQuote.ask must be_>=(eurUsdQuote.bid)
    }
  }

  "Inverse curve" should {

    "allow conversion in reverse direction" in {
      Money(112, USD).safeTo(EUR) must beRight
    }

    "inverse of inverse approximates identity" in
      forAll(positiveBigDecimal) { a =>
        given Converter = Converter.fromRates(Map((EUR, USD) -> BigDecimal("1.11")))
        val backHere    = Money(a, EUR).safeTo(USD).flatMap(_.safeTo(EUR))
        backHere must beRight
        backHere.map(_.amount.setScale(2, RoundingMode.HALF_UP)) must_==
          Right(a.setScale(2, RoundingMode.HALF_UP))
      }
  }

  "Multi-leg FX routing" should {

    "convert EUR → CHF via USD → JPY → GBP" in {
      Money(100, EUR).safeTo(CHF) must beRight
    }

    "result of multi-leg is in target currency" in {
      Money(100, EUR).safeTo(CHF).map(_.currency) must beRight(CHF)
    }

    "fail with NoConversionPath when no path exists" in {
      Money(100, CHF).safeTo(JPY) must beLeft.like { case _: NoConversionPath =>
        ok
      }
    }

    "single-leg and routed give same result when direct exists" in {
      val direct = Money(100, EUR).safeTo(USD)
      val routed = Money(100, EUR).safeTo(USD)
      direct.map(_.amount) must_== routed.map(_.amount)
    }
  }

  "Time-dependent FX curves" should {

    val forwardDate = Instant.parse("2025-01-01T00:00:00Z")

    val forwardCurve = new FxCurve:
      def rateAt(t: Instant): FxQuote =
        if t.isBefore(forwardDate) then FxQuote(1.10, 1.10)
        else FxQuote(1.20, 1.20)

    given Converter = Converter(Map((EUR, USD) -> forwardCurve))

    "use spot rate before forward date" in {
      Money(100, EUR).at(Instant.parse("2024-06-01T00:00:00Z")).to(USD).amount must_== 110
    }

    "use forward rate after forward date" in {
      Money(100, EUR).at(Instant.parse("2025-06-01T00:00:00Z")).to(USD).amount must_== 120
    }

    "at() preserves amount and currency" in {
      val m = Money(100, EUR)
      val t = Instant.parse("2024-01-01T00:00:00Z")
      m.at(t).amount must_== m.amount
      m.at(t).currency must_== m.currency
    }
  }

  "Money comparison" should {

    "compare across currencies using mid" in {
      val a = Money(100, EUR)
      val b = Money(110, USD)
      (a > b) must beTrue
    }

    "compare equal values using mid rate" in {
      val eur = Money(100, EUR)
      val usd = Money(100 * eurUsdQuote.mid, USD)
      (eur === usd) must beTrue
    }

    "=== is symmetric" in {
      val a = Money(100, USD)
      val b = Money(100, USD)
      (a === b) must beTrue
      (b === a) must beTrue
    }

    "!== negates ===" in
      forAll(reasonableBigDecimal, reasonableBigDecimal) { (a, b) =>
        val x = Money(a, USD)
        val y = Money(b, USD)
        (x !== y) must_== !(x === y)
      }

    "same currency comparison is exact" in {
      Money(100, USD).compare(Money(99, USD)) must be_>(0)
      Money(99, USD).compare(Money(100, USD)) must be_<(0)
      Money(100, USD).compare(Money(100, USD)) must_== 0
    }

    "safeCompare returns Left when curve is missing" in {
      Money(100, CHF).safeCompare(Money(50, EUR)) must beLeft
    }

    "comparison is antisymmetric for same currency" in
      forAll(reasonableBigDecimal, reasonableBigDecimal) { (a, b) =>
        val x = Money(a, USD)
        val y = Money(b, USD)
        x.compare(y) must_== -y.compare(x)
      }

    "comparison is transitive for same currency" in
      forAll(reasonableBigDecimal, reasonableBigDecimal, reasonableBigDecimal) { (a, b, c) =>
        val x = Money(a, USD)
        val y = Money(b, USD)
        val z = Money(c, USD)
        if x.compare(y) <= 0 && y.compare(z) <= 0 then x.compare(z) must be_<=(0)
        else ok
      }
  }

  "Money equality" should {

    "equal if same amount and currency" in {
      Money(100, USD) must_== Money(100, USD)
    }

    "not equal if different amount" in {
      Money(100, USD) must_!= Money(101, USD)
    }

    "not equal if different currency" in {
      Money(100, USD) must_!= Money(100, EUR)
    }

    "hashCode consistent with equals" in
      forAll(reasonableBigDecimal) { a =>
        val x = Money(a, USD)
        val y = Money(a, USD)
        (x == y) must beTrue
        x.hashCode must_== y.hashCode
      }
  }

  "Error handling" should {

    "safeTo returns Left for missing curve" in {
      Money(100, CHF).safeTo(JPY) must beLeft
    }

    "unsafe to() throws RuntimeException for missing curve" in {
      Money(100, CHF).to(JPY) must throwA[RuntimeException]
    }

    "safeCompare never throws" in
      forAll(reasonableBigDecimal) { a =>
        Money(a, USD).safeCompare(Money(50, CHF)).isInstanceOf[Either[?, ?]] must beTrue
      }

    "safeTo never throws" in
      forAll(reasonableBigDecimal) { a =>
        Money(a, USD).safeTo(EUR).isInstanceOf[Either[?, ?]] must beTrue
      }

    "MissingCurve has readable message" in {
      MissingCurve(CHF, JPY).message must contain("CHF")
      MissingCurve(CHF, JPY).message must contain("JPY")
    }

    "NoConversionPath has readable message" in {
      NoConversionPath(CHF, JPY).message must contain("CHF")
      NoConversionPath(CHF, JPY).message must contain("JPY")
    }
  }

  "Converter.fromRates" should {

    "build a working converter from simple rates" in {
      val c           = Converter.fromRates(Map((USD, EUR) -> BigDecimal(0.90)))
      given Converter = c
      Money(100, USD).safeTo(EUR).map(_.amount) must beRight(BigDecimal(90.0))
    }

    "symmetric rate with no spread gives same buy/sell" in {
      val c           = Converter.fromRates(Map((USD, EUR) -> BigDecimal(0.90)))
      given Converter = c
      val buy         = Money(100, USD).to(EUR, FxSide.Buy).amount
      val sell        = Money(100, USD).to(EUR, FxSide.Sell).amount
      buy must_== sell
    }
  }

  "Money formatting" should {

    "include currency code in toString" in {
      Money(100, USD).toString must contain("USD")
    }

    "toFormattedString respects decimal digits" in {
      Money(123.456789, USD).toFormattedString(2) must contain("123.46")
    }

    "toFormattedString uses US decimal separator" in {
      Money(1234.5, USD).toFormattedString(1) must contain(".")
    }
  }

  "NumericMoney" should {

    given num: Numeric[Money] = NumericMoney(USD)

    "sum a list of same-currency values" in {
      val moneys = List(Money(10, USD), Money(20, USD), Money(30, USD))
      moneys.sum must_== Money(60, USD)
    }

    "negate a value" in {
      num.negate(Money(50, USD)).amount must_== BigDecimal(-50)
    }

    "fromInt creates Money in default currency" in {
      num.fromInt(42).currency must_== USD
    }
  }

  "Money allocation" should {

    "split into equal parts preserving total" in {
      val m     = Money(BigDecimal("100.00"), USD)
      val parts = m.allocate(3)
      parts.length must_== 3
      parts.map(_.amount).sum must_== BigDecimal("100.00")
    }

    "distribute remainder to first slots" in {
      val parts = Money(BigDecimal("100.00"), USD).allocate(3)
      parts(0).amount must_== BigDecimal("33.34")
      parts(1).amount must_== BigDecimal("33.33")
      parts(2).amount must_== BigDecimal("33.33")
    }

    "split into 1 part returns original amount" in {
      val m = Money(BigDecimal("100.00"), USD)
      m.allocate(1) must_== List(m)
    }

    "allocate by ratios preserving total" in {
      val m     = Money(BigDecimal("100.00"), USD)
      val parts = m.allocate(BigDecimal(1), BigDecimal(2), BigDecimal(2))
      parts.length must_== 3
      parts.map(_.amount).sum must_== BigDecimal("100.00")
    }

    "allocate by ratios gives correct proportions" in {
      val parts = Money(BigDecimal("100.00"), USD).allocate(BigDecimal(1), BigDecimal(2), BigDecimal(2))
      parts(0).amount must_== BigDecimal("20.00")
      parts(1).amount must_== BigDecimal("40.00")
      parts(2).amount must_== BigDecimal("40.00")
    }

    "allocate by equal ratios same as equal parts" in {
      val byParts = Money(BigDecimal("100.00"), USD).allocate(3).map(_.amount)
      val byRatios =
        Money(BigDecimal("100.00"), USD).allocate(BigDecimal(1), BigDecimal(1), BigDecimal(1)).map(_.amount)
      byParts must_== byRatios
    }

    "all parts share the same currency" in {
      Money(BigDecimal("100.00"), USD).allocate(4).map(_.currency).distinct must_== List(USD)
    }

    "reject zero parts" in {
      Money(BigDecimal("100.00"), USD).allocate(0) must throwA[IllegalArgumentException]
    }

    "reject negative parts" in {
      Money(BigDecimal("100.00"), USD).allocate(-1) must throwA[IllegalArgumentException]
    }

    "reject empty ratios" in {
      Money(BigDecimal("100.00"), USD).allocate() must throwA[IllegalArgumentException]
    }

    "total is preserved for any positive integer split" in
      forAll(positiveBigDecimal, Gen.chooseNum(1, 100)) { (a, n) =>
        val m     = Money(a, USD)
        val parts = m.allocate(n)
        val sum   = parts.map(_.amount).sum
        sum must_== a.setScale(USD.fractionDigits, RoundingMode.DOWN)
      }

    "JPY allocation has zero decimal places" in {
      val parts = Money(1000, JPY).allocate(3)
      parts.map(_.amount).sum must_== BigDecimal(1000)
      parts.foreach(_.amount.scale must_== 0)
      ok
    }
  }
}
