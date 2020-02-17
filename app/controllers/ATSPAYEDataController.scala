/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import controllers.auth.AuthAction
import play.api.Play
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import services.NpsService
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

object ATSPAYEDataController extends ATSPAYEDataController {
  override val npsService = NpsService
  override val authAction: AuthAction = Play.current.injector.instanceOf[AuthAction]
}

trait ATSPAYEDataController extends BaseController {

  val authAction: AuthAction

  def npsService: NpsService

  def getATSData(nino: String, taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    npsService.getPayeATSData(nino, taxYear) map {
      case Right(response)     => Ok(Json.toJson(response))
      case Left(errorResponse) => new Status(errorResponse.status).apply(errorResponse.json)
    }
  }
}
