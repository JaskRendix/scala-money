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

final case class Converter(rates: Map[(Currency, Currency), BigDecimal]):

  /** Returns the conversion rate between two currencies. */
  def conversionRate(from: Currency, to: Currency): Either[MoneyError, BigDecimal] =
    if from == to then Right(1)
    else
      rates.get((from, to)) match
        case Some(rate) => Right(rate)
        case None       => Left(MissingRate(from, to))

  /** Converts a Money value to a target currency. */
  def convert(source: Money, target: Currency): Either[MoneyError, Money] =
    conversionRate(source.currency, target).map { rate =>
      Money(source.amount * rate, target)
    }

object Converter:
  /** Empty converter — mostly for tests or placeholder usage. */
  given empty: Converter = Converter(Map.empty)
