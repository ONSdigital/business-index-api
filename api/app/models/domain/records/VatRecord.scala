package models.domain.records

import cats.data.ValidatedNel
import models.domain.parsers._
import org.joda.time.DateTime

/**
  * LVO code	1	3
  * VAT registration number	4	12
  *Trade class 	13	17
  *Effective date of registration	18	23
  *VAT 1 date	24	29
  *Deregistration indicator	30	30
  *Deregistration date	31	36
  *VAT 30 date	37	42
  *Insolvency indicator	43	43
  *Insolvency date	44	49
  *Group-Division indicator	50	50
  *Voluntary registration indicator	51	51
  *Intending trader indicator	52	52
  *Status	53	53
  *Part-Exempt indicator	54	54
  *Stagger	55	56
  *Repayment indicator	57	57
  *Total turnover	58	66
  *Trading name  	67	84
  *Full name	85	189
  *Address 1  	190	219
  *Address 2   	220	249
  *Address 3 	250	279
  *Address 4  	280	309
  *Address 5	310	339
  *Postcode 	340	347
  */
case class VatRecord(
  lov_code: String,
  vat_registration_number: String,
  trade_class: String,
  effective_date_of_registration: DateTime,
  vat_1_date: DateTime,
  insolvency_indicator: String,
  insolvency_date: DateTime,
  group_division_indicator: String,
  voluntary_registration_indicator: String,
  intending_trader_indicator: String,
  status: String,
  `part-exempt indicator`: String,
  stagger: Int,
  repayment_indicator: Int,
  total_turnover: Long,
  trading_name: String,
  full_name: String,
  address: Address
)

object VatRecord {
  implicit object VatRecordDelimiter extends OffsetProvider[VatRecord] {
    override def parser: OffsetParser = OffsetParser(
      "LOV code" offset 1 --> 3,
      "VAT registration number" offset 4 --> 12,
      "Trade class" offset 14 --> 17,
      "Effective date of registration" offset 18 --> 23,
      "VAT 1 Date" offset 24 --> 29,
      "Deregistration indicator" offset 30 --> 30,
      "Deregistration date" offset 31 --> 36,
      "VAT 30 Date" offset 37 --> 42,
      "Insolvency indicator" offset 43 --> 43,
      "Insolvency date" offset 44 --> 49,
      "Group-Division indicator" offset 50 --> 50,
      "Voluntary registration indicator" offset 51 --> 51,
      "Intending trader indicator" offset 52 --> 52,
      "Status" offset 53 --> 53,
      "Part-Exempt indicator" offset 54 --> 54,
      "Stagger" offset 56 --> 56,
      "Repayment indicator" offset 57 --> 57,
      "Total turnover" offset 58 --> 66,
      "Trading name" offset 67 --> 84,
      "Full name" offset 85 --> 189,
      "Address 1" offset 190 --> 219,
      "Address 2" offset 220 --> 249,
      "Address 3" offset 250 --> 279,
      "Address 4" offset 280 --> 309,
      "Address 5" offset 310 --> 339,
      "Postcode" offset 340 --> 347
    )
  }

  /*
  implicit object VatRecParser extends BiParser[Map[String, String], VatRecord] {
    def parse(source: Map[String, String]): ValidatedNel[String, VatRecord] = {
      (parse[DateTime](source("effective_date_of_registration")) and
        parse[Long](source("number")) and parse[Int]map VatRecord.apply

        val res: ValidatedNel[String, VatRecord]
      res map {
        case Valid(vatRecord) =>
        case Invalid(listErrors) => listErrors.mkString("\n")
      }

    }
  }

  sparkContext.readFile("blabla").map(line => offsets(line).biparse[Map[String, String], T])
  */
}