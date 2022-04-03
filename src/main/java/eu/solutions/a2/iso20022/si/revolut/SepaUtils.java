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
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.DefaultWindowManager;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.dialogs.FileDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

public class SepaUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(SepaUtils.class);

	private static final TerminalSize LABEL_SIZE = new TerminalSize(20, 1);
	private static final TerminalSize TEXTBOX_SIZE = new TerminalSize(54, 1);
	private static final TerminalSize TEXTBOX_SIZE_2L = new TerminalSize(54, 2);
	private static final TerminalSize BUTTON_SIZE = new TerminalSize(38, 2);

	final MultiWindowTextGUI gui;
	final Window window;
	final PropertiesManager pm;

	final Panel panelMenu = new Panel();
	final Panel panelSetup = new Panel();
	final Panel panelRun = new Panel();
	final Button btnInput = new Button("");
	final Button btnOutput = new Button("");
	boolean runMode;

	SepaUtils() {
		Screen screen = null;
		try {
			// Setup terminal and screen layers
			final Terminal terminal = new DefaultTerminalFactory().createTerminal();
			screen = new TerminalScreen(terminal);
			screen.startScreen();
		
		} catch (IOException ioe) {
			LOGGER.error(ioe.getMessage());
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw);
			ioe.printStackTrace(pw);
			LOGGER.error(sw.toString());
			System.exit(1);
		}
		gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLUE));
		window = new BasicWindow("Revolut SEPA SI support utility");

		pm = new PropertiesManager();

		buildPanelMenu();
		buildPanelSetup();
		buildPanelRun();
	}

	private void buildPanelMenu() {
		panelMenu.setLayoutManager(new LinearLayout(Direction.VERTICAL));

		final Button btnPaymentBatch = new Button("Convert from pain.001.001.03");
		btnPaymentBatch.addListener(btn -> setPanelRunVisible(true));
		panelMenu.addComponent(btnPaymentBatch);

		final Button btnStatement = new Button("Convert to camt.053.001.02");
		btnStatement.addListener(btn -> setPanelRunVisible(false));
		panelMenu.addComponent(btnStatement);

		final Button btnSetup = new Button("Setup");
		btnSetup.addListener(btn -> setPanelSetupVisible());
		panelMenu.addComponent(btnSetup);

		final Button btnExit = new Button("Exit");
		btnExit.addListener(btn -> System.exit(0));
		panelMenu.addComponent(btnExit);
	}

	private void buildPanelSetup() {
		panelSetup.setLayoutManager(new GridLayout(2));
		//Row 1
		final Label lblPartyName = new Label("Company Name");
		lblPartyName.setPreferredSize(LABEL_SIZE);
		panelSetup.addComponent(lblPartyName);
		final TextBox tbPartyName = new TextBox();
		tbPartyName.setPreferredSize(TEXTBOX_SIZE);
		tbPartyName.setText(pm.partyName());
		panelSetup.addComponent(tbPartyName);
		//Row 2
		final Label lblIban = new Label("Company IBAN");
		lblIban.setPreferredSize(LABEL_SIZE);
		panelSetup.addComponent(lblIban);
		final TextBox tbIban = new TextBox();
		tbIban.setPreferredSize(TEXTBOX_SIZE);
		tbIban.setText(pm.iban());
		panelSetup.addComponent(tbIban);
		//Row 3
		final Label lblAddrLine1 = new Label("Address Line 1");
		lblAddrLine1.setPreferredSize(LABEL_SIZE);
		panelSetup.addComponent(lblAddrLine1);
		final TextBox tbAddrLine1 = new TextBox();
		tbAddrLine1.setPreferredSize(TEXTBOX_SIZE);
		tbAddrLine1.setText(pm.addressLine1());
		panelSetup.addComponent(tbAddrLine1);
		//Row 4
		final Label lblAddrLine2 = new Label("Address Line 2");
		lblAddrLine2.setPreferredSize(LABEL_SIZE);
		panelSetup.addComponent(lblAddrLine2);
		final TextBox tbAddrLine2 = new TextBox();
		tbAddrLine2.setPreferredSize(TEXTBOX_SIZE);
		tbAddrLine2.setText(pm.addressLine2());
		panelSetup.addComponent(tbAddrLine2);
		//Row 5
		final Label lblPersonalIbans = new Label("Personal IBAN's");
		lblPersonalIbans.setPreferredSize(LABEL_SIZE);
		panelSetup.addComponent(lblPersonalIbans);
		final TextBox tbPersonalIbans = new TextBox();
		tbPersonalIbans.setPreferredSize(TEXTBOX_SIZE_2L);
		tbPersonalIbans.setText(pm.personalIbans());
		panelSetup.addComponent(tbPersonalIbans);
		//Row 6
		final Button btnCancel = new Button("Cancel");
		btnCancel.setPreferredSize(BUTTON_SIZE);
		btnCancel.addListener(button -> {
			if (pm.empty()) {
				System.exit(0);
			} else {
				setPanelMenuVisible();
			}
		});
		panelSetup.addComponent(btnCancel);
		final Button btnSave = new Button("Save");
		btnSave.setPreferredSize(BUTTON_SIZE);
		btnSave.addListener(button -> {
			pm.setPartyInfo(tbPartyName.getText(), tbIban.getText(),
					tbAddrLine1.getText(), tbAddrLine2.getText());
			if (StringUtils.isNotBlank(tbPersonalIbans.getText())) {
				pm.personalIbans(tbPersonalIbans.getText());
			}
			setPanelMenuVisible();
		});
		panelSetup.addComponent(btnSave);
	}

	private void buildPanelRun() {
		panelRun.setLayoutManager(new GridLayout(2));
		//Row 1
		btnInput.setPreferredSize(BUTTON_SIZE);
		panelRun.addComponent(btnInput);
		btnOutput.setPreferredSize(BUTTON_SIZE);
		panelRun.addComponent(btnOutput);
		//Row 2
		final Label lblInputLabel = new Label("Source:");
		lblInputLabel.setPreferredSize(LABEL_SIZE);
		panelRun.addComponent(lblInputLabel);
		final Label lblInput = new Label("");
		lblInput.setPreferredSize(TEXTBOX_SIZE_2L);
		panelRun.addComponent(lblInput);
		//Row 3
		final Label lblOutputLabel = new Label("Destination:");
		lblOutputLabel.setPreferredSize(LABEL_SIZE);
		panelRun.addComponent(lblOutputLabel);
		final Label lblOutput = new Label("");
		lblOutput.setPreferredSize(TEXTBOX_SIZE_2L);
		panelRun.addComponent(lblOutput);
		//Row 4
		final Button btnConvert = new Button("Convert");
		btnConvert.setPreferredSize(BUTTON_SIZE);
		panelRun.addComponent(btnConvert);
		final Button btnCancel = new Button("Cancel");
		btnCancel.setPreferredSize(BUTTON_SIZE);
		panelRun.addComponent(btnCancel);

		//Button listeners
		btnInput.addListener(button -> {
			final File choosen = new FileDialogBuilder()
					.setTitle(runMode ? 
							"Source pain.001.001.03 .xml" :
							"Source Revolut .csv statement")
					.setDescription("Choose a source file")
					.setActionLabel("Choose")
					.build()
					.showDialog(gui);
		try {
			if (choosen != null) {
				lblInput.setText(choosen.getCanonicalPath());
				if (StringUtils.isNotBlank(lblInput.getText())) {
					final int lastDot = StringUtils.lastIndexOf(lblInput.getText(), ".");
					final String outputName;
					if (lastDot < 0 || lastDot >= lblInput.getText().length()) {
						outputName = lblInput.getText() + (runMode ? ".csv" : ".xml");
					} else {
						outputName = StringUtils.substring(lblInput.getText(), 0, lastDot) + 
								(runMode ? ".csv" : ".xml");
					}
					lblOutput.setText(outputName);
				}
			}
		} catch (IOException ioe) {
			LOGGER.error(ioe.getMessage());
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw);
			ioe.printStackTrace(pw);
			LOGGER.error(sw.toString());
		}
		});
		btnOutput.addListener(button -> {
			final File choosen = new FileDialogBuilder()
					.setTitle(runMode ? 
							"Destination Revolut .csv" :
							"Destination camt.053.001.02 .xml")
					.setDescription("Choose a source file")
					.setActionLabel("Choose")
					.build()
					.showDialog(gui);
		try {
			if (choosen != null) {
				lblOutput.setText(choosen.getCanonicalPath());
			}
		} catch (IOException ioe) {
			LOGGER.error(ioe.getMessage());
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw);
			ioe.printStackTrace(pw);
			LOGGER.error(sw.toString());
		}
		});
		btnConvert.addListener(button -> {
			if (pm.empty() ) {
				new MessageDialogBuilder()
					.setTitle("Required properties are not set!")
					.setText("Please set required properties in Setup menu!")
					.addButton(MessageDialogButton.Close)
					.build()
					.showDialog(gui);
			} else {
				if (StringUtils.isBlank(lblInput.getText())) {
					new MessageDialogBuilder()
						.setTitle("Source file not set!")
						.setText("Please choose file for conversion!")
						.addButton(MessageDialogButton.Close)
						.build()
						.showDialog(gui);
					window.setFocusedInteractable(btnInput);
				} else if (StringUtils.isBlank(lblOutput.getText())) {
					new MessageDialogBuilder()
						.setTitle("Destination file not set!")
						.setText("Please set destination file!")
						.addButton(MessageDialogButton.Close)
						.build()
						.showDialog(gui);
					window.setFocusedInteractable(btnOutput);
				} else {
					try {
						if (runMode) {
							//CAIN
							final List<String> personalIbans = Arrays.asList(pm.personalIbans().split(","));
							CreditTransferInitiation cti = new CreditTransferInitiation(lblInput.getText());
							cti.write(lblOutput.getText(), personalIbans);
						} else {
							//CAMT
							BankToCustomerStatement revolut = new BankToCustomerStatement(
									pm.tz(),
									pm.currency(),
									lblInput.getText());
							revolut.write(
									pm.partyCountry(),
									pm.addressLine1(),
									pm.addressLine2(),
									pm.partyName(),
									pm.branchCountry(),
									pm.bic(),
									pm.branch(),
									pm.iban(),
									lblOutput.getText());
						}
						lblInput.setText("");
						lblOutput.setText("");
						setPanelMenuVisible();
					} catch (IOException | JAXBException e) {
						LOGGER.error(e.getMessage());
						final StringWriter sw = new StringWriter();
						final PrintWriter pw = new PrintWriter(sw);
						e.printStackTrace(pw);
						LOGGER.error(sw.toString());
					}
				}
			}
		});
		btnCancel.addListener(button -> {
			lblInput.setText("");
			lblOutput.setText("");
			if (pm.empty()) {
				setPanelSetupVisible();
			} else {
				setPanelMenuVisible();
			}
		});
	}

	public void start() {
		if (pm.empty()) {
			setPanelSetupVisible();
		} else {
			setPanelMenuVisible();
		}
		gui.addWindowAndWait(window);
	}

	private void setPanelMenuVisible() {
		panelMenu.setVisible(true);
		panelSetup.setVisible(false);
		panelRun.setVisible(false);
		window.setComponent(null);
		window.setComponent(panelMenu);
	}

	private void setPanelSetupVisible() {
		panelMenu.setVisible(false);
		panelSetup.setVisible(true);
		panelRun.setVisible(false);
		window.setComponent(null);
		window.setComponent(panelSetup);
	}

	private void setPanelRunVisible(final boolean runMode) {
		this.runMode = runMode;
		if (runMode) {
			// PAIN001
			btnInput.setLabel("Choose source pain.001.001.03 .xml file");
			btnOutput.setLabel("Set Revolut payment batch .csv file");
		} else {
			// CAMT053
			btnInput.setLabel("Choose source Revolut statement in .csv format");
			btnOutput.setLabel("Set camt.053.001.02 .xml file");
		}
		panelMenu.setVisible(false);
		panelSetup.setVisible(false);
		panelRun.setVisible(true);
		window.setComponent(null);
		window.setComponent(panelRun);
	}

	public static void main(String[] args) {
		PropertyConfigurator.configure(SepaUtils.class.getResourceAsStream("/log4j.properties"));
		final SepaUtils su = new SepaUtils();
		su.start();
	}

}
