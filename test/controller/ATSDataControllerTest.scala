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

package controller

import akka.stream.Materializer
import com.google.inject.Inject
import controllers.ATSDataController
import errors.AtsError
import models.{AtsMiddleTierTaxpayerData, AtsYearList}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.test.FakeRequest
import services.OdsService
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eqTo, _}
import play.api.libs.json.Json
import utils.TestConstants._

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class ATSDataControllerTest extends UnitSpec with MockitoSugar with WithFakeApplication with ScalaFutures {

  implicit val mtzr: Materializer = fakeApplication.materializer


  implicit val defaultPatience =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  class TestController extends ATSDataController {
    val request = FakeRequest()
    override lazy val odsService: OdsService = mock[OdsService]
  }

  "getAtsData" should {

    "return a failed future" in new TestController {
      when(odsService.getPayload(eqTo(testUtr), eqTo(2014))(any[HeaderCarrier])).thenReturn(Future.failed(new Exception("failed")))
      val result = getATSData(testUtr, 2014)(request)

      whenReady(result.failed) { exception =>
        exception shouldBe a [Exception]
      }
    }
  }

  "hasAts" should {

    "return Not Found (404) on error" in new TestController {
      when(odsService.getList(eqTo(testUtr))(any[HeaderCarrier])).thenReturn(Future.failed(new Exception("failed")))
      val result = hasAts(testUtr)(request)
      status(result) shouldBe 404
    }
  }

  "getATSList" should {

    "return 200 ATlist as json" in new TestController {

      val atsYearList = AtsYearList(testUtr, AtsMiddleTierTaxpayerData(None, None), Nil)

      when(odsService.getATSList(eqTo(testUtr))(any[HeaderCarrier])).thenReturn(Future.successful(Right(atsYearList)))

      val result = getATSList(testUtr)(request)

      status(result) shouldBe OK
      whenReady(result) {res =>
        bodyOf(res) shouldBe Json.toJson(atsYearList).toString
      }

    }

    "return an Internal Server Error" when {

      "a generic error occurs" in new TestController {
        when(odsService.getATSList(eqTo(testUtr))(any[HeaderCarrier])).thenReturn(Future.successful(Left(AtsError("Generic error"))))

        val res = getATSList(testUtr)(request)
        status(res) shouldBe INTERNAL_SERVER_ERROR
        whenReady(res) { res =>
          bodyOf(res) shouldBe "Generic error"
        }
      }


      "a Json Parsing Error occurs" in new TestController {
        when(odsService.getATSList(eqTo(testUtr))(any[HeaderCarrier])).thenReturn(Future.successful(Left(AtsError("JsonParsingError"))))

        val res = getATSList(testUtr)(request)
        status(res) shouldBe INTERNAL_SERVER_ERROR
        whenReady(res) {res =>
          bodyOf(res) shouldBe "Failed to parse Json data"
        }
      }
    }

    "return a Not Found Error" in new TestController {
      when(odsService.getATSList(eqTo(testUtr))(any[HeaderCarrier])).thenReturn(Future.successful(Left(AtsError("NoAtsData"))))
      val res = getATSList(testUtr)(request)
      status(res) shouldBe NOT_FOUND

    }

    "return a failed future" in new TestController {
      when(odsService.getATSList(eqTo(testUtr))(any[HeaderCarrier])).thenReturn(Future.failed(new Exception("failed")))
      val result = getATSList(testUtr)(request)

      whenReady(result.failed) { exception =>
        exception shouldBe a [Exception]
      }
    }
  }
}
