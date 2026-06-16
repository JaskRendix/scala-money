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

/**
 * `Numeric[Money]` instance.
 *
 * Notes:
 *   - `times` treats the right-hand Money as a pure scalar (currency ignored). Multiplying two monetary amounts has no
 *     financial meaning; this exists only to satisfy the Numeric contract for use in generic collections.
 *   - `toInt` / `toLong` / `toFloat` / `toDouble` truncate silently — unavoidable given the typeclass contract.
 *   - `parseString` is unimplemented; contribute a "100 USD" parser if needed.
 */
final class NumericMoney(defaultCurrency: Currency)(using Converter) extends Numeric[Money]:

  override def plus(x: Money, y: Money): Money  = x + y
  override def minus(x: Money, y: Money): Money = x - y
  override def negate(x: Money): Money          = x.copy(amount = -x.amount)

  /** Right-hand operand treated as scalar — currency is ignored. */
  override def times(x: Money, y: Money): Money = x * y.amount

  override def fromInt(x: Int): Money                  = Money(BigDecimal(x), defaultCurrency)
  override def toInt(x: Money): Int                    = x.amount.toInt    // truncates
  override def toLong(x: Money): Long                  = x.amount.toLong   // truncates
  override def toFloat(x: Money): Float                = x.amount.toFloat  // truncates
  override def toDouble(x: Money): Double              = x.amount.toDouble // truncates
  override def compare(x: Money, y: Money): Int        = x.compare(y)
  override def parseString(str: String): Option[Money] = None

object NumericMoney:
  def given_Numeric_Money(using c: Converter): Numeric[Money] =
    NumericMoney(Currency.USD)(using c)
