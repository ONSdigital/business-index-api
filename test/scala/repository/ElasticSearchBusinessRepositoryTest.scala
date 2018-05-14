package scala.repository

import java.time.Month._

import com.sksamuel.elastic4s.http.{ HttpExecutable, _ }
import config.ElasticSearchConfig
import models.{ BusinessSearchRequest, ServiceUnavailable }
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FreeSpec, Matchers }
import play.api.mvc.{ AnyContent, Request }
import play.core.j.HttpExecutionContext

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import repository.ElasticSearchBusinessRepository
import utils.{ ElasticClient, ElasticRequestMapper }

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.sample.SampleBusiness

class ElasticSearchBusinessRepositoryTest extends FreeSpec with Matchers with MockFactory {
  //  elastic: HttpClient, requestMapper: ElasticRequestMapper, config: ElasticSearchConfig
  //

  private trait RequestFixture {
    val request = mock[Request[AnyContent]]
    (request.getQueryString _).expects("limit").returning(Some("100"))
    (request.getQueryString _).expects("offset").returning(Some("0"))
    (request.getQueryString _).expects("default_operator").returning(Some("AND"))
  }

  private trait Fixture extends SampleBusiness {
    val requestMapper: ElasticRequestMapper = mock[ElasticRequestMapper]
    val config = ElasticSearchConfig(index = "bi-dev", host = "localhost", port = 9000, ssl = true)
    val esClient: HttpClient = ElasticClient.getElasticClient(config) // mock[HttpClient]
    val repository = new ElasticSearchBusinessRepository(esClient, requestMapper, config)
  }

  "An ElasticSearch Business Repository" - {
    "can retrieve an enterprise unit by Enterprise Reference Number (ERN) and a period (yyyyMM) when a Enterprise exists" ignore new Fixture {
      //      (restRepository.findRow _).expects(TargetTable, TargetRowKey, DefaultColumnFamily).returning(Future.successful(Right(Some(ARow))))
      //      (rowMapper.fromRow _).expects(ARow).returning(Some(TargetEnterpriseUnit))
      //
      //      whenReady(repository.retrieveEnterpriseUnit(TargetErn, TargetPeriod)) { result =>
      //        result.right.value shouldBe Some(TargetEnterpriseUnit)
      //      }
    }

    "can retrieve a business by id, when a business with that id exists" ignore new Fixture {
      //      (esRepository.execute _).expects()
      1 shouldBe 1
    }

    "returns None when a business does not exist for a particular id" ignore new Fixture {
      //      (esRepository.execute _).expects()
    }

    "can retrieve search results for a search for BusinessName" ignore new Fixture with RequestFixture {
      val searchQuery: BusinessSearchRequest = BusinessSearchRequest("BusinessName:test", request)
      val a = new ElasticSearchBusinessRepository(esClient, requestMapper, config)
      // (esClient.execute _)(_: *).expects().returns("")
      val b = a.findBusinessById(12345L).map(x => {
        println(s"x is: ${x}")
      })
      println(s"b is: ${b}")
      1 shouldBe 1

      //
      //
      //      implicit val a = new HttpExecutable[BusinessSearchRequest, String] {
      //        override def execute(client: HttpRequestClient, request: BusinessSearchRequest): Future[HttpResponse] = ???
      //        override def responseHandler: ResponseHandler[String] = ???
      //      }
      //
      //      val heaterStub = stub[HttpClient]
      //
      //      (heaterStub.execute _)(_).when().returns(true)
      //
      //       stub or mock a, syntax for stub different
      //       implicit val b = mock[HttpExecutable[BusinessSearchRequest, String]]
      //
      //            (esRepository.execute _)(b).expects(*).returning(*)
      //       (esRepository.execute(_)(_)).expects(*).returning("123")
      //       (memcachedMock.get(_ : String)(_ : Codec)).expects("some_key", *).returning(Some(123))
    }

    "returns None when a business search returns no results" ignore new Fixture {
      // (esRepository.execute _).expects()
    }
  }

  "An ElasticSearchBusinessRepository" - {
    "can handle ElasticSearch being unavailable" in new Fixture with RequestFixture {
      val searchQuery: BusinessSearchRequest = BusinessSearchRequest("BusinessName:test", request)
      val elasticRepo = new ElasticSearchBusinessRepository(esClient, requestMapper, config)
      val resp = Await.result(elasticRepo.findBusinessById(12345L), 1 seconds)
      resp.left.get.isInstanceOf[ServiceUnavailable] shouldBe true
    }
  }
}
