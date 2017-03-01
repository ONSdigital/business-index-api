package scala

import org.scalatest.{FlatSpec, Matchers}
import services.InsertDemoUtils

/**
  * Created by Volodymyr.Glushak on 01/03/2017.
  */
class InsertDemoTest extends FlatSpec with Matchers {

  "It" should "generate data out of sample file" in {
    InsertDemoUtils.generateData.toList.size shouldBe 100000
  }

}
