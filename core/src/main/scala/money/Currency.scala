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

import java.util.Currency as JCurrency

/** ISO 4217 currency, value-equal by code. */
final case class Currency private (getCode: String):
  override def toString: String = getCode

object Currency:

  lazy val USD: Currency = apply("USD")
  lazy val EUR: Currency = apply("EUR")
  lazy val GBP: Currency = apply("GBP")

  private val symbolMap: Map[String, String] = Map(
    "$" -> "USD",
    "€" -> "EUR",
    "£" -> "GBP",
    "¥" -> "JPY",
    "₩" -> "KRW",
    "₣" -> "CHF"
  )

  def apply(code: String): Currency =
    val normalized = symbolMap.getOrElse(code.toUpperCase, code.toUpperCase)
    try
      val jc = JCurrency.getInstance(normalized)
      new Currency(jc.getCurrencyCode)
    catch
      case e: IllegalArgumentException =>
        throw IllegalArgumentException(s"Unknown or unsupported currency code: '$code'", e)
