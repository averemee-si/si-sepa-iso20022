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

package eu.solutions.a2.iso20022.si.revolut;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;

import eu.solutions.a2.iso20022.pain001.v001_03.CreditTransferTransactionInformation10;
import eu.solutions.a2.iso20022.pain001.v001_03.CustomerCreditTransferInitiationV03;
import eu.solutions.a2.iso20022.pain001.v001_03.Document;
import eu.solutions.a2.iso20022.pain001.v001_03.PaymentInstructionInformation3;
import eu.solutions.a2.iso20022.pain001.v001_03.StructuredRemittanceInformation7;

public class CreditTransferInitiation {

	private final CustomerCreditTransferInitiationV03 ccti;

	public CreditTransferInitiation(final String fileName) throws IOException, JAXBException {
		final JAXBContext jaxbContext = JAXBContext.newInstance("eu.solutions.a2.iso20022.pain001.v001_03");
		final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		final Source source = new StreamSource(new FileInputStream(fileName));
		final JAXBElement<Document> jaxbElement = unmarshaller.unmarshal(source, Document.class);
		ccti = jaxbElement.getValue().getCstmrCdtTrfInitn();
		if (ccti == null) {
			throw new JAXBException("Wrong pain001.v001.03 format!!!");
		}
	}

	public void write(final String fileName, List<String> personalIbanList) throws IOException, JAXBException {
		final Set<String> personalIbans = new HashSet<>();
		if (personalIbanList != null) {
			personalIbanList.forEach(iban -> 
				personalIbans.add(StringUtils.upperCase(iban)));
		}
		final FileWriter fileWriter = new FileWriter(fileName);
		final CSVFormat format = CSVFormat.DEFAULT.builder()
				.setHeader(
						"Name",
						"Recipient type",
						"IBAN",
						"BIC",
						"Recipient bank country",
						"Currency",
						"Amount",
						"Payment reference")
				.build();
		final CSVPrinter printer = new CSVPrinter(fileWriter, format);

		for (PaymentInstructionInformation3 pii : ccti.getPmtInf()) {
			CreditTransferTransactionInformation10 ctti = pii.getCdtTrfTxInf().get(0);
			final String iban = StringUtils.upperCase(ctti.getCdtrAcct().getId().getIBAN());
			final StructuredRemittanceInformation7 sri = ctti.getRmtInf().getStrd().get(0);
			final String recipientType;
			if (personalIbans.contains(iban)) {
				recipientType = "Individual";
			} else {
				recipientType = "Company";
			}
			printer.printRecord(
					ctti.getCdtr().getNm(),						//Name
					recipientType,								//Recipient type
					iban,										//IBAN
					ctti.getCdtrAgt().getFinInstnId().getBIC(),	//BIC
					iban.substring(0, 2),						//Recipient bank country
					ctti.getAmt().getInstdAmt().getCcy(),		//Currency
					ctti.getAmt().getInstdAmt().getValue(),		//Amount
					sri.getCdtrRefInf().getRef()				//Payment reference
			);
		}
		printer.close();
		fileWriter.close();
	}
	
}
