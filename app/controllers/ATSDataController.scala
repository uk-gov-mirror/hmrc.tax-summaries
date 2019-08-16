/*
 * Copyright 2019 HM Revenue & Customs
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

import errors.AtsError
import models.AtsYearList
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import services.OdsService
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

object ATSDataController extends ATSDataController {
  override val odsService = OdsService
}

trait ATSDataController extends BaseController {

  def odsService: OdsService

  def hasAts(utr: String): Action[AnyContent] = Action.async {
    implicit request => {
        odsService.getList(utr) map (Ok(_)) recover {
          case error => NotFound
        }
      }
  }

  def getATSData(utr: String, tax_year: Int): Action[AnyContent] = Action.async {
    implicit request => {
      odsService.getPayload(utr, tax_year)  map {Ok(_)}
    }
  }

  def getATSList(utr: String): Action[AnyContent] = Action.async {
    implicit request => {
      odsService.getATSList(utr).map{
        _.fold({
          case AtsError("NoAtsData") => NotFound
          case AtsError("JsonParsingError") => InternalServerError("Failed to parse Json data")
          case AtsError(e) => InternalServerError(e)
        },{
            atsYearList =>  Ok(Json.toJson(atsYearList))
          })
      }
    }
  }
}
