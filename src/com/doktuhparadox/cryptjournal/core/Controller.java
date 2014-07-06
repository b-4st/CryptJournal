package com.doktuhparadox.cryptjournal.core;

import com.doktuhparadox.cryptjournal.core.option.OptionManager;
import com.doktuhparadox.cryptjournal.util.NodeState;
import com.doktuhparadox.easel.control.keyboard.KeySequence;
import com.doktuhparadox.easel.utils.Clippy;
import com.doktuhparadox.easel.utils.FXMLWindow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.web.HTMLEditor;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.DialogStyle;
import org.controlsfx.dialog.Dialogs;

import java.io.File;
import java.util.Optional;

public class Controller {

    @FXML
    private Button createEntryButton;
    @FXML
    private Button deleteEntryButton;
    @FXML
    private Button openButton;
    @FXML
    private Button optionsButton;
    @FXML
    private Button saveButton;
    @FXML
    private Label journalEntryNameLabel;
    @FXML
    private ListView<JournalEntry> journalEntryListView;
    @FXML
    private HTMLEditor journalContentEditor;

    private static final File journalDir = new File("Journals/");

    @FXML
    protected void initialize() {
        OptionManager.initialize();
        journalEntryListView.setCellFactory(listView -> new JournalEntryListCellFactory());
        if (!journalDir.exists() && journalDir.mkdir()) System.out.println("Created journal entry directory");
        this.attachListeners();
        this.refreshListView();
        //Prevent exceptions
        if (journalEntryListView.getItems().size() == 0) {
            openButton.setDisable(true);
            deleteEntryButton.setDisable(true);
        }
    }

    private void attachListeners() {
        journalEntryListView.setOnKeyPressed(keyEvent -> {
            if (this.getSelectedEntry() != null) {
                if (keyEvent.getCode().equals(KeyCode.ENTER)) this.onOpenButtonPressed();
                if (keyEvent.getCode().equals(KeyCode.DELETE)) this.onDeleteButtonPressed();
            }
        });

        journalEntryListView.itemsProperty().addListener(observable -> {
            if (journalEntryListView.getItems().size() == 0) {
                NodeState.disable(openButton);
                NodeState.disable(deleteEntryButton);
            } else {
                NodeState.enable(openButton);
                NodeState.enable(deleteEntryButton);
            }
        });

        createEntryButton.setOnAction(event -> this.onCreateButtonPressed());
        saveButton.setOnAction(event -> this.onSaveButtonPressed());
        deleteEntryButton.setOnAction(event -> this.onDeleteButtonPressed());
        openButton.setOnAction(event -> this.onOpenButtonPressed());
        optionsButton.setOnAction(event -> this.onOptionsButtonPressed());

        //Easter eggs
        KeyCode[] delimiters = {KeyCode.SPACE, KeyCode.BACK_SPACE};
        new KeySequence(journalContentEditor, () -> Clippy.playSound("/resources/sound/smoke_weed_erryday.wav"), "WEED", delimiters).attach();
        new KeySequence(journalContentEditor, () -> new FXMLWindow(getClass().getResource("Doge.fxml"), "Doge", 510, 385, false).spawn(), "DOGE", delimiters).attach();
    }

    //**********Button event methods**********\\
    private void onCreateButtonPressed() {
        Optional input = this.createDialog("Create new entry", "Enter entry name").showTextInput();

        if (!input.equals(Optional.empty())) {
            JournalEntry newEntry = new JournalEntry(input.toString().replace("Optional[", "").replace("]", ""));
            this.refreshListView();
            NodeState.enable(saveButton);
            NodeState.enable(journalContentEditor);
            NodeState.enable(deleteEntryButton);
            NodeState.disable(createEntryButton);
            NodeState.disable(openButton);
            NodeState.disable(journalEntryListView);
            journalEntryListView.getSelectionModel().select(newEntry);
            journalEntryNameLabel.setText(newEntry.getName());
        }
    }

    private void onOpenButtonPressed() {
        String decodedContent = this.getSelectedEntry().read(this.promptForPassword());

        if (decodedContent.equals("BAD_PASSWORD")) {
            this.createDialog("Error", "Incorrect password.").showError();
            return;
        }

        journalContentEditor.setHtmlText(decodedContent);
        NodeState.enable(saveButton);
        NodeState.enable(journalContentEditor);
        NodeState.disable(journalEntryListView);
        NodeState.disable(createEntryButton);
        NodeState.disable(deleteEntryButton);
        journalEntryNameLabel.setText(this.getSelectedEntry().getName());
    }

    private void onSaveButtonPressed() {
        String password = this.promptForPassword();
        if (password == null) return;

        this.getSelectedEntry().write(journalContentEditor.getHtmlText(), password);
        NodeState.enable(createEntryButton);
        NodeState.enable(openButton);
        NodeState.enable(journalEntryListView);
        NodeState.disable(journalContentEditor);
        NodeState.disable(saveButton);
        journalEntryNameLabel.setText("");
        journalContentEditor.setHtmlText("");
    }

    private void onDeleteButtonPressed() {
        if (this.createDialog("Delete entry?", "Are you sure you want to delete this entry?").showConfirm() == Dialog.Actions.YES) {
            this.getSelectedEntry().delete();
            this.refreshListView();
            journalEntryListView.getSelectionModel().select(-1);

            if (journalEntryListView.getItems().size() == 0) {
                NodeState.disable(openButton);
                NodeState.disable(deleteEntryButton);
            }

            journalEntryNameLabel.setText("");

            if (!journalContentEditor.isDisabled()) {
                NodeState.enable(createEntryButton);
                NodeState.enable(openButton);
                NodeState.disable(saveButton);
                NodeState.disable(journalContentEditor);
                journalContentEditor.setHtmlText("");
            }
        }
    }

    private void onOptionsButtonPressed() {
        FXMLWindow optionsWindow = new FXMLWindow(getClass().getResource("option/OptionWindow.fxml"), "Options", 346, 372, false);
        optionsWindow.spawn();
    }
    //**********Section end, dog**********\\


    private void refreshListView() {
        ObservableList<JournalEntry> entries = FXCollections.observableArrayList();

        //noinspection ConstantConditions
        for (File file : journalDir.listFiles()) {
            if (file != null && file.getName().endsWith(".journal"))
                entries.add(new JournalEntry(file.getName().replace(".journal", "")));
        }

        journalEntryListView.setItems(entries);
    }

    private String promptForPassword() {
        String password;

        while ((password = this.createDialog("Enter password", "Input password for this entry\n(16 chars max)").showTextInput().toString().replace("Optional[", "").replace("]", ""))
                .length() > 16 || password.length() < 16 || password.length() == 0) {

            if (password.equals("Optional.empty")) {
                return null;
            } else if (password.length() > 16) {
                this.createDialog("Error", "Password is too long.").showError();
            } else if (password.length() < 16) {
                while (password.length() < 16) password += "=";
                break;
            }
        }

        return password;
    }

    private Dialogs createDialog(String title, String message) {
        return Dialogs.create().masthead(null).style(DialogStyle.NATIVE).title(title).message(message);
    }

    private JournalEntry getSelectedEntry() {
        return journalEntryListView.getSelectionModel().getSelectedItem();
    }
}
