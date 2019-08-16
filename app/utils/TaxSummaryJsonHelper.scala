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

package utils

import models.AtsYearList
import play.api.libs.json.{JsNumber, JsValue, Json}
import transformers.{ATSTaxpayerDataTransformer, ATSRawDataTransformer}

trait TaxSummaryJsonHelper {

  def getAllATSData(rawTaxpayerJson: JsValue, rawPayloadJson: JsValue, UTR: String, taxYear: Int): JsValue = {
    Json.toJson(ATSRawDataTransformer(rawPayloadJson, rawTaxpayerJson, UTR, taxYear).atsDataDTO)
  }

  def hasAtsForPreviousPeriod(rawJson: JsValue): Boolean = {
    val annualTaxSummaries: List[JsValue] = (rawJson \ "annualTaxSummaries").as[List[JsValue]]
    annualTaxSummaries.flatMap(item => (item \ "taxYearEnd").asOpt[JsNumber]).nonEmpty
  }

  def createTaxYearJson(rawJson: JsValue, utr: String, rawTaxpayerJson: JsValue): AtsYearList = {
    val annualTaxSummariesList: List[JsValue] = (rawJson \ "annualTaxSummaries").as[List[JsValue]]
    val atsYearList = annualTaxSummariesList.map(item => (item \ "taxYearEnd").as[Int])
    val taxPayer = ATSTaxpayerDataTransformer(rawTaxpayerJson).atsTaxpayerDataDTO

    AtsYearList(utr, taxPayer, atsYearList)
  }
}
