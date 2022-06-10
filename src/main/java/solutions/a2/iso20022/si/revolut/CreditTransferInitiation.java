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

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import solutions.a2.iso20022.pain001.v001_03.CreditTransferTransactionInformation10;
import solutions.a2.iso20022.pain001.v001_03.CustomerCreditTransferInitiationV03;
import solutions.a2.iso20022.pain001.v001_03.Document;
import solutions.a2.iso20022.pain001.v001_03.PaymentInstructionInformation3;
import solutions.a2.iso20022.pain001.v001_03.StructuredRemittanceInformation7;

public class CreditTransferInitiation {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreditTransferInitiation.class);

	private final CustomerCreditTransferInitiationV03 ccti;

	public CreditTransferInitiation(final String fileName) throws IOException, JAXBException {
		final JAXBContext jaxbContext = JAXBContext.newInstance("solutions.a2.iso20022.pain001.v001_03");
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

	public static void main(String[] argv) {
		// Check for valid log4j configuration
		final String log4jConfig = System.getProperty("a2.log4j.configuration");
		if (log4jConfig == null || "".equals(log4jConfig)) {
			BasicConfigurator.configure();
			LOGGER.warn("JVM argument -Da2.log4j.configuration not set!");
		} else {
			// Check that log4j configuration file exist
			Path path = Paths.get(log4jConfig);
			if (!Files.exists(path) || Files.isDirectory(path)) {
				BasicConfigurator.configure();
				LOGGER.error("JVM argument -Da2.log4j.configuration points to unknown file {}.", log4jConfig);
			} else {
				// Initialize log4j
				PropertyConfigurator.configure(log4jConfig);
			}
		}

		final Options options = new Options();

		final Option optionFileName = new Option("s", "source-file", true,
				"Full path to source file with payment instructions in xml format.");
		optionFileName.setRequired(true);
		options.addOption(optionFileName);

		final Option optionIban = new Option("i", "personal-ibans", true,
				"Comma separated list of IBAN's");
		optionIban.setRequired(true);
		options.addOption(optionIban);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, argv);
		} catch (org.apache.commons.cli.ParseException pe) {
			LOGGER.error(pe.getMessage());
			formatter.printHelp(CreditTransferInitiation.class.getCanonicalName(), options);
			System.exit(1);
		}

		final String fileName = cmd.getOptionValue("s");
		final List<String> personalIbans = Arrays.asList(cmd.getOptionValue("i").split(","));

		try {
			CreditTransferInitiation cti = new CreditTransferInitiation(fileName);
			cti.write(SepaUtils.getOutputName(fileName, true), personalIbans);
		} catch (IOException | JAXBException e) {
			LOGGER.error(e.getMessage());
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			LOGGER.error(sw.toString());
		}
	}
}
