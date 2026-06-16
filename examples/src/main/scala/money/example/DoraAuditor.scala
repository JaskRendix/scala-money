package money.example

import money._
import java.time.Instant

/**
 * Immutable audit record for DORA / FCA compliant tracking. Captures the full lifecycle of a conversion attempt.
 */
final case class AuditRecord(
  timestamp: Instant,
  operation: String,
  amountIn: String,
  amountOut: String,
  status: String,
  errorDetail: Option[String] = None
)

/**
 * DORA/FCA‑style auditor that wraps a Money conversion using safeTo. It never throws, never mutates, and always returns
 * a complete audit trail.
 */
object DoraAuditor {

  /**
   * Executes a conversion and produces:
   *   - Some(Money) on success, None on failure
   *   - A complete AuditRecord describing the event
   */
  def executeAndAudit(
    money: Money,
    target: Currency
  )(using converter: Converter): (Option[Money], AuditRecord) = {

    val now = Instant.now()

    money.safeTo(target) match {
      case Right(converted) =>
        val record = AuditRecord(
          timestamp = now,
          operation = "CURRENCY_CONVERSION",
          amountIn = money.toString,
          amountOut = converted.toString,
          status = "SUCCESS"
        )
        (Some(converted), record)

      case Left(error) =>
        val record = AuditRecord(
          timestamp = now,
          operation = "CURRENCY_CONVERSION",
          amountIn = money.toString,
          amountOut = "0.0",
          status = "FAILED",
          errorDetail = Some(error.toString)
        )
        (None, record)
    }
  }
}
