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

Let me tell you what this codebase was doing before, and why every one of these changes was not optional.

- **Currency factory** — The old approach created anonymous trait instances. Two `Currency("USD")` calls produced objects that were not equal. Every FX map lookup silently missed. The converter was broken by design. Replaced with a `case class` backed by `java.util.Currency`. Value equality works now. `fractionDigits` comes straight from the JDK — JPY gets 0, KWD gets 3, no guessing.

- **Scala 3 modernization** — Legacy implicits were scattered everywhere with no clear resolution order. Replaced with `given`, `using`, and `extension`. You can read the scope, you can reason about it, and it compiles the same way every time.

- **FX side model** — The old code used ask for everything, including comparisons. That is not mid-market. That is not neutral. Added `FxSide.Mid` as a proper case. Default conversions and comparisons now use it. The numbers mean what they say.

- **Inverse curve** — Previously, if you defined `EUR→USD` and forgot `USD→EUR`, the conversion failed silently at runtime. Now `InverseCurve` handles it automatically. Define one direction, get both.

- **FX engine** — Added multi-leg BFS routing, bid/ask spreads, and time-dependent curves. The engine now finds a path even when no direct quote exists. If there is no path at all, you get a typed error, not a `NullPointerException` at midnight.

- **Comparison semantics** — The old comparison was asymmetric. `a > b` and `b < a` could disagree depending on which direction the quote was defined. Comparison now uses mid-market rates in one consistent direction. It means what it says.

- **Error model** — `MissingCurve`, `NoConversionPath`, `InvalidAmount`, and `AllocationError` are typed, structured, and carry readable messages. `safeTo` and `safeCompare` never throw. `orThrow` exists for the cases where you want the exception, and you have to ask for it explicitly.

- **Allocation** — `allocate(parts)` and `allocate(ratios*)` split money with exact remainder distribution. The total is always preserved to the currency's natural precision. This is the only correct way to split monetary amounts. Anything else loses or creates cents.

- **Package structure** — The package object was nested inside its own package, creating `money.money.*`. Imports broke. Replaced with top-level Scala 3 definitions. The broken `apply` DSL extensions are replaced with `in`, which actually compiles.

- **Formatting** — `DecimalFormat` was using the JVM default locale. On a Swiss system, decimals came out with commas. Explicit `Locale.US` now. The output is stable.

- **Test suite** — 68 examples. Arithmetic laws, spread behavior, inverse curves, multi-leg routing, time-dependent rates, comparison invariants, allocation correctness, error messages, formatting, and `NumericMoney`. If it regresses, you will know exactly where.

The result is a DSL that does what it says. Conversions route, comparisons use mid, allocation preserves totals, errors are typed, and there is one clear path for every operation. That is what this codebase needed to be.
