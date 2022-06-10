/**
 * Copyright (c) 2018-present, A2 Rešitve d.o.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package solutions.a2.iso20022.si.revolut;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import solutions.a2.iso20022.camt053.v001_02.AccountIdentification4Choice;
import solutions.a2.iso20022.camt053.v001_02.AccountStatement2;
import solutions.a2.iso20022.camt053.v001_02.ActiveOrHistoricCurrencyAndAmount;
import solutions.a2.iso20022.camt053.v001_02.BalanceType5Choice;
import solutions.a2.iso20022.camt053.v001_02.BalanceType12;
import solutions.a2.iso20022.camt053.v001_02.BalanceType12Code;
import solutions.a2.iso20022.camt053.v001_02.BankToCustomerStatementV02;
import solutions.a2.iso20022.camt053.v001_02.BankTransactionCodeStructure4;
import solutions.a2.iso20022.camt053.v001_02.BranchAndFinancialInstitutionIdentification4;
import solutions.a2.iso20022.camt053.v001_02.CashAccount20;
import solutions.a2.iso20022.camt053.v001_02.CashBalance3;
import solutions.a2.iso20022.camt053.v001_02.CreditDebitCode;
import solutions.a2.iso20022.camt053.v001_02.DateAndDateTimeChoice;
import solutions.a2.iso20022.camt053.v001_02.Document;
import solutions.a2.iso20022.camt053.v001_02.EntryDetails1;
import solutions.a2.iso20022.camt053.v001_02.EntryStatus2Code;
import solutions.a2.iso20022.camt053.v001_02.EntryTransaction2;
import solutions.a2.iso20022.camt053.v001_02.FinancialInstitutionIdentification7;
import solutions.a2.iso20022.camt053.v001_02.GroupHeader42;
import solutions.a2.iso20022.camt053.v001_02.NumberAndSumOfTransactions1;
import solutions.a2.iso20022.camt053.v001_02.ObjectFactory;
import solutions.a2.iso20022.camt053.v001_02.PartyIdentification32;
import solutions.a2.iso20022.camt053.v001_02.PostalAddress6;
import solutions.a2.iso20022.camt053.v001_02.ProprietaryBankTransactionCodeStructure1;
import solutions.a2.iso20022.camt053.v001_02.RemittanceInformation5;
import solutions.a2.iso20022.camt053.v001_02.ReportEntry2;
import solutions.a2.iso20022.camt053.v001_02.TotalTransactions2;
import solutions.a2.iso20022.camt053.v001_02.TransactionReferences2;


public class BankToCustomerStatement {

	private static final int MAX_35_TEXT = 35;
	private static final int MAX_140_TEXT = 140;

	private final ObjectFactory factory;
	private final String currencyCode;
	private final Instant now;
	private final GregorianCalendar calendar;
	private final DecimalFormat decimalFormat;
	private final ZoneId localZoneId;
	private final List<CSVRecord> rows;
	private final ZonedDateTime startDate;
	private final ZonedDateTime endDate;
	private final String fieldStarted;
	private final String fieldCompleted;

	BankToCustomerStatement(
			final String timeZoneName,
			final String currencyCode,
			final String fileName) throws IOException {
		factory = new ObjectFactory();
		this.currencyCode = currencyCode;
		now = Instant.now(); 
		calendar = new GregorianCalendar();
		calendar.setTimeInMillis(now.toEpochMilli());

		final DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setGroupingSeparator(',');
		symbols.setDecimalSeparator('.');
		decimalFormat = new DecimalFormat("0.##", symbols);
		decimalFormat.setParseBigDecimal(true);

		localZoneId = ZoneId.of(timeZoneName);

		Reader in = new FileReader(fileName);
		final CSVFormat format = CSVFormat.DEFAULT.builder()
				.setHeader()
				.setSkipHeaderRecord(true)
				.build();
		CSVParser parser = new CSVParser(in, format);
		rows = parser.getRecords();
		parser.close();
		in.close();

		if (rows.size() == 0) {
			throw new IOException("Unable to process empty statement!");
		}

		fieldStarted = "Date started (" + timeZoneName + ")";
		fieldCompleted = "Date completed (" + timeZoneName + ")";

		startDate = LocalDate
				.parse(rows.get(rows.size() - 1).get(fieldStarted), DateTimeFormatter.ISO_DATE)
				.atStartOfDay(localZoneId);
		endDate = LocalDate
				.parse(rows.get(0).get(fieldCompleted), DateTimeFormatter.ISO_DATE)
				.atStartOfDay(localZoneId);

		if (startDate.getMonthValue() != endDate.getMonthValue() ||
				startDate.getYear() != endDate.getYear()) {
		//TODO
		//TODO Month only!!!
		//TODO
		}
	}

	private GroupHeader42 groupHeader() throws ParseException, DatatypeConfigurationException {
		final DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyyMMddhhmmssSSS").withZone(localZoneId);
		final GroupHeader42 grpHdr = factory.createGroupHeader42();
		grpHdr.setCreDtTm(DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar));
		grpHdr.setMsgId("MSGSTMT" + df.format(now));
		return grpHdr;
	}

	private CashBalance3 openingBalance() throws ParseException, DatatypeConfigurationException {
		final CashBalance3 balance = factory.createCashBalance3();

		final BalanceType12 tp = factory.createBalanceType12();
		final BalanceType5Choice cd = factory.createBalanceType5Choice();
		cd.setCd(BalanceType12Code.OPBD);
		tp.setCdOrPrtry(cd);
		balance.setTp(tp);

		final ActiveOrHistoricCurrencyAndAmount amt = factory.createActiveOrHistoricCurrencyAndAmount();
		amt.setCcy(currencyCode);
		final BigDecimal openingBal = (BigDecimal) decimalFormat.parse(rows.get(rows.size() - 1).get("Balance"));
		final BigDecimal amount = (BigDecimal) decimalFormat.parse(rows.get(rows.size() - 1).get("Amount"));
		final BigDecimal fee = (BigDecimal) decimalFormat.parse(rows.get(rows.size() - 1).get("Fee"));
		amt.setValue(openingBal.subtract(amount.add(fee)));
		balance.setAmt(amt);

		if (amt.getValue().compareTo(BigDecimal.ZERO) > 0) {
			balance.setCdtDbtInd(CreditDebitCode.CRDT);
		} else {
			balance.setCdtDbtInd(CreditDebitCode.DBIT);
		}

		final DateAndDateTimeChoice dt = factory.createDateAndDateTimeChoice();
		final ZonedDateTime openingDate = startDate
											.minusMonths(1)
											.with(TemporalAdjusters.lastDayOfMonth());
		dt.setDt(
				DatatypeFactory
					.newInstance().
					newXMLGregorianCalendar(GregorianCalendar.from(openingDate)));
		balance.setDt(dt);

		return balance;
	}

	private CashBalance3 closingBalance() throws ParseException, DatatypeConfigurationException {
		final CashBalance3 balance = factory.createCashBalance3();

		final BalanceType12 tp = factory.createBalanceType12();
		final BalanceType5Choice cd = factory.createBalanceType5Choice();
		cd.setCd(BalanceType12Code.CLBD);
		tp.setCdOrPrtry(cd);
		balance.setTp(tp);

		final ActiveOrHistoricCurrencyAndAmount amt = factory.createActiveOrHistoricCurrencyAndAmount();
		amt.setCcy(currencyCode);
		amt.setValue((BigDecimal) decimalFormat.parse(rows.get(0).get("Balance")));
		balance.setAmt(amt);

		if (amt.getValue().compareTo(BigDecimal.ZERO) > 0) {
			balance.setCdtDbtInd(CreditDebitCode.CRDT);
		} else {
			balance.setCdtDbtInd(CreditDebitCode.DBIT);
		}

		final DateAndDateTimeChoice dt = factory.createDateAndDateTimeChoice();
		//TODO - or just last date???
		final ZonedDateTime closingDate = endDate
											.with(TemporalAdjusters.lastDayOfMonth());
		dt.setDt(
				DatatypeFactory
					.newInstance().
					newXMLGregorianCalendar(GregorianCalendar.from(closingDate)));
		balance.setDt(dt);

		return balance;
	}

	private CashAccount20 cashAccount(
			final String partyCountryCode,
			final String partyAdrLine1,
			final String partyAdrLine2,
			final String partyName,
			final String branchCountry,
			final String branchBic,
			final String branchName,
			final String iban) {
		final CashAccount20 cashAcct = factory.createCashAccount20();
		final AccountIdentification4Choice cashAcctId = factory.createAccountIdentification4Choice();
		cashAcctId.setIBAN(iban);
		cashAcct.setId(cashAcctId);

		final PostalAddress6 partyAddress = factory.createPostalAddress6();
		partyAddress.setCtry(partyCountryCode);
		partyAddress.getAdrLine().add(partyAdrLine1);
		partyAddress.getAdrLine().add(partyAdrLine2);
		PartyIdentification32 partyId = factory.createPartyIdentification32();
		partyId.setNm(partyName);
		partyId.setPstlAdr(partyAddress);
		cashAcct.setOwnr(partyId);
	
		final PostalAddress6 branchAddress = factory.createPostalAddress6();
		branchAddress.setCtry(branchCountry);
		FinancialInstitutionIdentification7 branchId = factory.createFinancialInstitutionIdentification7();
		branchId.setBIC(branchBic);
		branchId.setNm(branchName);
		branchId.setPstlAdr(branchAddress);
		BranchAndFinancialInstitutionIdentification4 bankBranch = factory.createBranchAndFinancialInstitutionIdentification4();
		bankBranch.setFinInstnId(branchId);
		cashAcct.setSvcr(bankBranch);

		return cashAcct; 
	}

	private AccountStatement2 accountStatement(
			final String partyCountryCode,
			final String partyAdrLine1,
			final String partyAdrLine2,
			final String partyName,
			final String branchCountry,
			final String branchBic,
			final String branchName,
			final String iban) throws DatatypeConfigurationException, ParseException {
		final AccountStatement2 accStmt = factory.createAccountStatement2();
		accStmt.setId("REVOLUTSTMT" + startDate.getMonthValue() + "/" + startDate.getYear() + "-" + currencyCode);
		accStmt.setLglSeqNb(BigDecimal.valueOf(Integer.parseInt(startDate.getYear() + "" + startDate.getMonthValue())));
		accStmt.setCreDtTm(DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar));
		accStmt.setAcct(cashAccount(
				partyCountryCode, partyAdrLine1, partyAdrLine2, partyName,
				branchCountry, branchBic, branchName, iban));

		accStmt.getBal().add(openingBalance());
		accStmt.getBal().add(closingBalance());
		processTransactions(accStmt);
//		accStmt.getNtry().add(null)
		return accStmt;
	}

	private void processTransactions(final AccountStatement2 accStmt) {
		final TotalTransactions2 txsSummry = factory.createTotalTransactions2();

		final NumberAndSumOfTransactions1 cdt = factory.createNumberAndSumOfTransactions1();
		final NumberAndSumOfTransactions1 dbt = factory.createNumberAndSumOfTransactions1();

		final AtomicInteger cdtCount = new AtomicInteger(0);
		cdt.setSum(BigDecimal.ZERO);
		final AtomicInteger dbtCount = new AtomicInteger(0);
		dbt.setSum(BigDecimal.ZERO);
		rows.forEach(row -> {
			try {
				accStmt.getNtry().add(transactionLine(row));

				final BigDecimal amount = (BigDecimal) decimalFormat.parse(row.get("Amount"));
				if (amount.compareTo(BigDecimal.ZERO) < 0) {
					// Debit
					dbtCount.incrementAndGet();
					dbt.setSum(amount.abs().add(dbt.getSum()));
				} else {
					// Credit
					cdtCount.incrementAndGet();
					cdt.setSum(amount.add(cdt.getSum()));
				}
			} catch (ParseException | DatatypeConfigurationException e) {
				//TODO
				//TODO Auto-generated catch block
				//TODO
				e.printStackTrace();
			}
		});
		cdt.setNbOfNtries(cdtCount.toString());
		dbt.setNbOfNtries(dbtCount.toString());

		txsSummry.setTtlCdtNtries(cdt);
		txsSummry.setTtlDbtNtries(dbt);

		accStmt.setTxsSummry(txsSummry);
	}

	private ReportEntry2 transactionLine(final CSVRecord row) throws ParseException, DatatypeConfigurationException {
		final ReportEntry2 reportEntry = factory.createReportEntry2();

		final ActiveOrHistoricCurrencyAndAmount amt = factory.createActiveOrHistoricCurrencyAndAmount();
		final BigDecimal amount = (BigDecimal) decimalFormat.parse(row.get("Amount"));
		amt.setCcy(currencyCode);
		if (amount.compareTo(BigDecimal.ZERO) < 0) {
			amt.setValue(amount.abs());
			reportEntry.setCdtDbtInd(CreditDebitCode.DBIT);
		} else {
			amt.setValue(amount);
			reportEntry.setCdtDbtInd(CreditDebitCode.CRDT);
		}
		reportEntry.setAmt(amt);
		reportEntry.setSts(EntryStatus2Code.BOOK);

		final DateAndDateTimeChoice bookgDt = factory.createDateAndDateTimeChoice();
		final ZonedDateTime bookgDate = LocalDate
					.parse(row.get(fieldCompleted), DateTimeFormatter.ISO_DATE)
					.atStartOfDay(localZoneId);   
		bookgDt.setDt(DatatypeFactory
				.newInstance()
				.newXMLGregorianCalendar(GregorianCalendar.from(bookgDate)));
		reportEntry.setBookgDt(bookgDt);
		final DateAndDateTimeChoice valDt = factory.createDateAndDateTimeChoice();
		final ZonedDateTime valDate = LocalDate
					.parse(row.get(fieldStarted), DateTimeFormatter.ISO_DATE)
					.atStartOfDay(localZoneId);   
		valDt.setDt(DatatypeFactory
				.newInstance()
				.newXMLGregorianCalendar(GregorianCalendar.from(valDate)));
		reportEntry.setValDt(valDt);


		final String ntryRef = row.get("Reference");
		if (StringUtils.isNotBlank(ntryRef)) {
			if (ntryRef.length() > MAX_35_TEXT) {
				reportEntry.setNtryRef(ntryRef.substring(0, MAX_35_TEXT - 1));
				reportEntry.setAcctSvcrRef(ntryRef.substring(0, MAX_35_TEXT - 1));
			} else {
				reportEntry.setNtryRef(ntryRef);
				reportEntry.setAcctSvcrRef(ntryRef);
			}
			
		}

		//Replace dashes to in 35 char limit!
		final String transId = row.get("ID").replace("-", "");
		BankTransactionCodeStructure4 bkTxCd = factory.createBankTransactionCodeStructure4();
		ProprietaryBankTransactionCodeStructure1 btc = factory.createProprietaryBankTransactionCodeStructure1();
		btc.setCd(transId);
		bkTxCd.setPrtry(btc);
		reportEntry.setBkTxCd(bkTxCd);

		EntryDetails1 details = factory.createEntryDetails1();
		EntryTransaction2 trans = factory.createEntryTransaction2();
		TransactionReferences2 transRefs = factory.createTransactionReferences2();
		transRefs.setTxId(transId);
		transRefs.setEndToEndId("NOTPROVIDED");
		trans.setRefs(transRefs);

		final String ustrd = row.get("Description");
		if (StringUtils.isNotBlank(ustrd)) {
			RemittanceInformation5 ri = factory.createRemittanceInformation5();
			if (ustrd.length() > MAX_140_TEXT) {
				ri.getUstrd().add(ustrd.substring(0, MAX_140_TEXT - 1));
			} else {
				ri.getUstrd().add(ustrd);
			}
			trans.setRmtInf(ri);
		}

		details.getTxDtls().add(trans);
		reportEntry.getNtryDtls().add(details);

		return reportEntry;
	}

	public void write(
			final String partyCountryCode,
			final String partyAdrLine1,
			final String partyAdrLine2,
			final String partyName,
			final String branchCountry,
			final String branchBic,
			final String branchName,
			final String statementIban,
			final String outputFile) throws IOException {

		BankToCustomerStatementV02 statement = factory.createBankToCustomerStatementV02();
		try {
			statement.setGrpHdr(groupHeader());
			statement.getStmt().add(accountStatement(
				partyCountryCode, partyAdrLine1, partyAdrLine2, partyName,
				branchCountry, branchBic, branchName, statementIban));

			Document document = factory.createDocument();
			document.setBkToCstmrStmt(statement);
			JAXBContext jaxbCtx = JAXBContext.newInstance(Document.class);
			Marshaller marshaller = jaxbCtx.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8"); 
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			OutputStream os = new FileOutputStream(outputFile);
			marshaller.marshal(new ObjectFactory().createDocument(document), os);
		} catch (DatatypeConfigurationException | ParseException | JAXBException e) {
			throw new IOException(e);
		}
	}

}
