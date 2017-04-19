package uk.gov.ons.bi

/**
  * Created by Volodymyr.Glushak on 09/02/2017.
  */


/**
  * Abstract representation of data source.
  * Assume for tests there will be collections based implementation
  * and for Spark supposed to be RDD based DataSource
  *
  * @tparam T type of data it should process
  */
trait DataSource[I, T] {

  def map[Z](x: T => Z): DataSource[I, Z]

  def filter(x: T => Boolean): DataSource[I, T]

  def foreach[Z](x: T => Z): Unit

  def getById(id: I): Option[T]

}

class MapDataSource[I, T](val data: Map[I, T]) extends DataSource[I, T] {

  override def getById(id: I): Option[T] = data.get(id)

  override def map[Z](x: (T) => Z) = new MapDataSource(data.map { case (k, v) => k -> x(v) })

  override def foreach[Z](x: (T) => Z) = data.foreach { case (k, v) => k -> x(v) }

  override def filter(x: (T) => Boolean) = new MapDataSource(data.filter { case (k, v) => x(v) })
}
