/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.indicators.controllers

import java.time.YearMonth

import akka.util.Timeout
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.iteratee.Iteratee
import play.api.mvc.{Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.indicators.TestImplicits._
import uk.gov.hmrc.indicators.service.{MeasureResult, FrequentReleaseMetricResult, IndicatorsService, ReleaseLeadTimeResult}

import scala.concurrent.{Await, Future}

class ServiceIndicatorControllerSpec extends PlaySpec with MockitoSugar {

  private val mockIndicatorsService = mock[IndicatorsService]

  val controller = new ServiceIndicatorController {
    override val indicatorsService: IndicatorsService = mockIndicatorsService
  }


  "ServiceIndicatorController.frequentProdRelease" should {

    "return deployment lead time in json by default" in {
      when(mockIndicatorsService.getFrequentReleaseMetric("serviceName")).thenReturn(Future.successful(Some(List())))

      val result = controller.frequentProdRelease("serviceName")(FakeRequest())

      contentAsJson(result) mustBe """[]""".toJson

      header("content-type", result).get mustBe "application/json"

    }

    "return deployment lead time in json when application/json is requested" in {
      when(mockIndicatorsService.getFrequentReleaseMetric("serviceName")).thenReturn(Future.successful(Some(List())))

      val result = controller.frequentProdRelease("serviceName")(FakeRequest().withHeaders("Accepts" -> "application/json"))

      contentAsJson(result) mustBe """[]""".toJson

      header("content-type", result).get mustBe "application/json"

    }


    "return Frequent release metric for a given service in json format" in {

      when(mockIndicatorsService.getFrequentReleaseMetric("serviceName")).thenReturn(Future.successful(
        Some(List(
          FrequentReleaseMetricResult(YearMonth.of(2016, 4), Some(MeasureResult(5)),Some(MeasureResult(4))),
          FrequentReleaseMetricResult(YearMonth.of(2016, 5), Some(MeasureResult(6)), None)
        )))
      )

      val result = controller.frequentProdRelease("serviceName")(FakeRequest())

      contentAsJson(result) mustBe
        """[
          |{"period" : "2016-04", "leadTime" : {"median" : 5}, "interval" : {"median" : 4}},
          |{"period" : "2016-05", "leadTime" : {"median" : 6}}
          |]""".stripMargin.toJson

      header("content-type", result).get mustBe "application/json"
    }


    "return NotFound if None lead times returned" in {
      when(mockIndicatorsService.getFrequentReleaseMetric("serviceName")).thenReturn(Future.successful(None))

      val result = controller.frequentProdRelease("serviceName")(FakeRequest())

      status(result) mustBe NOT_FOUND


    }




  }

  /**
   * http://stackoverflow.com/questions/28461877/is-there-a-bug-in-play2-testing-with-fakerequests-and-chunked-responses-enumera
   */
  def contentAsBytes(of: Future[Result])(implicit timeout: Timeout): Array[Byte] = {
    val r = Await.result(of, timeout.duration)
    val e = r.header.headers.get(TRANSFER_ENCODING) match {
      case Some("chunked") => {
        r.body &> Results.dechunk
      }
      case _ => r.body
    }
    Await.result(e |>>> Iteratee.consume[Array[Byte]](), timeout.duration)
  }


}
