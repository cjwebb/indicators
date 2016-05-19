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

import java.io.ByteArrayInputStream
import java.time.YearMonth

import play.api.libs.iteratee.Enumerator
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.indicators.ComponentRegistry
import uk.gov.hmrc.indicators.service.{LeadTimeResult, IndicatorsService}
import uk.gov.hmrc.play.microservice.controller.BaseController
import scala.concurrent.ExecutionContext.Implicits.global


import scala.concurrent.Future

object ServiceIndicatorController extends ServiceIndicatorController {

  override val indicatorsService: IndicatorsService = ComponentRegistry.indicatorsService
}


trait ServiceIndicatorController extends BaseController {

  val AcceptingCsv = Accepting("text/csv")

  def indicatorsService: IndicatorsService


  def frequentProdRelease(serviceName: String) = Action.async { implicit request =>

    getLedTimeResults(serviceName).map {
      ls =>
        render {
          case Accepts.Json() => Ok(Json.toJson(ls)).as("application/json")
          case AcceptingCsv() => Ok.chunked(Enumerator(LeadTimeCsv(ls, serviceName))).as("text/csv")
        }
    }

  }


  private def getLedTimeResults(serviceName: String) = {
    indicatorsService.getProductionDeploymentLeadTime(serviceName)
  }
}

object LeadTimeCsv {

  def apply(leadTimes: List[LeadTimeResult], serviceName: String) = {
    leadTimes.flatMap(LeadTimeResult.unapply).unzip match {
      case (m, lt) =>
        s"""|Name,${m.mkString(",")}
            |$serviceName,${lt.map(_.getOrElse("")).mkString(",")}""".stripMargin
    }
  }
}
