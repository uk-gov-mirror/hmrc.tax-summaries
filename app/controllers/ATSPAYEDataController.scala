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

import com.google.inject.Inject
import controllers.auth.PayeAuthAction
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import services.NpsService
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

class ATSPAYEDataController @Inject()(npsService: NpsService, payeAuthAction: PayeAuthAction) extends BaseController {

  def getATSData(nino: String, taxYear: Int): Action[AnyContent] = payeAuthAction.async { implicit request =>
    npsService.getPayeATSData(nino, taxYear) map {
      case Right(response)     => Ok(Json.toJson(response))
      case Left(errorResponse) => new Status(errorResponse.status).apply(errorResponse.json)
    }
  }
}
