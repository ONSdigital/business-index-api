package services

import play.api.libs.json._

/**
 * Created by Volodymyr.Glushak on 27/04/2017.
 */
object JsonHelpers {

  implicit class WritesOps[A](val self: Writes[A]) extends AnyVal {
    def ensureField(fieldName: String, path: JsPath = __, value: JsValue = JsNull): Writes[A] = {
      val update = path.json.update(
        __.read[JsObject].map(o => if (o.keys.contains(fieldName)) o else o ++ Json.obj(fieldName -> value))
      )
      self.transform(js => js.validate(update) match {
        case JsSuccess(v, _) => v
        case err: JsError => throw new JsResultException(err.errors)
      })
    }

    def ensureFields(fieldNames: String*)(value: JsValue = JsNull, path: JsPath = __): Writes[A] =
      fieldNames.foldLeft(self)((w, fn) => w.ensureField(fn, path, value))

  }

}

/*
object WriteWithOptions extends DefaultWrites {

  override def OptionWrites[T](implicit fmt: Writes[T]): Writes[Option[T]] = (o: Option[T]) => {
    o match {
      case Some(a) => Json.toJson(a)(fmt)
      case None => JsNull
    }
  }
}*/
