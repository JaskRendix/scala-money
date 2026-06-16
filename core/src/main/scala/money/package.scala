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

import java.text.{DecimalFormat, DecimalFormatSymbols}
import java.time.Instant
import java.util.Locale

type Conversion = Map[(Currency, Currency), FxCurve]

/** Pretty-print a BigDecimal with explicit locale (always US decimal format). */
def toFormattedString(
  value: BigDecimal,
  decimalDigits: Int = 5,
  roundingMode: scala.math.BigDecimal.RoundingMode.RoundingMode = scala.math.BigDecimal.RoundingMode.HALF_DOWN
): String =
  require(
    decimalDigits >= 0 && decimalDigits <= 100,
    s"decimalDigits valid range is [0, 100], both inclusive"
  )
  val symbols = DecimalFormatSymbols(Locale.US)
  val pattern = if decimalDigits == 0 then "0" else "0." + ("#" * decimalDigits)
  val df      = DecimalFormat(pattern, symbols)
  df.format(value.setScale(decimalDigits, roundingMode).underlying())

/** DSL: `100 in USD` or `BigDecimal("99.5") in EUR` */
extension (value: Int)
  def in(currency: Currency)(using Converter): Money =
    Money(BigDecimal(value), currency)

extension (value: Double)
  def in(currency: Currency)(using Converter): Money =
    Money(BigDecimal(value), currency)

extension (value: BigDecimal)
  def in(currency: Currency)(using Converter): Money =
    Money(value, currency)
