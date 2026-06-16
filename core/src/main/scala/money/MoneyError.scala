package money

sealed trait MoneyError:
  def message: String

final case class MissingCurve(from: Currency, to: Currency) extends MoneyError:
  def message: String = s"No FX curve found for ${from} → ${to}"

final case class NoConversionPath(from: Currency, to: Currency) extends MoneyError:
  def message: String = s"No conversion path found from ${from} to ${to}"

final case class InvalidAmount(reason: String) extends MoneyError:
  def message: String = s"Invalid amount: $reason"

extension [A](e: Either[MoneyError, A]) def orThrow: A = e.fold(err => throw RuntimeException(err.message), identity)
