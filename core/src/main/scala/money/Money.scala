/*
 * Copyright 2014 Alessandro Lacava.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package money

import java.time.Instant
import scala.math.BigDecimal.RoundingMode
import scala.math.BigDecimal.RoundingMode.RoundingMode

/**
 * An amount of money in a specific currency.
 *
 * Arithmetic between different currencies uses `converter` to convert the right-hand operand into `this.currency`
 * before operating. The result currency is always `this.currency`.
 *
 * Comparison uses mid-market rates for symmetry.
 */
final case class Money(
  amount: BigDecimal,
  currency: Currency,
  timestamp: Instant = Instant.now()
)(using val converter: Converter)
    extends Ordered[Money]:

  def at(t: Instant): Money = copy(timestamp = t)

  /** Convert to target currency at mid-market rate. */
  def to(target: Currency): Money               = safeTo(target).orThrow
  def to(target: Currency, side: FxSide): Money = safeTo(target, side).orThrow

  def safeTo(target: Currency, side: FxSide = FxSide.Mid): Either[MoneyError, Money] =
    converter
      .conversionRateAt(currency, target, timestamp, side)
      .map(rate => Money(amount * rate, target, timestamp))

  def +(that: Money): Money       = performOperation(that, _ + _)
  def +(value: BigDecimal): Money = this + Money(value, currency)
  def -(that: Money): Money       = performOperation(that, _ - _)
  def -(value: BigDecimal): Money = this - Money(value, currency)
  def *(value: BigDecimal): Money = copy(amount = amount * value)
  def /(value: BigDecimal): Money =
    if value == 0 then throw ArithmeticException("Division by zero")
    else copy(amount = amount / value)

  def round(n: Int, mode: RoundingMode = RoundingMode.HALF_UP): Money =
    copy(amount = amount.setScale(n, mode))

  def ===(that: Money): Boolean = compare(that) == 0
  def !==(that: Money): Boolean = compare(that) != 0

  /** Consistent ordering: convert both to a canonical direction at mid. */
  override def compare(that: Money): Int =
    safeCompare(that).orThrow

  def safeCompare(that: Money): Either[MoneyError, Int] =
    if this.currency == that.currency then Right(this.amount compare that.amount)
    else
      converter
        .directRate(this.currency, that.currency, this.timestamp, FxSide.Mid)
        .map(rate => (this.amount * rate) compare that.amount)

  def toFormattedString(decimalDigits: Int = 5): String =
    s"${money.toFormattedString(amount, decimalDigits)} ${currency}"

  override def toString: String = toFormattedString()

  override def equals(other: Any): Boolean = other match
    case m: Money => amount == m.amount && currency == m.currency
    case _        => false

  override def hashCode(): Int = (amount, currency).hashCode()

  private def performOperation(
    that: Money,
    op: (BigDecimal, BigDecimal) => BigDecimal
  ): Money =
    converter.convertAt(that, this.currency, this.timestamp, FxSide.Mid) match
      case Right(converted) => copy(amount = op(this.amount, converted.amount))
      case Left(err)        => throw RuntimeException(err.message)
