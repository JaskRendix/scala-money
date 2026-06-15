/*
 * Copyright 2015 Alessandro Lacava.
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

import java.time.Instant
import scala.collection.mutable

/** FX quote with bid/ask spread. */
final case class FxQuote(bid: BigDecimal, ask: BigDecimal):
  def mid: BigDecimal = (bid + ask) / 2

/** Time‑dependent FX curve. */
trait FxCurve:
  def rateAt(t: Instant): FxQuote

/** Default curve: constant quote (no time dependency). */
final case class ConstantCurve(quote: FxQuote) extends FxCurve:
  def rateAt(t: Instant): FxQuote = quote

/** FX conversion side. */
enum FxSide:
  case Buy, Sell

/**
 * Converter supporting:
 *   - direct FX rates
 *   - multi‑leg FX paths
 *   - bid/ask spreads
 *   - time‑dependent FX curves
 */
final case class Converter(
  curves: Map[(Currency, Currency), FxCurve]
):

  def directCurve(
    from: Currency,
    to: Currency
  ): Either[MoneyError, FxCurve] =
    if from == to then Right(ConstantCurve(FxQuote(1, 1)))
    else curves.get((from, to)).toRight(MissingCurve(from, to))

  def directQuote(
    from: Currency,
    to: Currency,
    at: Instant
  ): Either[MoneyError, FxQuote] =
    directCurve(from, to).map(_.rateAt(at))

  def directRate(
    from: Currency,
    to: Currency,
    at: Instant,
    side: FxSide
  ): Either[MoneyError, BigDecimal] =
    directQuote(from, to, at).map { case FxQuote(bid, ask) =>
      side match
        case FxSide.Buy  => ask
        case FxSide.Sell => bid
    }

  /** Outgoing neighbors from a currency. */
  private def neighbors(c: Currency): Iterable[Currency] =
    curves.keys.collect { case (from, to) if from == c => to }

  /**
   * BFS to find a conversion path. Returns the list of edges (from -> to).
   */
  private def findPath(from: Currency, to: Currency): Option[List[(Currency, Currency)]] =
    if from == to then Some(Nil)
    else
      val visited = mutable.Set[Currency](from)
      val queue   = mutable.Queue[(Currency, List[(Currency, Currency)])]()
      queue.enqueue((from, Nil))

      while queue.nonEmpty do
        val (current, path) = queue.dequeue()

        for next <- neighbors(current) do
          if !visited.contains(next) then
            val newPath = path :+ (current -> next)

            if next == to then return Some(newPath) // ✔ allowed: return from method, not closure

            visited += next
            queue.enqueue((next, newPath))

      None

  private def chainRates(
    path: List[(Currency, Currency)],
    at: Instant,
    side: FxSide
  ): Either[MoneyError, BigDecimal] =
    path.foldLeft(Right(BigDecimal(1)): Either[MoneyError, BigDecimal]) { case (acc, (from, to)) =>
      for
        r1 <- acc
        r2 <- directRate(from, to, at, side)
      yield r1 * r2
    }

  /** Conversion rate at time t, using multi‑leg paths if needed. */
  def conversionRateAt(
    from: Currency,
    to: Currency,
    at: Instant,
    side: FxSide
  ): Either[MoneyError, BigDecimal] =
    if from == to then Right(1)
    else
      directRate(from, to, at, side) match
        case ok @ Right(_) => ok
        case Left(_) =>
          findPath(from, to)
            .toRight(NoConversionPath(from, to))
            .flatMap(path => chainRates(path, at, side))

  /** Convert a Money value at time t. */
  def convertAt(
    source: Money,
    target: Currency,
    at: Instant,
    side: FxSide
  ): Either[MoneyError, Money] =
    conversionRateAt(source.currency, target, at, side).map { rate =>
      Money(source.amount * rate, target, at)
    }

  /** Backward‑compatible: convert using mid‑market spot. */
  def convert(source: Money, target: Currency): Either[MoneyError, Money] =
    convertAt(source, target, Instant.now(), FxSide.Buy)

object Converter:
  /** Empty converter — mostly for tests or placeholder usage. */
  given empty: Converter = Converter(Map.empty)
