package config

import com.typesafe.config.Config

/**
 * Created by coolit on 04/05/2018.
 */
object ElasticSearchConfigLoader {
  def load(rootConfig: Config, path: String = "db.elasticsearch"): ElasticSearchConfig = {
    val config = rootConfig.getConfig(path)
    ElasticSearchConfig(
      index = config.getString("index"),
      port = config.getInt("port"),
      ssl = config.getBoolean("ssl"),
      host = config.getString("host")
    )
  }
}
