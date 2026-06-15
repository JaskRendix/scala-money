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
import money.{toFormattedString => bigDecimalToFormattedString}

/**
 * Represents an amount of money in a specific currency.
 *
 * @param amount
 *   the numeric value
 * @param currency
 *   the associated currency
 * @param timestamp
 *   timestamp for time‑dependent FX (defaults to "now")
 * @param converter
 *   contextual converter used for currency operations
 */
final case class Money(
  amount: BigDecimal,
  currency: Currency,
  timestamp: Instant = Instant.now()
)(using converter: Converter)
    extends Ordered[Money]:

  /** Attach a timestamp to this Money for time‑dependent FX. */
  def at(t: Instant): Money =
    copy(timestamp = t)

  /** Converts this money to another currency using mid‑market spot. */
  def to(target: Currency): Money =
    safeTo(target) match
      case Right(m) => m
      case Left(err) =>
        throw new RuntimeException(err.toString)

  /** Safe version of `to`, returning an Either instead of throwing. */
  def safeTo(target: Currency): Either[MoneyError, Money] =
    converter
      .conversionRateAt(this.currency, target, timestamp, FxSide.Buy)
      .map(rate => Money(this.amount * rate, target, timestamp))

  /** Convert using explicit side (Buy/Sell). */
  def to(target: Currency, side: FxSide): Money =
    converter.convertAt(this, target, timestamp, side) match
      case Right(m) => m
      case Left(err) =>
        throw new RuntimeException(err.toString)

  /** Adds two Money values, converting currencies if needed. */
  def +(that: Money): Money =
    performOperation(that, _ + _)

  /** Adds a raw BigDecimal to this Money. */
  def +(value: BigDecimal): Money =
    this + Money(value, this.currency)

  /** Subtracts two Money values, converting currencies if needed. */
  def -(that: Money): Money =
    performOperation(that, _ - _)

  /** Subtracts a raw BigDecimal from this Money. */
  def -(value: BigDecimal): Money =
    this - Money(value, this.currency)

  /** Multiplies this Money by a numeric value. */
  def *(value: BigDecimal): Money =
    copy(amount = amount * value)

  /** Divides this Money by a numeric value. */
  def /(value: BigDecimal): Money =
    copy(amount = amount / value)

  /** Round this Money to n decimal places (HALF_UP by default). */
  def round(n: Int, mode: RoundingMode = RoundingMode.HALF_UP): Money =
    copy(amount = amount.setScale(n, mode))

  /** Equality comparison after converting currencies. */
  def ===(that: Money): Boolean =
    this.compare(that) == 0

  /** Inequality comparison after converting currencies. */
  def !==(that: Money): Boolean =
    this.compare(that) != 0

  /** Compares two Money values after converting currencies. */
  override def compare(that: Money): Int =
    safeCompare(that) match
      case Right(cmp) => cmp
      case Left(err) =>
        throw new RuntimeException(err.toString)

  /** Safe version of compare, returning Either instead of throwing. */
  def safeCompare(that: Money): Either[MoneyError, Int] =
    converter
      .directRate(this.currency, that.currency, this.timestamp, FxSide.Sell)
      .map { rate =>
        val thisInThat = this.amount * rate
        thisInThat compare that.amount
      }

  /** Pretty formatting helper. */
  def toFormattedString(decimalDigits: Int = 5): String =
    s"${bigDecimalToFormattedString(amount, decimalDigits)} ${currency.toString}"

  override def toString: String =
    toFormattedString()

  override def equals(other: Any): Boolean =
    other match
      case m: Money =>
        this.amount == m.amount &&
        this.currency == m.currency
      case _ => false

  override def hashCode(): Int =
    (amount, currency).hashCode()

  private def performOperation(
    that: Money,
    op: (BigDecimal, BigDecimal) => BigDecimal
  ): Money =
    converter.convertAt(that, this.currency, timestamp, FxSide.Buy) match
      case Right(converted) =>
        copy(amount = op(this.amount, converted.amount))
      case Left(err) =>
        throw new RuntimeException(err.toString)
