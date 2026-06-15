package money

sealed trait MoneyError
case class MissingRate(from: Currency, to: Currency) extends MoneyError
