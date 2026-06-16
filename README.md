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

Here's the direct rundown of what was updated:

- **Currency factory** — Replaced the trait/anonymous-instance approach with a `case class` backed by `java.util.Currency`. Value equality now works correctly; two `Currency("USD")` calls produce equal instances.
- **Scala 3 modernization** — Removed legacy implicits and implicit classes. Everything now uses `given`, `using`, and `extension` so the API is clear and consistent.
- **FX side model** — Added `FxSide.Mid` alongside `Buy` and `Sell`. Default conversions and comparisons use mid-market rates. The old code silently used ask for everything.
- **Inverse curve** — Converters no longer require both directions of a pair. If `EUR→USD` is defined, `USD→EUR` resolves automatically via `InverseCurve`.
- **FX engine** — Added multi-leg routing, bid/ask spreads, and time-dependent curves. Direct lookups power comparison; BFS routing powers conversion.
- **Comparison semantics** — Comparison uses mid-market rates in a consistent direction. The old asymmetric behavior is gone.
- **Error model** — `MissingCurve`, `NoConversionPath`, and `InvalidAmount` replace silent throws. `safeTo` and `safeCompare` never throw. `orThrow` centralizes the unsafe path.
- **Package structure** — Dropped `package object money` inside `package money`. Top-level Scala 3 definitions replace it. The broken `apply` DSL extensions are replaced with `in`.
- **Formatting** — `toFormattedString` now uses explicit `Locale.US`. Decimal separators are stable regardless of JVM locale.
- **Test suite** — 56 examples covering arithmetic laws, spread behavior, inverse curves, multi-leg routing, time-dependent rates, comparison invariants, error messages, formatting, and `NumericMoney`.

The result is a DSL that behaves the same way in every context: conversions route, comparisons use mid, errors are typed, and the API has one clear path for every operation.
