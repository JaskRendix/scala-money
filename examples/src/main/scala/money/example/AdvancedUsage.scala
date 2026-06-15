package money.example

import money._
import java.time.Instant

object AdvancedUsage:

  val USD = Currency("USD")
  val EUR = Currency("EUR")
  val GBP = Currency("GBP")
  val JPY = Currency("JPY")
  val CHF = Currency("CHF")

  val spot: FxQuote => FxCurve = q => ConstantCurve(q)

  val spotCurves: Map[(Currency, Currency), FxCurve] = Map(
    (EUR, USD) -> spot(FxQuote(1.10, 1.11)),
    (USD, JPY) -> spot(FxQuote(150.0, 150.5)),
    (JPY, GBP) -> spot(FxQuote(0.0052, 0.0053)),
    (GBP, CHF) -> spot(FxQuote(1.12, 1.13)),
    (USD, EUR) -> spot(FxQuote(0.90, 0.91))
  )

  val forwardDate = Instant.parse("2025-01-01T00:00:00Z")

  val forwardCurve: FxCurve = new FxCurve:
    def rateAt(t: Instant): FxQuote =
      if t.isBefore(forwardDate) then FxQuote(1.10, 1.11)
      else FxQuote(1.15, 1.16)

  val curves: Map[(Currency, Currency), FxCurve] =
    spotCurves ++ Map((EUR, USD) -> forwardCurve)

  given Converter = Converter(curves)

  def main(args: Array[String]): Unit = {

    println("=== Advanced Money DSL Example ===")

    val amount = Money(1000, EUR)

    val chfViaGraph = amount.safeTo(CHF)
    println(s"Multi‑leg EUR→CHF: $chfViaGraph")

    val buyChf  = amount.to(CHF, FxSide.Buy)
    val sellChf = amount.to(CHF, FxSide.Sell)

    println(s"Buy CHF (ask):  $buyChf")
    println(s"Sell CHF (bid): $sellChf")

    val tSpot    = Instant.parse("2024-06-01T00:00:00Z")
    val tForward = Instant.parse("2025-06-01T00:00:00Z")

    val usdSpot    = amount.at(tSpot).to(USD)
    val usdForward = amount.at(tForward).to(USD)

    println(s"EUR→USD at spot date:    $usdSpot")
    println(s"EUR→USD at forward date: $usdForward")

    val eur1 = Money(500, EUR).at(tSpot)
    val eur2 = Money(500, EUR).at(tForward)

    val sumInUSD = (eur1 + eur2.to(EUR)).to(USD)
    println(s"Sum of EUR at different times → USD: $sumInUSD")

    val cmp = Money(100, USD).at(tForward) > Money(90, EUR).at(tSpot)
    println(s"100 USD (forward) > 90 EUR (spot)? $cmp")
  }
