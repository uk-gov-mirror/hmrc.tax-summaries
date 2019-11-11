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

package transformers

import java.text.NumberFormat
import java.util.Locale

import config.ApplicationConfig
import errors.AtsError
import models.Liability.{StatePension, _}
import models.LiabilityTransformer._
import models._
import play.api.Logger
import play.api.libs.json._
import services.TaxRateService
import transformers.ATSDataInterpreter._

case class ATSParsingException(s: String) extends Exception(s)

case class ATSRawDataTransformer(summaryLiability: TaxSummaryLiability, rawTaxPayerJson: JsValue, UTR: String, taxYear: Int) {

  val description=Descriptors(summaryLiability)

  //val formatter = NumberFormat.getNumberInstance(Locale.UK)

  private val noAtsResult: AtsMiddleTierData = AtsMiddleTierData(taxYear, None, None, None, None, None, None, None, None, Option(AtsError("NoAtsError")))

  def atsDataDTO = createATSDataDTO

  private def createATSDataDTO = {
    try {
      description.hasLiability match {
        case true => AtsMiddleTierData(taxYear, Some(UTR), createIncomeTaxData, createSummaryData, createIncomeData, createAllowanceData, createCapitalGainsData, createGovSpendData, createTaxPayerData, None)
        case false => noAtsResult
      }
    }
    catch {
      case ATSParsingException(message) => {
        AtsMiddleTierData(taxYear, None, None, None, None, None, None, None, None, Option(AtsError(message)))
      }
      case otherError: Throwable =>
        Logger.error("Unexpected error has occurred", otherError)
        AtsMiddleTierData(taxYear, None, None, None, None, None, None, None, None, Option(AtsError("Other Error")))
    }
  }

  private def createGovSpendData = {
    val transform = new GovSpendingDataTransformer(description.totalTax, taxYear)
    Option(transform.govSpendReferenceDTO)
  }

  private def createSummaryData = Option(
    DataHolder(Some(createSummaryPageBreakdown), createSummaryPageRates, None))

  private def createIncomeData = Option(
    DataHolder(Some(createYourIncomeBeforeTaxBreakdown), None, None))

  private def createIncomeTaxData = Option(
    DataHolder(Some(createTotalIncomeTaxPageBreakdown), createTotalIncomeTaxPageRates, summaryLiability.incomeTaxStatus))

  private def createAllowanceData = Option(
    DataHolder(Some(createYourTaxFreeAmountBreakdown), None, None))

  private def createCapitalGainsData = Option(DataHolder(Some(createCapitalGainsTaxBreakdown), createCapitalGainsTaxRates, None))

  private def createTaxPayerData = Option(ATSTaxpayerDataTransformer(rawTaxPayerJson).atsTaxpayerDataDTO)

  //done
  //  private def createCapitalGainsTaxBreakdown =
  //    Option(Map("taxable_gains" -> createTaxableGains, //
  //      "less_tax_free_amount" -> createLessTaxFreeAmount, //s
  //      "pay_cg_tax_on" -> createPayCapitalGainsTaxOn, //
  //      "amount_at_entrepreneurs_rate" -> createCtnCgAtEntrepreneursRate, //s
  //      "amount_due_at_entrepreneurs_rate" -> createCtnCgDueEntrepreneursRate, //s
  //      "amount_at_ordinary_rate" -> createCtnCgAtLowerRate, //s
  //      "amount_due_at_ordinary_rate" -> createCtnCgDueLowerRate, //s
  //      "amount_at_higher_rate" -> createCtnCgAtHigherRate, //s
  //      "amount_due_at_higher_rate" -> createCtnCgDueHigherRate, //s
  //      "adjustments" -> createCapAdjustmentAmt, //s
  //      "total_cg_tax" -> createTotalCapitalGainsTax, //
  //      "cg_tax_per_currency_unit" -> createCgTaxPerCurrencyUnit,//TODO done
  //      "amount_at_rpci_lower_rate" -> createCtnCGAtLowerRateRPCI, //s
  //      "amount_due_rpci_lower_rate" -> createCtnLowerRateCgtRPCI, //s
  //      "amount_at_rpci_higher_rate" -> createCtnCGAtHigherRateRPCI, //s
  //      "amount_due_rpci_higher_rate" -> cretaeCtnHigherRateCgtRPCI //s
  //    ))


  private def createCapitalGainsTaxBreakdown: Map[LiabilityTransformer, Amount] =
    Map(
      TaxableGains -> description.taxableGains,
      LessTaxFreeAmount -> description.get(CgAnnualExempt), //s
      PayCgTaxOn -> description.payCapitalGainsTaxOn, //
      AmountAtEntrepreneursRate -> description.get(CgAtEntrepreneursRate), //s
      AmountDueAtEntrepreneursRate -> description.get(CgDueEntrepreneursRate), //s
      AmountAtOrdinaryRate -> description.get(CgAtLowerRate), //s
      AmountDueAtOrdinaryRate -> description.get(CgDueLowerRate), //s
      AmountAtHigherRate -> description.get(CgAtHigherRate), //s
      AmountDueAtHigherRate -> description.get(CgDueHigherRate), //s
      Adjustments -> description.get(CapAdjustment), //s
      TotalCgTax -> description.totalCapitalGainsTax, //
      CgTaxPerCurrencyUnit -> description.capitalGainsTaxPerCurrency, //
      AmountAtRPCILowerRate -> description.get(CGAtLowerRateRPCI), //s
      AmountDueRPCILowerRate -> description.get(LowerRateCgtRPCI), //s
      AmountAtRPCIHigheRate -> description.get(CGAtHigherRateRPCI), //s
      AmountDueRPCIHigherRate -> description.get(HigherRateCgtRPCI) //s
    )


  //TODO RATES
  private def createCapitalGainsTaxRates: Option[Map[String, ApiRate]] =
    Option(Map("cg_entrepreneurs_rate" -> TaxRateService.cgEntrepreneursRate(taxYear),
      "cg_ordinary_rate" -> TaxRateService.cgOrdinaryRate(taxYear),
      "cg_upper_rate" -> TaxRateService.cgUpperRate(taxYear),
      "total_cg_tax_rate" -> description.totalCgTaxLiabilityAsPercentage,
      "prop_interest_rate_lower_rate" -> TaxRateService.individualsForResidentialPropertyAndCarriedInterestLowerRate(taxYear),
      "prop_interest_rate_higher_rate" -> TaxRateService.individualsForResidentialPropertyAndCarriedInterestHigherRate(taxYear)
    ).collect{
      case (k,v)=>(k,v.apiValue)
    }
    )

  //done
  //  private def createYourIncomeBeforeTaxBreakdown =
  //    Option(Map("self_employment_income" -> createSelfEmployment, //
  //      "income_from_employment" -> createIncomeFromEmployment, //s
  //     "state_pension" -> createStatePension, //s
  //      "other_pension_income" -> createOtherPension, //
  //      "taxable_state_benefits" -> createTaxableStateBenefits,//
  //      "other_income" -> createOtherIncome, //
  //      "benefits_from_employment" -> createBenefitsFromEmployment, //s
  //      "total_income_before_tax" -> createTotalIncomeBeforeTax))//

  private def createYourIncomeBeforeTaxBreakdown: Map[LiabilityTransformer, Amount] =
    Map(
      SelfEmploymentIncome -> description.selfEmployment, //
      IncomeFromEmployment -> description.get(SummaryTotalEmployment), //s
      LiabilityTransformer.StatePension ->description.get(StatePension), //s
      OtherPensionIncome -> description.otherPension, //
      TaxableStateBenefits -> description.taxableStateBenefits, //
      OtherIncome -> description.otherIncome, //
      BenefitsFromEmployment -> description.get(EmploymentBenefits), //s
      TotalIncomeBeforeTax ->description.totalIncomeBeforeTax
    ) //

  //done
  //  private def createYourTaxFreeAmountBreakdown =
  //    Option(Map("personal_tax_free_amount" -> createPersonalTaxFreeAmount, //s
  //      "marriage_allowance_transferred_amount" -> createMarriageAllowanceTransferredAmount,//s
  //      "other_allowances_amount" -> createOtherAllowancesAmount,//
  //      "total_tax_free_amount" -> createTotalTaxFreeAmount))//

  private def createYourTaxFreeAmountBreakdown: Map[LiabilityTransformer, Amount] =
    Map(
      PersonalTaxFreeAmount -> description.get(PersonalAllowance), //s
      MarriageAllowanceTransferredAmount -> description.get(MarriageAllceOut), //s
      OtherAllowancesAmount -> description.otherAllowances, //
      TotalTaxFreeAmount -> description.totalTaxFreeAmount
    ) //


  //  private def createSummaryPageBreakdown =
  //    Option(Map("employee_nic_amount" -> createTotalAmountEmployeeNic,
  //      "total_income_tax_and_nics" -> createTotalAmountTaxAndNics,
  //      "your_total_tax" -> createYourTotalTax,
  //      "personal_tax_free_amount" -> createPersonalTaxFreeAmount,
  //      "total_tax_free_amount" -> createTotalTaxFreeAmount,
  //      "total_income_before_tax" -> createTotalIncomeBeforeTax,
  //      "total_income_tax" -> createTotalIncomeTaxAmount,
  //      "total_cg_tax" -> createTotalCapitalGainsTax,
  //      "taxable_gains" -> createTaxableGains,
  //      "cg_tax_per_currency_unit" -> createCgTaxPerCurrencyUnit,
  //      "nics_and_tax_per_currency_unit" -> createNicsAndTaxPerCurrencyUnit))

  //done
  private def createSummaryPageBreakdown: Map[LiabilityTransformer, Amount] =
    Map(
      EmployeeNicAmount -> description.totalAmountEmployeeNic, //
      TotalIncomeTaxAndNics -> description.totalAmountTaxAndNics, //
      YourTotalTax -> description.totalTax, //
      PersonalTaxFreeAmount -> description.get(PersonalAllowance), //s
      TotalTaxFreeAmount ->description.totalTaxFreeAmount, //
      TotalIncomeBeforeTax -> description.totalIncomeBeforeTax, //
      TotalIncomeTax -> description.totalIncomeTaxAmount, //
      TotalCgTax -> description.totalCapitalGainsTax, //
      TaxableGains -> description.taxableGains, //
      CgTaxPerCurrencyUnit ->description.capitalGainsTaxPerCurrency,
      NicsAndTaxPerCurrencyUnit -> description.nicsAndTaxPerCurrency
    ) //TODO Percentage done RATES

  //TODO RATES
  private def createSummaryPageRates: Option[Map[String, ApiRate]] =
    Option(Map("total_cg_tax_rate" -> description.totalCgTaxLiabilityAsPercentage.apiValue, //TODO RATES
      "nics_and_tax_rate" -> description.totalNicsAndTaxLiabilityAsPercentage.apiValue
    )
    )


  //one
  //  private def createTotalIncomeTaxPageBreakdown =
  //    Option(Map("starting_rate_for_savings" -> createStartingRateForSavings, //s
  //      "starting_rate_for_savings_amount" -> createStartingRateForSavingsAmount, //s
  //      "basic_rate_income_tax" -> createBasicRateIncomeTax,//
  //      "basic_rate_income_tax_amount" -> basicRateIncomeTaxAmount,//s
  //    "higher_rate_income_tax" -> createHigherRateIncomeTax, //
  //      "higher_rate_income_tax_amount" -> createHigherRateIncomeTaxAmount,//
  //      "additional_rate_income_tax" -> createAdditionalRateIncomeTax,//
  //      "additional_rate_income_tax_amount" -> createAdditionalRateIncomeTaxAmount,//
  //      "ordinary_rate" -> createOrdinaryRateDividends, //s
  //      "ordinary_rate_amount" -> createOrdinaryRateDividendsAmount,//s
  //      "upper_rate" -> createUpperRateDividends, //s
  //      "upper_rate_amount" -> createUpperRateDividendsAmount, //s
  //      "additional_rate" -> createAdditionalRateDividends, //s
  //      "additional_rate_amount" -> createAdditionalRateDividendsAmount, //s
  //      "other_adjustments_increasing" -> createOtherAdjustmentsIncreasing,//
  //      "marriage_allowance_received_amount" -> createMarriageAllowanceReceivedAmount,//
  //      "other_adjustments_reducing" -> createOtherAdjustmentsReducing,//
  //      "total_income_tax" -> createTotalIncomeTaxAmount,//
  //      "scottish_income_tax" -> createScottishIncomeTax))//

  private def createTotalIncomeTaxPageBreakdown: Map[LiabilityTransformer, Amount] =
    Map(
      StartingRateForSavings -> description.get(SavingsChargeableStartRate), //s
      StartingRateForSavingsAmount -> description.get(SavingsTaxStartingRate), //s
      BasicRateIncomeTax -> description.basicIncomeRateIncomeTax, //
      BasicRateIncomeTaxAmount ->description. basicRateIncomeTaxAmount, //s
      HigherRateIncomeTax -> description.higherRateIncomeTax, //
      HigherRateIncomeTaxAmount ->description. higherRateIncomeTaxAmount, //
      AdditionalRateIncomeTax -> description.additionalRateIncomeTax, //
      AdditionalRateIncomeTaxAmount -> description.additionalRateIncomeTaxAmount, //
      OrdinaryRate -> description.get(DividendChargeableLowRate), //s
      OrdinaryRateAmount -> description.get(DividendTaxLowRate), //s
      UpperRate -> description.get(DividendChargeableHighRate), //s
      UpperRateAmount -> description.get(DividendTaxHighRate), //s --
      AdditionalRate -> description.get(DividendChargeableAddHRate), //s
      AdditionalRateAmount -> description.get(DividendTaxAddHighRate), //s ---
      OtherAdjustmentsIncreasing -> description.otherAdjustmentsIncreasing, //
      MarriageAllowanceReceivedAmount -> description.get(MarriageAllceIn), //
      OtherAdjustmentsReducing -> description.otherAdjustmentsReducing, //
      TotalIncomeTax -> description.totalIncomeTaxAmount, //
      ScottishIncomeTax -> description.scottishIncomeTax
    ) //

//
//  private def createStartingRateForSavings = getTliSlpAmountVal("ctnSavingsChgbleStartRate")
//
//  private def createStartingRateForSavingsAmount = getTliSlpAmountVal("ctnSavingsTaxStartingRate")
//
//  private def pensionLumpSumRate = getTliSlpBigDecimalVal("ctnPensionLumpSumTaxRate")
//
//  private def hasPensionLumpSumAtBasicRate = pensionLumpSumRate.equals(BigDecimal(0.20))
//
//  private def hasPensionLumpSumAtHigherRate = pensionLumpSumRate.equals(BigDecimal(0.40))
//
//  private def hasPensionLumpSumAtAdditionalRate = pensionLumpSumRate.equals(BigDecimal(0.45))
//
//  //done
//  private def createBasicRateIncomeTax = getAmountSum("ctnIncomeChgbleBasicRate", "ctnSavingsChgbleLowerRate")

  //done
  //  private def basicRateIncomeTaxAmount = {
  //    if (hasPensionLumpSumAtBasicRate)
  //      getAmountSum("ctnIncomeTaxBasicRate", "ctnSavingsTaxLowerRate", "ctnPensionLsumTaxDueAmt")
  //    else
  //      getAmountSum("ctnIncomeTaxBasicRate", "ctnSavingsTaxLowerRate")
  //  }

  //done
//  private def createHigherRateIncomeTax = getAmountSum("ctnIncomeChgbleHigherRate", "ctnSavingsChgbleHigherRate")
//
//  //done
//  private def createHigherRateIncomeTaxAmount = {
//    if (hasPensionLumpSumAtHigherRate)
//      getAmountSum("ctnIncomeTaxHigherRate", "ctnSavingsTaxHigherRate", "ctnPensionLsumTaxDueAmt")
//    else
//      getAmountSum("ctnIncomeTaxHigherRate", "ctnSavingsTaxHigherRate")
//  }
//
//  //done
//  private def createAdditionalRateIncomeTax = getAmountSum("ctnIncomeChgbleAddHRate", "ctnSavingsChgbleAddHRate")
//
//  //done
//  private def createAdditionalRateIncomeTaxAmount = {
//    if (hasPensionLumpSumAtAdditionalRate)
//      getAmountSum("ctnIncomeTaxAddHighRate", "ctnSavingsTaxAddHighRate", "ctnPensionLsumTaxDueAmt")
//    else
//      getAmountSum("ctnIncomeTaxAddHighRate", "ctnSavingsTaxAddHighRate")
//  }
//
//  private def createOrdinaryRateDividends = getTliSlpAmountVal("ctnDividendChgbleLowRate")
//
//  private def createOrdinaryRateDividendsAmount = getTliSlpAmountVal("ctnDividendTaxLowRate")
//
//  private def createUpperRateDividends = getTliSlpAmountVal("ctnDividendChgbleHighRate")
//
//  private def createUpperRateDividendsAmount = getTliSlpAmountVal("ctnDividendTaxHighRate")
//
//  private def createAdditionalRateDividends = getTliSlpAmountVal("ctnDividendChgbleAddHRate")
//
//  private def createAdditionalRateDividendsAmount = getTliSlpAmountVal("ctnDividendTaxAddHighRate")
//
//  //done
//  private def createOtherAdjustmentsIncreasing = getAmountSum(
//    "nonDomChargeAmount",
//    "taxExcluded",
//    "incomeTaxDue",
//    "netAnnuityPaytsTaxDue",
//    "ctnChildBenefitChrgAmt",
//    "ctnPensionSavingChrgbleAmt") - getTliSlpAmountVal("ctn4TaxDueAfterAllceRlf")
//
//  //done
//  private def createOtherAdjustmentsReducing = (getAmountSum(
//    "ctnDeficiencyRelief",
//    "topSlicingRelief",
//    "ctnVctSharesReliefAmt",
//    "ctnEisReliefAmt",
//    "ctnSeedEisReliefAmt",
//    "ctnCommInvTrustRelAmt",
//    "atsSurplusMcaAlimonyRel",
//    "ctnNotionalTaxCegs",
//    "ctnNotlTaxOthrSrceAmo",
//    "ctnTaxCredForDivs",
//    "ctnQualDistnReliefAmt",
//    "figTotalTaxCreditRelief",
//    "ctnNonPayableTaxCredits") + createReliefForFinanceCosts).roundAmountUp
//
//  private def createReliefForFinanceCosts = getTliSlpAmountOptVal("reliefForFinanceCosts")

  //done
  //  private def createTotalIncomeTaxAmount = createStartingRateForSavingsAmount + //
  //    basicRateIncomeTaxAmount + //
  //    createHigherRateIncomeTaxAmount + //
  //    createAdditionalRateIncomeTaxAmount + //
  //    createOrdinaryRateDividendsAmount + //s
  //    createUpperRateDividendsAmount + //s
  //    createAdditionalRateDividendsAmount + //s
  //    createOtherAdjustmentsIncreasing - //
  //    createOtherAdjustmentsReducing - //
  //    createMarriageAllowanceReceivedAmount //s

  //rates TODO
  private def createTotalIncomeTaxPageRates: Option[Map[String, ApiRate]] =
    Option(Map(
      "starting_rate_for_savings_rate" -> TaxRateService.startingRateForSavingsRate(taxYear),
      "basic_rate_income_tax_rate" -> TaxRateService.basicRateIncomeTaxRate(taxYear),
      "higher_rate_income_tax_rate" -> TaxRateService.higherRateIncomeTaxRate(taxYear),
      "additional_rate_income_tax_rate" -> TaxRateService.additionalRateIncomeTaxRate(taxYear),
      "ordinary_rate_tax_rate" -> TaxRateService.dividendsOrdinaryRate(taxYear),
      "upper_rate_rate" -> TaxRateService.dividendUpperRateRate(taxYear),
      "additional_rate_rate" -> TaxRateService.dividendAdditionalRate(taxYear)
    ).collect{
      case (k,v)=>(k,v.apiValue)
    }
    )

  //done
//  private def createSelfEmployment = getAmountSum(
//    "ctnSummaryTotalScheduleD",
//    "ctnSummaryTotalPartnership")
//
//  private def createIncomeFromEmployment = getTliSlpAmountVal("ctnSummaryTotalEmployment")
//
//  private def createStatePension = getTliSlpAmountVal("atsStatePensionAmt")
//
//  //done
//  private def createOtherPension = getAmountSum(
//    "atsOtherPensionAmt",
//    "itfStatePensionLsGrossAmt")
//
//  //done
//  private def createTaxableStateBenefits = getAmountSum("atsIncBenefitSuppAllowAmt",
//    "atsJobSeekersAllowanceAmt",
//    "atsOthStatePenBenefitsAmt")
//
//  //done
//  private def createOtherIncome = getAmountSum(
//    "ctnSummaryTotShareOptions",
//    "ctnSummaryTotalUklProperty",
//    "ctnSummaryTotForeignIncome",
//    "ctnSummaryTotTrustEstates",
//    "ctnSummaryTotalOtherIncome",
//    "ctnSummaryTotalUkInterest",
//    "ctnSummaryTotForeignDiv",
//    "ctnSummaryTotalUkIntDivs",
//    "ctn4SumTotLifePolicyGains")
//
//  private def createBenefitsFromEmployment = getTliSlpAmountVal("ctnEmploymentBenefitsAmt")
//
//  //done
//  private def createTotalIncomeBeforeTax =
//    createSelfEmployment +
//      createIncomeFromEmployment +
//      createStatePension +
//      createOtherPension +
//      createTaxableStateBenefits +
//      createOtherIncome +
//      createBenefitsFromEmployment
//
//  //done
//  private def createTaxableGains = getAmountSum(
//    "atsCgTotGainsAfterLosses",
//    "atsCgGainsAfterLossesAmt") //
//
//  private def createCtnCgAtEntrepreneursRate = getTliSlpAmountVal("ctnCgAtEntrepreneursRate")
//
//  private def createCtnCgDueEntrepreneursRate = getTliSlpAmountVal("ctnCgDueEntrepreneursRate")
//
//  private def createCtnCgAtLowerRate = getTliSlpAmountVal("ctnCgAtLowerRate")
//
//  private def createCtnCgDueLowerRate = getTliSlpAmountVal("ctnCgDueLowerRate")
//
//  private def createCtnCgAtHigherRate = getTliSlpAmountVal("ctnCgAtHigherRate")
//
//  private def createCtnCgDueHigherRate = getTliSlpAmountVal("ctnCgDueHigherRate")
//
//  private def createLessTaxFreeAmount = getTliSlpAmountVal("atsCgAnnualExemptAmt")
//
//  private def createCapAdjustmentAmt = getTliSlpAmountVal("capAdjustmentAmt")
//
//  //done
//  private def createTotalCapitalGainsTax = createCtnCgDueEntrepreneursRate + createCtnCgDueLowerRate + createCtnCgDueHigherRate - createCapAdjustmentAmt + createCtnLowerRateCgtRPCI + cretaeCtnHigherRateCgtRPCI
//
//  //done
//  private def createPayCapitalGainsTaxOn = if (createTaxableGains < createLessTaxFreeAmount) Amount(0.00, "GBP") else createTaxableGains - createLessTaxFreeAmount
//
//  private def createPersonalTaxFreeAmount = getTliSlpAmountVal("ctnPersonalAllowance")
//
//  private def createMarriageAllowanceTransferredAmount = getTliSlpAmountOptVal("ctnMarriageAllceOutAmt")
//
//  private def createMarriageAllowanceReceivedAmount = getTliSlpAmountOptVal("ctnMarriageAllceInAmt")
//
 // private def createIncomeTaxStatus = Option(getTliSlpString("incomeTaxStatus"))
//
//  private def createCtnIncomeChgbleBasicRate = getTliSlpAmountOptVal("ctnIncomeChgbleBasicRate")
//
//  private def createCtnIncomeChgbleHigherRate = getTliSlpAmountOptVal("ctnIncomeChgbleHigherRate")
//
//  private def createCtnIncomeChgbleAddHRate = getTliSlpAmountOptVal("ctnIncomeChgbleAddHRate")
//
//  //done
//  private def createScottishIncomeTax = Amount((createCtnIncomeChgbleBasicRate + createCtnIncomeChgbleHigherRate + createCtnIncomeChgbleAddHRate).amount * 0.1, "GBP")
//
//  private def createCtnCGAtLowerRateRPCI = getTliSlpAmountOptVal("ctnCGAtLowerRateRPCI")
//
//  private def createCtnLowerRateCgtRPCI = getTliSlpAmountOptVal("ctnLowerRateCgtRPCI")
//
//  private def createCtnCGAtHigherRateRPCI = getTliSlpAmountOptVal("ctnCGAtHigherRateRPCI")
//
//  private def cretaeCtnHigherRateCgtRPCI = getTliSlpAmountOptVal("ctnHigherRateCgtRPCI")
//
//  //done
//  private def createOtherAllowancesAmount = getAmountSum(
//    "ctnEmploymentExpensesAmt",
//    "ctnSummaryTotalDedPpr",
//    "ctnSumTotForeignTaxRelief",
//    "ctnSumTotLoanRestricted",
//    "ctnSumTotLossRestricted",
//    "grossAnnuityPayts",
//    "itf4GiftsInvCharitiesAmo",
//    "itfTradeUnionDeathBenefits",
//    "ctnBpaAllowanceAmt",
//    "itfBpaAmount",
//    "grossExcludedIncome").roundAmountUp
//
//  //done
//  private def createTotalTaxFreeAmount =
//    createOtherAllowancesAmount +
//      createPersonalTaxFreeAmount -
//      createMarriageAllowanceTransferredAmount
//
//  //done
//  private def createTotalAmountEmployeeNic =
//    getSaPayeAmountVal("employeeClass1Nic") +
//      getSaPayeAmountVal("employeeClass2Nic") +
//      getTliSlpAmountVal("class4Nic")
//
//  //done


  // private def createTotalAmountTaxAndNics = createTotalAmountEmployeeNic + createTotalIncomeTaxAmount

  //done
  //private def createYourTotalTax = totalAmountTaxAndNics + totalCapitalGainsTax

  //done
  //private def createCgTaxPerCurrencyUnit = taxPerTaxableCurrencyUnit(createTotalCapitalGainsTax, createTaxableGains)
//
//  //done
//  private def createTotalCgTaxRate = rateFromPerUnitAmount(createCgTaxPerCurrencyUnit)
////
////  //done
// // private def createNicsAndTaxPerCurrencyUnit = taxPerTaxableCurrencyUnit(createTotalAmountTaxAndNics, createTotalIncomeBeforeTax)
////
//  //done
//  private def createNicsAndTaxTaxRate = rateFromPerUnitAmount(createNicsAndTaxPerCurrencyUnit)
////
////  //done
//  private def taxPerTaxableCurrencyUnit(tax: Amount, taxable: Amount) =
//    taxable match {
//      case value if value.isZero => taxable
//      case _ => tax.divideWithPrecision(taxable, 4)
//    }
////
//  //done
//  private def rateFromPerUnitAmount(amountPerUnit: Amount) = {
//    Rate(formatter.format((amountPerUnit.amount * 100).setScale(2, BigDecimal.RoundingMode.DOWN)) + "%")
//  }
////
//  private def getTliSlpString(key: String): String = {
//    val res = jsonValLookupWithErrorHandlingWithOpt[String](key, "tliSlpAtsData")
//    res.getOrElse("")
//  }
//
//  private def getTliSlpAmountVal(key: String): Amount = {
//    jsonValLookupWithErrorHandling[Amount](key, "tliSlpAtsData")
//  }
//
//  private def getTliSlpAmountOptVal(key: String): Amount = {
//    val res = jsonValLookupWithErrorHandlingWithOpt[Amount](key, "tliSlpAtsData")
//    res.getOrElse(Amount(0, "GBP"))
//  }
//
//  private def getSaPayeAmountVal(key: String): Amount = {
//    jsonValLookupWithErrorHandling[Amount](key, "saPayeNicDetails")
//  }
//
//  private def getTliSlpBigDecimalVal(key: String): BigDecimal = {
//    jsonValLookupWithErrorHandling[BigDecimal](key, "tliSlpAtsData")
//  }
//
//  private def jsonValLookupWithErrorHandlingWithOpt[T: Reads](key: String, topLevelContainer: String): Option[T] = {
//
//    val theOption = (rawJsonFromStub \ topLevelContainer \ key).validate[T]
//
//    theOption match {
//      case s: JsSuccess[T] => Some(s.get)
//      case e: JsError => None
//    }
//  }
//
//  private def jsonValLookupWithErrorHandling[T: Reads](key: String, topLevelContainer: String): T = {
//
//    val theOption = (rawJsonFromStub \ topLevelContainer \ key).validate[T]
//
//    theOption match {
//      case s: JsSuccess[T] => s.get
//      case e: JsError =>
//        Logger.error("Errors: " + JsError.toJson(e).toString() + " we were looking for " + key + " in " + topLevelContainer)
//        throw new ATSParsingException(key)
//    }
//  }
//
//  private def getAmountSum(keys: String*) = {
//    (keys map (key => getTliSlpAmountVal(key))).reduceLeft[Amount](_ + _)
//  }
}


