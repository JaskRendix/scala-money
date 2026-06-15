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

/**
 * Generic Currency trait
 */
trait Currency {
  def getCode: String
  override def toString: String = getCode
}

/**
 * Currency Factory
 */
object Currency {
  val $   = apply("USD")
  val EUR = apply("EUR")
  val GBP = apply("GBP")

  /**
   * Creates a Currency instance from an ISO 4217 code. Leverages java.util.Currency to support all standard codes.
   */
  def apply(code: String): Currency = {
    val normalizedCode = code.toUpperCase match {
      case "$"   => "USD"
      case "€"   => "EUR"
      case "£"   => "GBP"
      case other => other
    }

    try {
      val javaCurr = JCurrency.getInstance(normalizedCode)
      new Currency {
        val getCode: String = javaCurr.getCurrencyCode
      }
    } catch {
      case e: IllegalArgumentException =>
        throw IllegalArgumentException(s"Unknown or unsupported currency code: $code")
    }
  }
}
