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
 * @param converter
 *   contextual converter used for currency operations
 */
final case class Money(amount: BigDecimal, currency: Currency)(using converter: Converter) extends Ordered[Money]:

  /** Converts this money to another currency. */
  def to(target: Currency): Money =
    converter.conversionRate(currency, target) match
      case Right(rate) =>
        Money(amount * rate, target)
      case Left(err) =>
        throw new RuntimeException(err.toString)

  /** Safe version of `to`, returning an Either instead of throwing. */
  def safeTo(target: Currency): Either[MoneyError, Money] =
    converter.convert(this, target)

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
    performOperation(Money(value, this.currency), _ * _)

  /** Divides this Money by a numeric value. */
  def /(value: BigDecimal): Money =
    performOperation(Money(value, this.currency), _ / _)

  /** Equality comparison after converting currencies. */
  def ===(that: Money): Boolean =
    this.compare(that) == 0

  /** Inequality comparison after converting currencies. */
  def !==(that: Money): Boolean =
    this.compare(that) != 0

  /** Rounds this Money to a given number of decimal digits. */
  def round(decimalDigits: Int, roundingMode: RoundingMode = RoundingMode.HALF_DOWN): Money =
    Money(amount.setScale(decimalDigits, roundingMode), currency)

  override def toString: String =
    toFormattedString()

  /** Compares two Money values after converting currencies. */
  override def compare(that: Money): Int =
    converter.conversionRate(that.currency, this.currency) match
      case Right(rate) =>
        val thatAmount = rate * that.amount
        this.amount compare thatAmount
      case Left(err) =>
        throw new RuntimeException(err.toString)

  /** Safe version of compare, returning Either instead of throwing. */
  def safeCompare(that: Money): Either[MoneyError, Int] =
    converter.conversionRate(that.currency, this.currency).map { rate =>
      val thatAmount = rate * that.amount
      this.amount compare thatAmount
    }

  /** Pretty formatting helper. */
  def toFormattedString(decimalDigits: Int = 5): String =
    s"${bigDecimalToFormattedString(amount, decimalDigits)} ${currency.toString}"

  /** Internal helper for +, -, *, /. */
  private def performOperation(that: Money, op: (BigDecimal, BigDecimal) => BigDecimal): Money =
    converter.convert(that, this.currency) match
      case Right(converted) =>
        Money(op(this.amount, converted.amount), this.currency)
      case Left(err) =>
        throw new RuntimeException(err.toString)
