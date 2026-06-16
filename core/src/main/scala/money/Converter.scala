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
  lazy val mid: BigDecimal = (bid + ask) / 2

object FxQuote:
  def symmetric(rate: BigDecimal): FxQuote = FxQuote(rate, rate)

/** Time-dependent FX curve. */
trait FxCurve:
  def rateAt(t: Instant): FxQuote

/** Constant quote — no time dependency. */
final case class ConstantCurve(quote: FxQuote) extends FxCurve:
  def rateAt(t: Instant): FxQuote = quote

object ConstantCurve:
  def apply(rate: BigDecimal): ConstantCurve = ConstantCurve(FxQuote.symmetric(rate))

/** Whether you are buying or selling the base currency. */
enum FxSide:
  case Buy, Sell, Mid

final case class Converter(curves: Map[(Currency, Currency), FxCurve]):

  def directCurve(from: Currency, to: Currency): Either[MoneyError, FxCurve] =
    if from == to then Right(ConstantCurve(FxQuote(1, 1)))
    else
      curves
        .get((from, to))
        .orElse(curves.get((to, from)).map(InverseCurve(_)))
        .toRight(MissingCurve(from, to))

  def directQuote(from: Currency, to: Currency, at: Instant): Either[MoneyError, FxQuote] =
    directCurve(from, to).map(_.rateAt(at))

  def directRate(
    from: Currency,
    to: Currency,
    at: Instant,
    side: FxSide
  ): Either[MoneyError, BigDecimal] =
    directQuote(from, to, at).map:
      case FxQuote(bid, ask) =>
        side match
          case FxSide.Buy  => ask
          case FxSide.Sell => bid
          case FxSide.Mid  => (bid + ask) / 2

  private def neighbors(c: Currency): Iterable[Currency] =
    curves.keys.collect { case (from, to) if from == c => to }

  private def findPath(from: Currency, to: Currency): Option[List[(Currency, Currency)]] =
    if from == to then Some(Nil)
    else
      val visited = mutable.Set[Currency](from)
      val queue   = mutable.Queue[(Currency, List[(Currency, Currency)])]()
      queue.enqueue((from, Nil))
      var result: Option[List[(Currency, Currency)]] = None

      while queue.nonEmpty && result.isEmpty do
        val (current, path) = queue.dequeue()
        for next <- neighbors(current) if !visited.contains(next) do
          val newPath = path :+ (current -> next)
          if next == to then result = Some(newPath)
          else
            visited += next
            queue.enqueue((next, newPath))

      result

  private def chainRates(
    path: List[(Currency, Currency)],
    at: Instant,
    side: FxSide
  ): Either[MoneyError, BigDecimal] =
    path.foldLeft(Right(BigDecimal(1)): Either[MoneyError, BigDecimal]):
      case (acc, (from, to)) =>
        for
          r1 <- acc
          r2 <- directRate(from, to, at, side)
        yield r1 * r2

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
            .flatMap(chainRates(_, at, side))

  private inline def makeMoney(amount: BigDecimal, currency: Currency, at: Instant): Money =
    Money(amount, currency, at)(using this)

  def convertAt(
    source: Money,
    target: Currency,
    at: Instant,
    side: FxSide
  ): Either[MoneyError, Money] =
    conversionRateAt(source.currency, target, at, side).map: rate =>
      makeMoney(source.amount * rate, target, at)

  def convert(source: Money, target: Currency, at: Instant = Instant.now()): Either[MoneyError, Money] =
    convertAt(source, target, at, FxSide.Mid)

/** Wraps a curve and returns the inverse rate (1/rate). */
private final case class InverseCurve(underlying: FxCurve) extends FxCurve:
  def rateAt(t: Instant): FxQuote =
    val q = underlying.rateAt(t)
    FxQuote(1 / q.ask, 1 / q.bid) // inverse flips bid/ask

object Converter:
  /** Convenience: build from simple mid rates (no spread). */
  def fromRates(rates: Map[(Currency, Currency), BigDecimal]): Converter =
    Converter(rates.map { case (pair, rate) => pair -> ConstantCurve(rate) })
