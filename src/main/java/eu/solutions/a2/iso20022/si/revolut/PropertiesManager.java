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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesManager.class);

	private static final String PROPERTY_IBAN = "IBAN";
	private static final String PROPERTY_PARTY = "PARTY";
	private static final String PROPERTY_ADDR_LINE_1 = "ADDR_LINE_1";
	private static final String PROPERTY_ADDR_LINE_2 = "ADDR_LINE_2";
	private static final String PERSONAL_IBANS = "PERSONAL_IBANS";	
	
	private final String fileName;
	private final Properties props;
	private File propsFile;

	public PropertiesManager() {
		final String directory = System.getProperty("user.home") + File.separator + ".a2-solutions.eu";
		fileName = directory + File.separator + "SEPA-SI.properties";
		props = new Properties();
		try {
			final File propsDir = new File(directory);
			if (!propsDir.exists()) {
				propsDir.mkdirs();
			}
			propsFile = new File(fileName);
			if (propsFile.exists()) {
				try (InputStream is = new FileInputStream(propsFile)) {
					props.load(is);
				} catch (IOException ioe) {
					LOGGER.error(ioe.getMessage());
					final StringWriter sw = new StringWriter();
					final PrintWriter pw = new PrintWriter(sw);
					ioe.printStackTrace(pw);
					LOGGER.error(sw.toString());
				}
			}
		} catch (SecurityException se) {
			LOGGER.error(se.getMessage());
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw);
			se.printStackTrace(pw);
			LOGGER.error(sw.toString());
		}
	}


	/**
	 * 
	 * @param partyName  - name of party
	 * @param iban       - IBAN
	 * @param addrLine1  - address line 1
	 * @param addrLine2  - address line 2
	 * 
	 */
	public void setPartyInfo(
			final String partyName, final String iban, final String addrLine1, final String addrLine2) {
		props.setProperty(PROPERTY_PARTY, partyName);
		props.setProperty(PROPERTY_IBAN, iban);
		props.setProperty(PROPERTY_ADDR_LINE_1, addrLine1);
		props.setProperty(PROPERTY_ADDR_LINE_2, addrLine2);
		if (propsFile != null) {
			try {
				OutputStream os = new FileOutputStream(propsFile);
				props.store(os, "Created by pain.001.001.03 and camt.053.001.02 conversion utilities");
				os.flush();
				os.close();
			} catch (IOException ioe) {
				LOGGER.error(ioe.getMessage());
				final StringWriter sw = new StringWriter();
				final PrintWriter pw = new PrintWriter(sw);
				ioe.printStackTrace(pw);
				LOGGER.error(sw.toString());
			}
		}
	}

	public void personalIbans(final String personalIbansList) {
		props.setProperty(PERSONAL_IBANS, personalIbansList);
	}

	public boolean empty() {
		return StringUtils.isBlank(props.getProperty(PROPERTY_PARTY)) ||
				StringUtils.isBlank(props.getProperty(PROPERTY_IBAN)) ||
				StringUtils.isBlank(props.getProperty(PROPERTY_ADDR_LINE_1)) ||
				StringUtils.isBlank(props.getProperty(PROPERTY_ADDR_LINE_2));
	}

	public String tz() {
		return "Europe/Ljubljana";
	}

	public String currency() {
		return "EUR";
	}

	public String bic() {
		return "REVOLT21";
	}

	public String branch() {
		return "Revolut Payments UAB";
	}

	public String branchCountry() {
		return "LT";
	}

	public String iban() {
		return props.getProperty(PROPERTY_IBAN);
	}

	public String partyName() {
		return props.getProperty(PROPERTY_PARTY);
	}

	public String partyCountry() {
		return "SI";
	}

	public String addressLine1() {
		return props.getProperty(PROPERTY_ADDR_LINE_1);
	}

	public String addressLine2() {
		return props.getProperty(PROPERTY_ADDR_LINE_2);
	}

	public String personalIbans() {
		return props.getProperty(PERSONAL_IBANS);
	}

}
