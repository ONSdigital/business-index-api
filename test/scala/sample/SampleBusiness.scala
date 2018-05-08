package scala.sample

import models.Business

/**
 * Created by coolit on 08/05/2018.
 */
trait SampleBusiness {
  val SampleBusinessId = 10205415L
  val SampleBusinessName = "TEST GRILL LTD"
  val SampleUPRN = Some(380268L)
  val SamplePostCode = Some("ID80 5QB")
  val SampleIndustryCode = Some("86762")
  val SampleLegalStatus = Some("2")
  val SampleTradingStatus = Some("A")
  val SampleTurnover = Some("A")
  val SampleEmploymentBands = Some("B")
  val SampleVatRefs = Some(Seq("105463"))
  val SamplePayeRefs = Some(Seq("210926"))
  val SampleCompanyNo = Some("29531562")

  val SampleBusinessWithAllFields: Business = Business(
    SampleBusinessId, SampleBusinessName, SampleUPRN, SamplePostCode, SampleIndustryCode, SampleLegalStatus,
    SampleTradingStatus, SampleTurnover, SampleEmploymentBands, SampleVatRefs, SamplePayeRefs, SampleCompanyNo
  )

  val SampleBusinessWithNoOptionalFields: Business = Business(
    SampleBusinessId, SampleBusinessName, None, None, None, None, None, None, None, None, None, None
  )
}
