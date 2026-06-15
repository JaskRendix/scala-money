package money.example

import money._

object Usage {
  val USD = Currency("USD")
  val EUR = Currency("EUR")
  val GBP = Currency("GBP")

  def main(args: Array[String]): Unit = {
    val conversion: Map[(Currency, Currency), BigDecimal] = Map(
      (EUR, USD) -> BigDecimal("1.13"),
      (EUR, GBP) -> BigDecimal("0.71"),
      (USD, EUR) -> BigDecimal("0.88"),
      (USD, GBP) -> BigDecimal("0.63"),
      (GBP, EUR) -> BigDecimal("1.40"),
      (GBP, USD) -> BigDecimal("1.59")
    )

    given converter: Converter = Converter(conversion)

    val m1 = Money(100.001, USD)
    val m2 = Money(200, EUR)

    val sumAndConversion1 = (m1 + m2).to(GBP)
    println(s"sumAndConversion1: $sumAndConversion1")

    val m3                = Money(100, USD)
    val m4                = Money(210.4, EUR)
    val sumAndConversion2 = (m3 + m4).to(EUR)
    println(s"sumAndConversion2: $sumAndConversion2")

    val comparison = Money(100, USD) > Money(99, EUR)
    println(s"100 USD > 99 EUR? $comparison")

    println("\n--- Safe conversions ---")
    println(Money(100, USD).safeTo(EUR))             // Right(88 EUR)
    println(Money(100, USD).safeTo(GBP))             // Right(63 GBP)
    println(Money(100, USD).safeTo(Currency("JPY"))) // Left(MissingRate)

    println("\n--- Safe comparisons ---")
    println(Money(100, USD).safeCompare(Money(50, EUR)))
    println(Money(100, USD).safeCompare(Money(50, Currency("JPY"))))

    println("\n--- Rounding ---")
    println(Money(123.456789, USD).round(2)) // 123.46 USD
    println(Money(1.2345, USD).round(3))     // 1.235 USD

    println("\n--- Identity conversion ---")
    println(Money(100, USD).to(USD)) // 100 USD

    println("\n--- Error handling with safeTo ---")
    Money(10, USD).safeTo(Currency("JPY")) match {
      case Right(v)  => println(v)
      case Left(err) => println(s"Conversion failed: $err")
    }
  }
}
