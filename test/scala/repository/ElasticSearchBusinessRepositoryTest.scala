package scala.repository

import java.time.Month._

import com.sksamuel.elastic4s.http.{ HttpExecutable, _ }
import config.ElasticSearchConfig
import models.BusinessSearchRequest
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FreeSpec, Matchers }
import play.api.mvc.{ AnyContent, Request }
import play.core.j.HttpExecutionContext
import repository.ElasticSearchBusinessRepository
import utils.ElasticRequestMapper

import scala.concurrent.{ ExecutionContext, Future }
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
    val esClient: HttpClient = mock[HttpClient]
    val requestMapper: ElasticRequestMapper = mock[ElasticRequestMapper]
    val config = ElasticSearchConfig("bi-dev", "localhost", 9000, true)
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
    }

    "returns None when a business does not exist for a particular id" in new Fixture {
      //      (esRepository.execute _).expects()
    }

    "can retrieve search results for a search for BusinessName" ignore new Fixture with RequestFixture {
      //      val searchQuery: BusinessSearchRequest = BusinessSearchRequest("BusinessName:test", request)
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

      // stub or mock a, syntax for stub different
      // implicit val b = mock[HttpExecutable[BusinessSearchRequest, String]]

      //      (esRepository.execute _)(b).expects(*).returning(*)
      // (esRepository.execute(_)(_)).expects(*).returning("123")
      // (memcachedMock.get(_ : String)(_ : Codec)).expects("some_key", *).returning(Some(123))
    }

    "returns None when a business search returns no results" ignore new Fixture {
      // (esRepository.execute _).expects()
    }
  }
}
