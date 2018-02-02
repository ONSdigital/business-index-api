package connector

import javax.inject.{ Inject, Singleton }

import org.apache.hadoop.conf.{ Configuration => HadoopConfiguration }
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.{ HBaseConfiguration, TableName }
import play.api.{ Configuration => PlayConfig }
import org.slf4j.LoggerFactory
import play.api.inject.ApplicationLifecycle

import scala.annotation.tailrec
import scala.concurrent.{ ExecutionContext, Future }
import play.api.Play

import scala.util.{ Failure, Success, Try }

@Singleton
class HBaseConnector @Inject() (lifecycle: ApplicationLifecycle, config: PlayConfig, envt: play.api.Environment)(implicit context: ExecutionContext) {

  val logger = LoggerFactory.getLogger(getClass)

  private[this] val conf: HadoopConfiguration = HBaseConfiguration.create()

  /**
   * From http://hbase.apache.org/0.94/book/zookeeper.html
   * A distributed Apache HBase (TM) installation depends on a running ZooKeeper cluster. All participating nodes and clients
   * need to be able to access the running ZooKeeper ensemble. Apache HBase by default manages a ZooKeeper "cluster" for you.
   * It will start and stop the ZooKeeper ensemble as part of the HBase start/stop process. You can also manage the ZooKeeper
   * ensemble independent of HBase and just point HBase at the cluster it should use. To toggle HBase management of ZooKeeper,
   * use the HBASE_MANAGES_ZK variable in conf/hbase-env.sh. This variable, which defaults to true, tells HBase whether to
   * start/stop the ZooKeeper ensemble servers as part of HBase start/stop.
   */
  val env = sys.props.get("environment").getOrElse("default")

  private val envvv = config.getConfig(s"env.$env.hbase.zookeeper")

  val zk = envvv.flatMap(e => e.getString("quorum")).getOrElse {
    throw new Exception("no config entry for 'hbase.zookeeper.quorum'")
  }
  conf.set("hbase.zookeeper.quorum", zk)

  // everything lazy: initialized only when used...
  lazy val connection: Connection = ConnectionFactory.createConnection(conf)

  def getTable(tableName: String): Table = connection.getTable(TableName.valueOf(tableName))

  def closedConnection: Boolean = {

    def isClosed(waitingMillis: Long): Boolean = if (!connection.isClosed) {
      wait(waitingMillis)
      connection.isClosed
    } else true

    @tailrec
    def tryClosing(checkIntervalSec: Long, totalNoOfAttempts: Int, noOfAttemprtLeft: Int): Boolean = {
      if (connection.isClosed) true
      else if (noOfAttemprtLeft == 0) {
        logger.warn(s"Could not close HBase connection. Attempted $totalNoOfAttempts times with intervals of $checkIntervalSec millis")
        false
      } else {
        connection.close
        if (isClosed(checkIntervalSec)) true
        else {
          logger.info(s"trying closing hbase connection. Attempt ${totalNoOfAttempts - noOfAttemprtLeft} of $totalNoOfAttempts")
          tryClosing(checkIntervalSec, totalNoOfAttempts, noOfAttemprtLeft - 1)
        }
      }
    }

    val checkIntervalSec: Long = config.getLong("hbase.connection.closeattempt.interval").getOrElse(1000L)
    val noOfAttempts: Int = config.getInt("hbase.connection.closeattempt.number").getOrElse(5)

    tryClosing(checkIntervalSec, noOfAttempts, noOfAttempts)
  }

  lifecycle.addStopHook { () =>
    Future {
      Try(closedConnection) match {
        case Success(isConnectionClosed) => if (isConnectionClosed) logger.info("HBase connection successfully closed.")
        else logger.warn("Could not close HBase connection.")
        case Failure(e) => logger.warn("Error occurred while closing HBase connection.", e)
      }

    }

  }
}

