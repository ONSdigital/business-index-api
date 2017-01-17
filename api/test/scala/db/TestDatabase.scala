package scala.db

import com.outworkers.phantom.builder.query.CQLQuery
import com.outworkers.phantom.builder.serializers.KeySpaceSerializer
import com.outworkers.phantom.connectors.ContactPoint
import com.outworkers.phantom.dsl._

object TestConnector {

  val space = "business_index_test"

  val initQuery: CQLQuery = KeySpaceSerializer(space).ifNotExists()
      .`with`(replication eqs SimpleStrategy.replication_factor(1))
      .and(durable_writes eqs true)
      .qb

  val connector: KeySpaceDef = ContactPoint.local.keySpace(
    space, (session, keySpace) => initQuery.queryString
  )
}

object TestDatabase extends AppDatabase(TestConnector.connector)

trait TestDbProvider extends BusinessDbProvider {
  override val database: AppDatabase = TestDatabase
}
