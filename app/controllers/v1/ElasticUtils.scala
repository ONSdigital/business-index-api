package controllers.v1

import com.typesafe.config.Config

/**
 * Created by Volodymyr.Glushak on 06/04/2017.
 */
trait ElasticUtils {

  protected def config: Config

  protected val indexName: String = config.getString("elasticsearch.bi.name").concat("/business")

}
