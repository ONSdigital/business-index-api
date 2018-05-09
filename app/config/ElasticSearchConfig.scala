package config

case class ElasticSearchConfig(index: String, host: String, port: Int, ssl: Boolean)
