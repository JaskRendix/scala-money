# Scala DSL for Money Operations

A small DSL for doing arithmetic and conversions across currencies. Conversions work automatically as long as a `given Converter` is in scope.
The original repository is hosted on GitHub at: https://github.com/lambdista/money

## Setup

// No published artifact — use the source directly

## Usage Example

```scala
import money._

object Usage {
  val USD = Currency("USD")
  val EUR = Currency("EUR")
  val GBP = Currency("GBP")

  def main(args: Array[String]): Unit = {
    val conversion: Map[(Currency, Currency), BigDecimal] = Map(
      (EUR, USD) -> 1.13,
      (EUR, GBP) -> 0.71,
      (USD, EUR) -> 0.88,
      (USD, GBP) -> 0.63,
      (GBP, EUR) -> 1.40,
      (GBP, USD) -> 1.59
    )

    given converter: Converter = Converter(conversion)

    val result = (Money(100.001, USD) + Money(200, EUR)) to GBP
    println(s"Result: $result")

    val comparison = Money(100, USD) > Money(99, EUR)
    println(s"100 USD > 99 EUR? $comparison")
  }
}
```

## Key Features

- **ISO 4217 Support** — Uses `java.util.Currency` under the hood.
- **Scala 3 Syntax** — Modern `extension` methods and `given/using`.
- **Type‑Safe Conversions** — All cross‑currency math goes through the provided `Converter`.

## License

Apache License 2.0 — © 2014–2026 Alessandro Lacava.

---

Here’s a tightened, updated rewrite of your “What Actually Changed” section — **same tone, same voice, same structure**, but with all the updates from your modernized engine and without fluff or adverbs.

---

## What Actually Changed

I focused on the parts that matter in a money DSL: predictable math, stable FX behavior, and a model that behaves the same way in every pipeline that touches it.

Here’s the direct rundown of what was updated:

- **Currency factory** — Replaced the old case‑object zoo with a single `Currency("USD")` constructor backed by `java.util.Currency`. No drift, no maintenance overhead.
- **Scala 3 modernization** — Removed legacy implicits and implicit classes. Everything now uses `given`, `using`, and `extension` so the API is clear and consistent.
- **Implicit scope stability** — `given Converter` and `DEFAULT_CURRENCY` now sit in stable locations. Conversions no longer fail because an implicit slipped out of scope.
- **FX engine rewrite** — Added multi‑leg routing, bid/ask handling, and time‑dependent curves. Direct lookups power comparison; routing powers conversion. The behavior is explicit and predictable.
- **Comparison semantics** — Comparison now uses direct BID quotes only, matching real FX value checks and removing the old asymmetric behavior.
- **Error model** — Introduced `MissingCurve` and `NoConversionPath` so failures are clear and typed. `safeTo` and `safeCompare` never throw.
- **Test suite expansion** — Added tests for routing, spreads, time‑dependent curves, comparison rules, error handling, and identity properties. The suite now covers the full engine.
- **Example update** — Rewrote the example to show routing, safe operations, comparison, rounding, and error handling. It now reflects the actual API.
- **Formatting cleanup** — Removed formatting traps and made the codebase scalafmt‑friendly.
- **API consistency** — Everything uses `Money(amount, currency)` with no hidden constructors or alternate paths.

The result is a small DSL that behaves the same way in every context: conversions route, comparisons don’t, errors are typed, and the API is stable.
