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

  // Constant quotes (no spreads)
  def const(rate: BigDecimal): FxCurve =
    ConstantCurve(FxQuote(rate, rate))

  // Bid/ask asymmetric quotes
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
    Gen.chooseNum(-1e9, 1e9).map(BigDecimal(_))

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

    "round correctly" in {
      Money(123.4567, USD).round(2) must_== Money(123.46, USD)
    }
  }

  "Bid/ask spreads" should {

    "use ask when buying target currency" in {
      val m = Money(100, EUR).to(USD, FxSide.Buy)
      m.amount must_== BigDecimal(100) * eurUsdQuote.ask
    }

    "use bid when selling target currency" in {
      val m = Money(100, EUR).to(USD, FxSide.Sell)
      m.amount must_== BigDecimal(100) * eurUsdQuote.bid
    }

    "produce different results for buy vs sell" in {
      val buy  = Money(100, EUR).to(USD, FxSide.Buy)
      val sell = Money(100, EUR).to(USD, FxSide.Sell)
      buy.amount must be_>(sell.amount)
    }
  }

  "Multi-leg FX routing" should {

    "convert EUR → CHF via USD → JPY → GBP" in {
      val result = Money(100, EUR).safeTo(CHF)
      result must beRight
    }

    "fail when no path exists" in {
      val result = Money(100, CHF).safeTo(JPY) // no CHF→... edges
      result must beLeft
    }
  }

  "Time-dependent FX curves" should {

    val forwardDate = Instant.parse("2025-01-01T00:00:00Z")

    val forwardCurve = new FxCurve:
      def rateAt(t: Instant): FxQuote =
        if t.isBefore(forwardDate) then FxQuote(1.10, 1.10)
        else FxQuote(1.20, 1.20)

    given Converter = Converter(
      Map((EUR, USD) -> forwardCurve)
    )

    "use spot rate before forward date" in {
      val t = Instant.parse("2024-06-01T00:00:00Z")
      Money(100, EUR).at(t).to(USD).amount must_== 110
    }

    "use forward rate after forward date" in {
      val t = Instant.parse("2025-06-01T00:00:00Z")
      Money(100, EUR).at(t).to(USD).amount must_== 120
    }
  }

  "Money comparison" should {

    "compare across currencies using spreads" in {
      val a = Money(100, EUR)
      val b = Money(110, USD)
      // With routing + spreads, 100 EUR → USD = 112, so 112 < 110 is false
      (a < b) must beFalse
    }

    "compare equal values across currencies" in {
      val eur = Money(100, EUR)
      val usd = Money(100 * eurUsdQuote.bid, USD) // 110 USD
      (eur === usd) must beTrue
    }

    "safeCompare returns Left when missing curve" in {
      Money(100, CHF).safeCompare(Money(50, EUR)) must beLeft
    }
  }

  "Error handling" should {

    "safeTo returns Left(MissingCurve) when curve missing" in {
      Money(100, CHF).safeTo(JPY) must beLeft
    }

    "unsafe to() throws when curve missing" in {
      Money(100, CHF).to(JPY) must throwA[RuntimeException]
    }

    "safeCompare never throws" in {
      Money(100, USD).safeCompare(Money(50, CHF)).isInstanceOf[Either[MoneyError, Int]] must beTrue
    }
  }

  "Property-based tests" should {

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
