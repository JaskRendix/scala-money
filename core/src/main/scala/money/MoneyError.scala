package money

sealed trait MoneyError

final case class MissingCurve(from: Currency, to: Currency) extends MoneyError

final case class NoConversionPath(from: Currency, to: Currency) extends MoneyError
