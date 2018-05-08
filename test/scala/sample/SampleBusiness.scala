package scala.sample

import models.Business

/**
 * Created by coolit on 08/05/2018.
 */
trait SampleBusiness {
  val SampleBusinessId = 1234567890L
  val SampleBusinessName = "Test Business"
  val SampleUPRN = Some(9876543210L)
  val SamplePostCode = Some("NP10 XGH")
  val SampleIndustryCode = Some("A")
  val SampleLegalStatus = Some("D")
  val SampleTradingStatus = Some("C")
  val SampleTurnover = Some("1000")
  val SampleEmploymentBands = Some("A")
  val SampleVatRefs = Some(Seq("HJ34", "LP78"))
  val SamplePayeRefs = Some(Seq("1234", "5678"))
  val SampleCompanyNo = Some("AB123456")

  val SampleBusinessWithAllFields: Business = Business(
    SampleBusinessId, SampleBusinessName, SampleUPRN, SamplePostCode, SampleIndustryCode, SampleLegalStatus,
    SampleTradingStatus, SampleTurnover, SampleEmploymentBands, SampleVatRefs, SamplePayeRefs, SampleCompanyNo
  )

  val SampleBusinessWithNoOptionalFields: Business = Business(
    SampleBusinessId, SampleBusinessName, None, None, None, None, None, None, None, None, None, None
  )
}
