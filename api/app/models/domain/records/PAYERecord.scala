package models.domain.records

import com.outworkers.phantom.dsl.DateTime

case class Address(
  line_1: String,
  line_2: String,
  line_3: String,
  line_4: String,
  line_5: String,
  postcode: String
)

case class PAYERecord(
  emp_stats_rec_id: String,
  district_number: Int,
  employer_reference: Int,
  accounts_office_reference: Int,
  address: Address,
  trading_as_name_1: String,
  trading_as_name_2: String,
  communication_name: String,
  communication_address: Address,
  legal_status: String,
  scheme_type: String,
  date_commenced: DateTime,
  transfer_in_identifier: String,
  successor_in_identifier: String,
  date_ceased: DateTime,
  transfer_out_identifier: String,
  merger_out_identifier: String,
  succession_out_identifier: String,
  date_of_transfer: DateTime,
  scheme_cancelled_date: DateTime,
  scheme_reopened_date:DateTime
) {
  def trade_classification_number: String = legal_status
}
