package models

import java.util

import com.sksamuel.elastic4s.http.RequestSuccess
import com.sksamuel.elastic4s.http.search.SearchResponse
import play.api.libs.json._
import BIndexConsts._

import scala.collection.JavaConverters._
import play.api.libs.functional.syntax._

/**
 * Created by coolit on 03/05/2018.
 */
/*mapping rules:
* for all non-collection optional fields:
*    obj -> json: None -> "" ("key":"" - empty string value)
*    json -> obj: all entries with empty string values mapped to None
* for optional collection-valued fields
*    obj -> json: None -> None (no entries added to json)
*    json -> obj: empty collection -> Option of empty collection ([] -> Some(Seq.empty))
*    */
case class BusinessIndexRec(
    id: Long, // the same as uprn ?
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
  def secured: BusinessIndexRec = this.copy(vatRefs = None, payeRefs = None, uprn = None)

  def toCsvSecured: String = BusinessIndexRec.toString(List(id, businessName, uprn, industryCode, legalStatus,
    tradingStatus, turnover, employmentBands))

  def toCsv: String = BusinessIndexRec.toString(List(id, businessName, uprn, industryCode, legalStatus,
    tradingStatus, turnover, employmentBands, vatRefs.map(seq => seq.mkString(",")), payeRefs.map(seq => seq.mkString(",")), companyNo))

  def blankFieldsForNameSearch = this.copy(uprn = None, vatRefs = None, payeRefs = None)

}

object BusinessIndexRec {

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
  )((id, businessName, uprn, postcode, industryCode, legalstatus, radingstatus, turnover, employmentbands, vatrefs, payerefs, companyno) =>
      BusinessIndexRec.apply(id, businessName, uprn, mapStrOption(postcode), mapStrOption(industryCode),
        mapStrOption(legalstatus), mapStrOption(radingstatus), mapStrOption(turnover), mapStrOption(employmentbands),
        vatrefs,
        payerefs,
        mapStrOption(companyno)))

  implicit val biWrites = new Writes[BusinessIndexRec] { //writes use only for hbase caching
    override def writes(b: BusinessIndexRec): JsValue = {
      import b._
      //JsObject()
      //      println(s"float ${1L}")
      //      println(s"b is: ${b.vatRefs}")
      //      val vat = vatRefs match {
      //        case Some(s) => s.map(x => x.toString)
      //        case None => Seq()
      //      }
      JsObject(Seq(
        "id" -> Json.toJson(id),
        "businessName" -> Json.toJson(businessName),
        "uprn" -> Json.toJson(uprn),
        "postCode" -> Json.toJson(postCode.getOrElse("")),
        "industryCode" -> Json.toJson(industryCode.getOrElse("")),
        "legalStatus" -> Json.toJson(legalStatus.getOrElse("")),
        "tradingStatus" -> Json.toJson(tradingStatus.getOrElse("")),
        "turnover" -> Json.toJson(turnover.getOrElse("")),
        "employmentBands" -> Json.toJson(employmentBands.getOrElse("")),
        // vatRefs.map(vr => "vatRefs" -> Json.toJson(vr)).orNull,
        //"vatRefs" -> Json.toJson(vatRefs.orNull.map(x => x)),
        vatRefs.map(v => ("vatRefs" -> Json.toJson(v.filterNot(_.trim.isEmpty)))).orNull,
        //vatRefs.map(pr => ("vatRefs" -> Json.toJson(pr.filterNot(_.trim.isEmpty)))).orNull,
        //        payeRefs.map(pr => "payeRefs" -> Json.toJson(pr.filterNot(_.trim.isEmpty))).orNull,
        // "vat" -> Json.toJson(vatRefs.orNull),
        //"vat" -> Json.toJson(vat),
        payeRefs.map(pr => ("payeRefs" -> Json.toJson(pr.filterNot(_.trim.isEmpty)))).orNull,
        "companyNo" -> Json.toJson(companyNo.getOrElse(""))
      ).filter(_ != null))
    }
  }

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
    vatRefs = map.get(cBiPayeRefs).map {
      case e: util.ArrayList[String] => e.asScala
      case ps: Seq[String] => ps
      case e: String => e.split(",").toSeq
    },
    payeRefs = map.get(cBiPayeRefs).map {
      case e: util.ArrayList[String] => e.asScala
      case ps: Seq[String] => ps
      case e: String => e.split(",").toSeq
    },
    companyNo = map.get(cBiCompanyNo).map(_.toString)
  )

  def fromRequestSuccessSearch(resp: RequestSuccess[SearchResponse]): Option[List[BusinessIndexRec]] = {
    println(s"test: ${resp.result.hits.hits.toList}")
    resp.result.hits.hits.toList match {
      case Nil => None
      case xs => Some(xs.map(x => BusinessIndexRec.fromMap(x.id.toLong, x.sourceAsMap).secured))
    }
  }

  def fromRequestSuccessId(resp: RequestSuccess[SearchResponse]): Option[BusinessIndexRec] = {
    resp.result.hits.hits.toList match {
      case Nil => None
      case x :: _ => Some(BusinessIndexRec.fromMap(x.id.toLong, x.sourceAsMap))
    }
  }

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