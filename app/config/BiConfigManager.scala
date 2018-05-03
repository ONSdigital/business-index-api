package config

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

/**
 * Created by coolit on 03/05/2018.
 */
object BiConfigManager extends LazyLogging {

  def envConf(conf: Config): Config = {
    val env = sys.props.get("environment").getOrElse("default")
    logger.info(s"Load config for [$env] env")
    val envConf = conf.getConfig(s"env.$env")
    logger.debug(envConf.toString)
    envConf
  }
}
