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

package transformers

import models.LiabilityKey._
import models.RateKey.{IncomeBasic, IncomeHigher, NICS, Ordinary, Upper}
import models._
import models.paye.{PayeAtsData, PayeAtsMiddleTier}
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.play.test.UnitSpec
import utils.{PayeAtsDataUtil, TestConstants}

class PayeAtsDataTest extends UnitSpec with OneAppPerSuite {

  val atsData: PayeAtsData = PayeAtsDataUtil.atsData
  val nino: String = TestConstants.testNino
  val taxYear = "2020"
  lazy val transformedData: PayeAtsMiddleTier =
    atsData.transformToPayeMiddleTier(nino, taxYear.toInt)

  "transformToPayeMiddleTier" should {
    "populate the nino and tax year" in {
      transformedData.nino shouldBe nino
      transformedData.taxYear shouldBe taxYear.toInt
    }

    "create allowance data" in {
      val allowanceData: DataHolder =
        transformedData.allowance_data.getOrElse(fail("No allowance data"))

      val expectedValues: Map[LiabilityKey, Amount] = Map(
        PersonalTaxFreeAmount              -> Amount.gbp(12500.00),
        MarriageAllowanceTransferredAmount -> Amount.gbp(1250.00),
        OtherAllowancesAmount              -> Amount.gbp(6000.00),
        TotalTaxFreeAmount                 -> Amount.gbp(25500.00),
        TotalIncomeBeforeTax               -> Amount.gbp(28000.00)
      )

      allowanceData shouldBe DataHolder(Some(expectedValues), None, None)

    }

    "create income data" in {
      val incomeData: DataHolder =
        transformedData.income_data.getOrElse(fail("No income data"))

      val expectedValues: Map[LiabilityKey, Amount] = Map(
        IncomeFromEmployment   -> Amount.gbp(25000.00),
        StatePension           -> Amount.gbp(1000.00),
        OtherPensionIncome     -> Amount.gbp(500.00),
        OtherIncome            -> Amount.gbp(3000.00),
        TotalIncomeBeforeTax   -> Amount.gbp(28000.00),
        BenefitsFromEmployment -> Amount.gbp(200.00),
        TaxableStateBenefits   -> Amount.gbp(500.00)
      )
      incomeData shouldBe DataHolder(Some(expectedValues), None, None)
    }

    "create summary data" in {
      val summaryData: DataHolder =
        transformedData.summary_data.getOrElse(fail("No summary data"))

      val expectedValues: Map[LiabilityKey, Amount] = Map(
        TotalIncomeBeforeTax  -> Amount.gbp(28000.00),
        TotalTaxFreeAmount    -> Amount.gbp(25500.00),
        TotalIncomeTaxAndNics -> Amount.gbp(4200.00),
        IncomeAfterTaxAndNics -> Amount.gbp(5000.00),
        TotalIncomeTax        -> Amount.gbp(4010.00),
        EmployeeNicAmount     -> Amount.gbp(200.00),
        EmployerNicAmount     -> Amount.gbp(100.00)
      )

      summaryData shouldBe DataHolder(Some(expectedValues), Some(Map(NICS -> ApiRate("25%"))), None)
    }

    "create income tax" in {
      val incomeTax: DataHolder =
        transformedData.income_tax.getOrElse(fail("No income tax data"))

      val expectedPayloadValues: Map[LiabilityKey, Amount] = Map(
        BasicRateIncomeTaxAmount        -> Amount.gbp(2000.00),
        BasicRateIncomeTax              -> Amount.gbp(10000.00),
        HigherRateIncomeTaxAmount       -> Amount.gbp(2000.00),
        HigherRateIncomeTax             -> Amount.gbp(10000.00),
        OrdinaryRateAmount              -> Amount.gbp(200.00),
        OrdinaryRate                    -> Amount.gbp(2000.00),
        UpperRateAmount                 -> Amount.gbp(200.00),
        UpperRate                       -> Amount.gbp(2000.00),
        MarriedCouplesAllowance         -> Amount.gbp(500.00),
        MarriageAllowanceReceivedAmount -> Amount.gbp(1250.00),
        LessTaxAdjustmentPrevYear       -> Amount.gbp(200.00),
        TaxUnderpaidPrevYear            -> Amount.gbp(200.00),
        TotalIncomeTax                  -> Amount.gbp(4000.00)
      )

      val expectedRatesValues: Map[RateKey, ApiRate] = Map(
        Ordinary     -> ApiRate("7.5%"),
        IncomeHigher -> ApiRate("40%"),
        IncomeBasic  -> ApiRate("20%"),
        Upper        -> ApiRate("32.5%")
      )

      incomeTax shouldBe DataHolder(Some(expectedPayloadValues), Some(expectedRatesValues), None)
    }
  }
}
