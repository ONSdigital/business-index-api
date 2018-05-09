package scala.sample

/**
 * Created by coolit on 08/05/2018.
 */
trait SampleBusinessJson {
  val BusinessExactMatchESResponseBody =
    """{
        |"took":3,
        |"timed_out":false,
        |"_shards": {
        |  "total":5,"successful":5,"failed":0},
        |  "hits":{
        |    "total":1,
        |    "max_score":1.0,
        |    "hits":[{
        |        "_index":"bi-dev",
        |        "_type":"business",
        |        "_id":"10205415",
        |        "_version":1,
        |        "_score":1.0,
        |        "_source":{
        |          "LegalStatus":"2",
        |          "BusinessName":"TEST GRILL LTD",
        |          "VatRefs":[105463],
        |          "IndustryCode":"86762",
        |          "CompanyNo":"29531562",
        |          "UPRN":380268,
        |          "Turnover":"A",
        |          "PayeRefs":["210926"],
        |          "TradingStatus":"A",
        |          "PostCode":"ID80 5QB",
        |          "EmploymentBands":"B"
        |        }
        |    }]
        |  }
        |}""".stripMargin

  val BusinessFuzzyMatchESResponseBody =
    """{
       |"took": 99,
       |"timed_out": false,
       |"_shards": {
       |"total": 5,
       |"successful": 5,
       |"failed": 0
       |},
       |"hits": {
       |"total": 6,
       |"max_score": 19.635054,
       |"hits": [
       |  {
       |"_index": "bi-dev",
       |"_type": "business",
       |"_id": "10205415",
       |"_version": 1,
       |"_score": 19.635054,
       |"_source": {
       |"LegalStatus": "2",
       |"BusinessName": "TEST GRILL LTD",
       |"VatRefs": [
       |  105463
       |],
       |"IndustryCode": "86762",
       |"CompanyNo": "29531562",
       |"UPRN": 380268,
       |"Turnover": "A",
       |"PayeRefs": [
       |  "210926"
       |],
       |"TradingStatus": "A",
       |"PostCode": "ID80 5QB",
       |"EmploymentBands": "B"
       |}
       |},
       |  {
       |"_index": "bi-dev",
       |"_type": "business",
       |"_id": "87504854",
       |"_version": 1,
       |"_score": 19.624659,
       |"_source": {
       |"LegalStatus": "6",
       |"BusinessName": "GO LIVE TEST LIMITED",
       |"VatRefs": [
       |  81153
       |],
       |"IndustryCode": "27520",
       |"CompanyNo": "96743003",
       |"UPRN": 578740,
       |"Turnover": "F",
       |"PayeRefs": [
       |  "162306"
       |],
       |"TradingStatus": "D",
       |"PostCode": "ZD38 1TI",
       |"EmploymentBands": "O"
       |}
       |}
       |]
       |}
       |}""".stripMargin
}
