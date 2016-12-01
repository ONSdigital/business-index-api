package models.domain.records


case class EmployerAddress(
  employer_address_line_1: String,
  employer_address_line_2: String,
  employer_address_line_3: String,
  employer_address_line_4: String,
  employer_address_line_5: String,
  postcode: String
)

case class CommunicationAddress(
  communication_address_line_1: String,
  communication_address_line_2: String,
  communication_address_line_3: String,
  communication_address_line_4: String,
  communication_address_line_5: String,
  communication_postcode: String
)

case class PAYERecord(
  emp_stats_rec_id: String,
  district_number: Int,
  employer_reference: Int,
  employer_name_line_1: Int,
  accounts_office_reference: Int,
  address: EmployerAddress,
  trading_as_name_1: String,
  trading_as_name_2: String,
  communication_name: String,
  communication_address: CommunicationAddress,
  legal_status: String
) {
  def trade_classification_number: String = legal_status
}
