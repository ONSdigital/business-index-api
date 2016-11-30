package models.domain.records

object Hmrc {
  case class VatRecord(
    id: String,
    vat_category: String
  )
}

case class Business(
  charitiesCommission: Option[CharitiesCommissionEntry]
)

