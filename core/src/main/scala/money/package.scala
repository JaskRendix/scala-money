/*
 * Copyright 2014 Alessandro Lacava.
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

import scala.math.BigDecimal.RoundingMode
import scala.math.BigDecimal.RoundingMode.RoundingMode
import java.text.DecimalFormat
import java.time.Instant

package object money {

  /** Default currency used when none is specified. */
  implicit val DEFAULT_CURRENCY: Currency = Currency("USD")

  /** New type alias: FX curves instead of static rates. */
  type Conversion = Map[(Currency, Currency), FxCurve]

  /** Pretty formatting for BigDecimal values. */
  def toFormattedString(
    value: BigDecimal,
    decimalDigits: Int = 5,
    roundingMode: RoundingMode = RoundingMode.HALF_DOWN
  ): String = {
    val lowerBound = 0
    val upperBound = 100
    require(
      decimalDigits >= 0 && decimalDigits <= 100,
      s"decimalDigits valid range is [$lowerBound, $upperBound], both inclusive"
    )

    val df =
      if (decimalDigits == 0)
        new DecimalFormat("0")
      else
        new DecimalFormat("0." + ("#" * decimalDigits))

    df.format(value.setScale(decimalDigits, roundingMode).underlying())
  }

  extension (value: BigDecimal)
    def apply(currency: Currency)(using converter: Converter): Money =
      Money(value, currency)

  extension (value: Int)
    def apply(currency: Currency)(using converter: Converter): Money =
      Money(BigDecimal(value), currency)

  extension (value: Double)
    def apply(currency: Currency)(using converter: Converter): Money =
      Money(BigDecimal(value), currency)

  /** Timestamp syntax: Money(100, USD).at(Instant) */
  extension (m: Money) def at(t: Instant): Money = m.at(t)

  /** Numeric instance for Money. */
  implicit def numericMoney(using converter: Converter): NumericMoney =
    new NumericMoney(DEFAULT_CURRENCY)
}
