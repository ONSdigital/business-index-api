package uk.gov.ons.bi.models

import java.util

import io.swagger.annotations.ApiModelProperty
import uk.gov.ons.bi.models.BIndexConsts._

import scala.collection.JavaConverters._

case class BusinessIndexRec(
     @ApiModelProperty(value = "A unique business id", dataType = "java.lang.Long", example = "85282744", required = true) id: Long, // the same as uprn ?
     @ApiModelProperty(value = "The Name of the Business or Company", dataType = "String", example = "BI (2018) LIMITED", required = true) businessName: String,
     @ApiModelProperty(value = "A unique property number identifier", dataType = "java.lang.Long", example = "977146940701", required = false) uprn: Option[Long],
     @ApiModelProperty(value = "Business's post code", dataType = "String", example = "SE", required = false) postCode: Option[String],
     @ApiModelProperty(value = "Industry Code representing the industry the business is in", dataType = "String", required = false) industryCode: Option[String],
     @ApiModelProperty(value = "The legal category the business falls within", dataType = "String", example = "1", required = false) legalStatus: Option[String],
     @ApiModelProperty(value = "The operational status of the business", dataType = "String", example = "A", required = false) tradingStatus: Option[String],
     @ApiModelProperty(value = "The turnover of the business", dataType = "String", example = "B", required = false) turnover: Option[String],
     @ApiModelProperty(value = "The number of employees the company employees in bands", dataType = "String", example = "E", required = false) employmentBands: Option[String],
     @ApiModelProperty(value = "The business's VAT Reference Number", required = false) vatRefs: Option[Seq[Long]],
     @ApiModelProperty(value = "The business's PAYE Reference Number", required = false) payeRefs: Option[Seq[String]],
     @ApiModelProperty(value = "The business's Company Number to be identified by Companies House", dataType = "String", example = "", required = false) companyNo: Option[String]
                           ) {

  // method that used as output on UI (some fields are hidden)
  def secured: BusinessIndexRec = this.copy(vatRefs = None, payeRefs = None, uprn = None)


  def toCsvSecured: String = BusinessIndexRec.toString(List(id, businessName, uprn, industryCode, legalStatus,
    tradingStatus, turnover, employmentBands))

  def toCsv: String = BusinessIndexRec.toString(List(id, businessName, uprn, industryCode, legalStatus,
    tradingStatus, turnover, employmentBands, vatRefs.map(seq => seq.mkString(",")), payeRefs.map(seq => seq.mkString(",")), companyNo))

}

object BusinessIndexRec {

  val Delim = ","

  def toString(fields: List[Any]): String = fields.map {
    case Some(a) => s"$a"
    case None => ""
    case z => s"$z"
  }.map(x => s""" "$x" """).mkString(Delim)

  private[this] def industryCodeNormalize(s: Option[String]) = s match {
    case None | Some("") | Some("0") => None
    case Some(v) if v.length < 5 => Some("0" * (5 - v.length) + v)
    case Some(v) => Some(v)
  }

  def normalize(b: BusinessIndexRec): BusinessIndexRec = b.copy(industryCode = industryCodeNormalize(b.industryCode))

  // build business index from elastic search map of fields
  def fromMap(id: Long, map: Map[String, Any]) = BusinessIndexRec(
    id = id,
    businessName = map.getOrElse(cBiName, cEmptyStr).toString,
    uprn = map.get(cBiUprn).map(x => java.lang.Long.parseLong(x.toString)),
    postCode = map.get(cBiPostCode).map(_.toString),
    industryCode = industryCodeNormalize(map.get(cBiIndustryCode).map(_.toString)),
    legalStatus = map.get(cBiLegalStatus).map(_.toString),
    tradingStatus = map.get(cBiTradingStatus).map(_.toString),
    turnover = map.get(cBiTurnover).map(_.toString),
    employmentBands = map.get(cBiEmploymentBand).map(_.toString),
    vatRefs = map.get(cBiVatRefs).map {
      case e: util.ArrayList[Any] =>
        // Any: elastic does not guarantee Long return for Long type. It returns type based on actual value
        e.asScala map {
          case x: Int => x.toLong
          case z: Long => z
        }
      case e: Seq[Long] => e
      case "" => Seq.empty[Long]
      case e: String => e.split(",").map(_.toLong)
    },
    payeRefs = map.get(cBiPayeRefs).map {
      case e: util.ArrayList[String] => e.asScala
      case ps: Seq[String] => ps
      case e: String => e.split(",").toSeq
    },
    companyNo = map.get(cBiCompanyNo).map(_.toString)
  )

  def toMap(bi: BusinessIndexRec): Map[String, Any] = Map(
    cBiName -> bi.businessName.toUpperCase,
    cBiUprn -> bi.uprn.orNull,
    cBiPostCode -> bi.postCode.orNull,
    cBiIndustryCode -> bi.industryCode.orNull,
    cBiLegalStatus -> bi.legalStatus.orNull,
    cBiTradingStatus -> bi.tradingStatus.orNull,
    cBiTurnover -> bi.turnover.orNull,
    cBiEmploymentBand -> bi.employmentBands.orNull,
    cBiVatRefs -> bi.vatRefs.orNull,
    cBiPayeRefs -> bi.payeRefs.orNull,
    cBiCompanyNo -> bi.companyNo.orNull
  )

  val cBiSecuredHeader: String = toString(List("ID", cBiName, cBiUprn, cBiIndustryCode, cBiLegalStatus,
    cBiTradingStatus, cBiTurnover, cBiEmploymentBand))

}

object BIndexConsts {

  val cBiType = "business"
  val cBiName = "BusinessName"
  val cBiNameSuggest = "BusinessName_suggest"
  val cBiUprn = "UPRN"
  val cBiPostCode = "PostCode"
  val cBiIndustryCode = "IndustryCode"
  val cBiLegalStatus = "LegalStatus"
  val cBiTradingStatus = "TradingStatus"
  val cBiTurnover = "Turnover"
  val cBiEmploymentBand = "EmploymentBands"
  val cBiVatRefs = "VatRefs"
  val cBiPayeRefs = "PayeRefs"
  val cBiCompanyNo = "CompanyNo"

  val cEmptyStr = ""

}