package controllers

import javax.inject._
import play.api.mvc._

@Singleton
class HomeController @Inject()(webJarAssets: WebJarAssets) extends Controller {

  def index: Action[AnyContent] = Action {
    Ok(views.html.index("ONS BI DEMO", webJarAssets))
  }

  def importer: Action[AnyContent] = Action {
    Ok(views.html.importer("ONS BI Importer", webJarAssets))
  }
}
