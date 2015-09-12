package com.dici.javafx;

import static com.dici.javafx.Settings.strings;
import javafx.scene.Node;

import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialogs;

public class DialogsFactory {
	public static Action showPreFormattedError(Node owner, String titlePropertyName, String mastheadPropertyName, String msgPropertyName) {
		return preformattedDialog(owner,titlePropertyName,mastheadPropertyName,msgPropertyName).showError();
	}
	
	public static Action showError(Node owner, String title, String masthead, String msg) {
		return Dialogs.create()
				.owner(owner)
				.title(title)
				.masthead(masthead)
				.message(msg)
				.showError();
	}
	
	public static Action showPreFormattedWarning(Node owner, String titlePropertyName, String mastheadPropertyName, String msgPropertyName) {
		return preformattedDialog(owner,titlePropertyName,mastheadPropertyName,msgPropertyName).showWarning();
	}
	
	private static Dialogs preformattedDialog(Node owner, String titlePropertyName, String mastheadPropertyName, String msgPropertyName) {
		return Dialogs.create()
				.owner(owner)
				.title(strings.getProperty(titlePropertyName))
				.masthead(strings.getProperty(mastheadPropertyName))
				.message(strings.getProperty(msgPropertyName));
	}
}
