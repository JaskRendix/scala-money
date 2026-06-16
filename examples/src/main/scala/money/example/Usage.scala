package money.example

import money._

object Usage {

  val USD = Currency("USD")
  val EUR = Currency("EUR")
  val GBP = Currency("GBP")

  def main(args: Array[String]): Unit = {

    given converter: Converter = Converter.fromRates(
      Map(
        (EUR, USD) -> BigDecimal("1.13"),
        (EUR, GBP) -> BigDecimal("0.71"),
        (USD, EUR) -> BigDecimal("0.88"),
        (USD, GBP) -> BigDecimal("0.63"),
        (GBP, EUR) -> BigDecimal("1.40"),
        (GBP, USD) -> BigDecimal("1.59")
      )
    )

    println("\n--- Basic arithmetic and conversion ---")
    val m1 = Money(100.001, USD)
    val m2 = Money(200, EUR)
    println(s"sumAndConversion1: ${(m1 + m2).to(GBP)}")

    val m3 = Money(100, USD)
    val m4 = Money(210.4, EUR)
    println(s"sumAndConversion2: ${(m3 + m4).to(EUR)}")

    println(s"100 USD > 99 EUR? ${Money(100, USD) > Money(99, EUR)}")

    println("\n--- Safe conversions ---")
    println(Money(100, USD).safeTo(EUR))
    println(Money(100, USD).safeTo(GBP))
    println(Money(100, USD).safeTo(Currency("JPY"))) // Left(MissingCurve)

    println("\n--- Safe comparisons ---")
    println(Money(100, USD).safeCompare(Money(50, EUR)))
    println(Money(100, USD).safeCompare(Money(50, Currency("JPY"))))

    println("\n--- Rounding ---")
    println(Money(123.456789, USD).round(2))
    println(Money(1.2345, USD).round(3))

    println("\n--- Identity conversion ---")
    println(Money(100, USD).to(USD))

    println("\n--- Error handling ---")
    Money(10, USD).safeTo(Currency("JPY")) match
      case Right(v)  => println(v)
      case Left(err) => println(s"Conversion failed: ${err.message}")

    println("\n--- Allocation ---")
    val shares = Money(100, USD).allocate(3)
    shares.foreach(println)
    println(s"Total: ${shares.map(_.amount).sum}")

    val byRatio = Money(100, USD).allocate(BigDecimal(1), BigDecimal(2), BigDecimal(2))
    byRatio.foreach(println)
    println(s"Total: ${byRatio.map(_.amount).sum}")

    println("\n--- DORA audit (success) ---")
    val (converted, auditOk) = DoraAuditor.executeAndAudit(Money(250, EUR), GBP)
    println(s"Converted: $converted")
    println(s"Audit:     $auditOk")

    println("\n--- DORA audit (failure) ---")
    val (failed, auditFail) = DoraAuditor.executeAndAudit(Money(250, EUR), Currency("JPY"))
    println(s"Converted: $failed")
    println(s"Audit:     $auditFail")
  }
}
