package org.javaFX.controller.impl.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.encryfoundation.tg.javaIntegration.JavaInterMsg;
import org.javaFX.EncryWindow;
import org.javaFX.controller.DialogController;

public class CreatePrivateCommonDialogController extends DialogController {

    @FXML
    private TextField privateChatTitle;

    @FXML
    private void createButtonAction() throws InterruptedException {
        final String secretChatName = privateChatTitle.getText();
        //TODO: insert somewhere
        JavaInterMsg msg = new JavaInterMsg.CreatePrivateGroupChat(getLocalCommunity().getCommunityName());
        getState().get().msgsQueue.put(msg);
        getEncryWindow().launchWindowByPathToFXML(EncryWindow.pathToMainWindowFXML);
        getDialogStage().close();
    }

}