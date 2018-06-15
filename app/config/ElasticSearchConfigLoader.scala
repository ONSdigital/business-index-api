package config

import com.typesafe.config.Config
import com.typesafe.sslconfig.util.ConfigLoader

object ElasticSearchConfigLoader extends ConfigLoader[ElasticSearchConfig] {
  def load(rootConfig: Config, path: String = "db.elasticsearch"): ElasticSearchConfig = {
    val config = rootConfig.getConfig(path)
    ElasticSearchConfig(
      username = config.getString("username"),
      password = config.getString("password"),
      index = config.getString("index"),
      port = config.getInt("port"),
      ssl = config.getBoolean("ssl"),
      host = config.getString("host"),
      loadTestData = config.getBoolean("loadTestData"),
      recreateIndex = config.getBoolean("recreateIndex"),
      csvFilePath = config.getString("csvFilePath"),
      connectionTimeout = config.getInt("connectionTimeout")
    )
  }
}
