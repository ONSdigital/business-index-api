package models

import play.api.libs.json._
import BusinessFields._
import play.api.libs.functional.syntax._

import scala.util.Try

case class Business(
    id: Long,
    businessName: String,
    uprn: Option[Long],
    postCode: Option[String],
    industryCode: Option[String],
    legalStatus: Option[String],
    tradingStatus: Option[String],
    turnover: Option[String],
    employmentBands: Option[String],
    vatRefs: Option[Seq[String]],
    payeRefs: Option[Seq[String]],
    companyNo: Option[String]
) {
  // method that used as output on UI (some fields are hidden)
  def secured: Business = this.copy(vatRefs = None, payeRefs = None, uprn = None)
}

object Business {

  def mapStrOption(opt: Option[String]): Option[String] = opt match {
    case Some(str) if (!str.trim.isEmpty) => opt
    case _ => None
  }

  implicit val businessReads = (
    (JsPath \ "id").read[Long] and
    (JsPath \ "BusinessName").read[String] and
    (JsPath \ "UPRN").readNullable[Long] and
    (JsPath \ "PostCode").readNullable[String] and
    (JsPath \ "IndustryCode").readNullable[String] and
    (JsPath \ "LegalStatus").readNullable[String] and
    (JsPath \ "TradingStatus").readNullable[String] and
    (JsPath \ "Turnover").readNullable[String] and
    (JsPath \ "EmploymentBands").readNullable[String] and
    (JsPath \ "VatRefs").readNullable[Seq[String]] and
    (JsPath \ "PayeRefs").readNullable[Seq[String]] and
    (JsPath \ "CompanyNo").readNullable[String]
  )((id, businessName, uprn, postcode, industryCode, legalstatus, tradingstatus, turnover, employmentbands, vatrefs, payerefs, companyno) =>
      Business.apply(id, businessName, uprn, mapStrOption(postcode), mapStrOption(industryCode),
        mapStrOption(legalstatus), mapStrOption(tradingstatus), mapStrOption(turnover), mapStrOption(employmentbands),
        vatrefs,
        payerefs,
        mapStrOption(companyno)))

  implicit val biWrites = new Writes[Business] { //writes use only for hbase caching
    override def writes(b: Business): JsValue = {
      import b._
      JsObject(Seq(
        "id" -> Json.toJson(id),
        "BusinessName" -> Json.toJson(businessName),
        "UPRN" -> Json.toJson(uprn),
        "PostCode" -> Json.toJson(postCode.getOrElse("")),
        "IndustryCode" -> Json.toJson(industryCode.getOrElse("")),
        "LegalStatus" -> Json.toJson(legalStatus.getOrElse("")),
        "TradingStatus" -> Json.toJson(tradingStatus.getOrElse("")),
        "Turnover" -> Json.toJson(turnover.getOrElse("")),
        "EmploymentBands" -> Json.toJson(employmentBands.getOrElse("")),
        vatRefs.map(v => ("VatRefs" -> Json.toJson(v.filterNot(_.trim.isEmpty)))).orNull,
        payeRefs.map(pr => ("PayeRefs" -> Json.toJson(pr.filterNot(_.trim.isEmpty)))).orNull,
        "CompanyNo" -> Json.toJson(companyNo.getOrElse(""))
      ).filter(_ != null))
    }
  }

  private[this] def industryCodeNormalize(s: Option[String]) = s match {
    case None | Some("") | Some("0") => None
    case Some(v) if v.length < 5 => Some("0" * (5 - v.length) + v)
    case Some(v) => Some(v)
  }

  // build business index from elastic search map of fields
  def fromMap(id: Long, map: Map[String, Any]) = Business(
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
      case a: Seq[Any] => a.map(x => x.toString)
    },
    payeRefs = map.get(cBiPayeRefs).map {
      case a: Seq[Any] => a.map(x => x.toString)
    },
    companyNo = map.get(cBiCompanyNo).map(_.toString)
  )

  def fromCSVMap(id: Long, map: Map[String, String]) = Business(
    id = id,
    businessName = map.getOrElse(cBiName, cEmptyStr),
    uprn = Try(map.get(cBiUprn).map(x => x.toLong)).toOption.flatten,
    postCode = map.get(cBiPostCode),
    industryCode = industryCodeNormalize(map.get(cBiIndustryCode)),
    legalStatus = map.get(cBiLegalStatus),
    tradingStatus = map.get(cBiTradingStatus),
    turnover = map.get(cBiTurnover),
    employmentBands = map.get(cBiEmploymentBand),
    vatRefs = map.get(cBiVatRefs).map(Seq(_)),
    payeRefs = map.get(cBiPayeRefs).map(Seq(_)),
    companyNo = map.get(cBiCompanyNo)
  )

  def toMap(b: Business): Map[String, Any] = Map(
    cBiName -> b.businessName.toUpperCase,
    cBiUprn -> b.uprn.orNull,
    cBiPostCode -> b.postCode.orNull,
    cBiIndustryCode -> b.industryCode.orNull,
    cBiLegalStatus -> b.legalStatus.orNull,
    cBiTradingStatus -> b.tradingStatus.orNull,
    cBiTurnover -> b.turnover.orNull,
    cBiEmploymentBand -> b.employmentBands.orNull,
    cBiVatRefs -> b.vatRefs.orNull,
    cBiPayeRefs -> b.payeRefs.orNull,
    cBiCompanyNo -> b.companyNo.orNull
  )
}

object IndexConsts {
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
  val cBiAnalyzer = "bi-analyzer"
  val cBiNameBoost = 4
}