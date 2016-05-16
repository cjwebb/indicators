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

package uk.gov.hmrc.indicators.service

import java.time.LocalDate

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


case class Release(version: String, releasedAt: LocalDate)

trait ReleasesDataSource {

  def getServiceReleases(serviceName: String): Future[List[Release]]

}

class AppReleasesDataSource(releasesClient: ReleasesClient) extends ReleasesDataSource {
  def getServiceReleases(serviceName: String): Future[List[Release]] =
    releasesClient.getAllReleases.map(ReleasesByService(serviceName))

}

object ReleasesByService {
  def apply(serviceName: String)(allReleases: List[AppRelease]): List[Release] = {
    allReleases.foldLeft(Set.empty[Release]) { case (rss, ar) =>
      if (byServiceNameAndEnv(serviceName)(ar))
        rss + Release(ar.ver, ar.fs)
      else rss
    }.toList
  }

  def byServiceNameAndEnv(serviceName: String): (AppRelease) => Boolean = {
    r => r.an == serviceName && (r.env.startsWith("production") || r.env.startsWith("prod"))
  }
}





