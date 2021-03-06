package org.javaFX.controller.impl.handler;

import javafx.animation.AnimationTimer;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;

import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import org.encryfoundation.tg.javaIntegration.FrontMsg;
import org.encryfoundation.tg.javaIntegration.BackMsg;
import org.javaFX.EncryWindow;
import org.javaFX.controller.DataHandler;
import org.javaFX.util.KeyboardHandler;

import java.util.concurrent.atomic.AtomicBoolean;

public class EnterPhoneNumberHandler extends DataHandler {

    @FXML
    private TextField phoneNumberTextField;

    @FXML
    private ImageView nextButtonImg;

    @FXML
    private Label enterPhoneNumberLabel;

    @FXML
    private MenuButton selectCountryMenu;

    @FXML
    private Label countryCodeLabel;

    @FXML
    private MenuItem russianFederationMenuItem;

    @FXML
    private MenuItem belarusMenuItem;

    @FXML
    private Label error;

    public EnterPhoneNumberHandler() {}

    @FXML
    private void handleKeyTyped(){
        if (selectCountryMenu.getText().equals(russianFederationMenuItem.getText())){
            phoneNumberTextField.addEventFilter(KeyEvent.KEY_TYPED, KeyboardHandler.maxLengthHandler(10));
        }
        else if(selectCountryMenu.getText().equals(belarusMenuItem.getText())){
            phoneNumberTextField.addEventFilter(KeyEvent.KEY_TYPED, KeyboardHandler.maxLengthHandler(9));
        }
    }

    @FXML
    private void handleConfirmNumberAction(){
        String phoneNumberStr = phoneNumberTextField.getCharacters().toString();
        if (phoneNumberStr.isEmpty()) error.setText("Empty phone field :( Please enter it!");
        else {
            if (selectCountryMenu.getText().equals(russianFederationMenuItem.getText())) {
                phoneNumberStr = "7" + phoneNumberStr;
            } else if (selectCountryMenu.getText().equals(belarusMenuItem.getText())) {
                phoneNumberStr = "375" + phoneNumberStr;
            }
            getUserStateRef().get().setPhoneNumber(phoneNumberStr);
            try {
                getUserStateRef().get().outQueue.put(new BackMsg.SetPhone(phoneNumberStr));
                FrontMsg nextStep = getUserStateRef().get().inQueue.take();
                if (nextStep.code() == FrontMsg.Codes$.MODULE$.loadPass()) {
                    getEncryWindow().launchWindowByPathToFXML(EncryWindow.pathToEnterPasswordWindowFXML);
                } else if (nextStep.code() == FrontMsg.Codes$.MODULE$.loadVc()) {
                    getEncryWindow().launchWindowByPathToFXML(EncryWindow.pathToEnterVerificationCodeWindowFXML);
                    ((EnterVerificationCodeHandler) getEncryWindow().getCurrentController())
                            .setPhoneNumberLabelText(getUserStateRef().get().getPreparedPhoneNumber());
                } else if (nextStep.code() == FrontMsg.Codes$.MODULE$.loadChats()) {
                    getEncryWindow().launchWindowByPathToFXML(
                            EncryWindow.pathToChatsWindowFXML, EncryWindow.afterInitializationWidth, EncryWindow.afterInitializationHeight
                    );
                } else if (nextStep.code() == FrontMsg.Codes$.MODULE$.error()) {
                    error.setText("Oops error!");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handlePhoneNumberAreaPressed(){
        handleNumberAccepted(nextButtonImg);
    }

    private void handleNumberAccepted(Node node){
        AtomicBoolean[] keysPressed = KeyboardHandler.handleShiftEnterPressed(node);
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if ( keysPressed[0].get() && keysPressed[1].get()   ) {
                    handleConfirmNumberAction();
                }
            }
        }.start();
    }

    @FXML
    private void showPrompt(){
        enterPhoneNumberLabel.setVisible(true);
    }

    @FXML
    private void hidePrompt(){
        enterPhoneNumberLabel.setVisible(false);
    }

    private void setCountryMenuItem(String country, String code, String promptText){
        nextButtonImg.setDisable(false);
        phoneNumberTextField.setDisable(false);
        selectCountryMenu.setText(country);
        countryCodeLabel.setVisible(true);
        countryCodeLabel.setText(code);
        phoneNumberTextField.setText("");
        phoneNumberTextField.setPromptText(promptText);
    }

    @FXML
    private void setRussiaDefault(){
        setCountryMenuItem(russianFederationMenuItem.getText(), "+7", "--- --- -- --");
    }

    @FXML
    private void setBelarusDefault(){
        setCountryMenuItem(belarusMenuItem.getText(), "+375", "-- --- -- --");
    }


}
