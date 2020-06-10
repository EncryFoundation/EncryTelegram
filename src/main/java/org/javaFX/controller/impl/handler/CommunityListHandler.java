package org.javaFX.controller.impl.handler;

import javafx.beans.property.SimpleLongProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.javaFX.EncryWindow;
import org.javaFX.controller.DataHandler;
import org.javaFX.controller.impl.dialog.CreatePrivateCommonDialogController;
import org.javaFX.controller.impl.dialog.EnterCommunityNameDialogController;
import org.javaFX.model.JLocalCommunity;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CommunityListHandler extends DataHandler {

    @FXML
    private TableView<JLocalCommunity> communitiesTable;

    @FXML
    private TableColumn<JLocalCommunity, Integer> rowNumberColumn;

    @FXML
    private TableColumn<JLocalCommunity, Long> communityID;

    @FXML
    private TableColumn<JLocalCommunity, String> communityNameColumn;

    @FXML
    private TableColumn<JLocalCommunity, Integer> membersNumberColumn;

    private ScheduledExecutorService service;

    private void runDelayedInitialization(){
        service = Executors.newSingleThreadScheduledExecutor();
        service.schedule(() -> updateEncryWindow(getEncryWindow()), 200, TimeUnit.MILLISECONDS);
    }

    public CommunityListHandler() {
        runDelayedInitialization();
    }

    @Override
    public void updateEncryWindow(EncryWindow encryWindow) {
        super.setEncryWindow(encryWindow);
        communitiesTable.setItems(getObservableCommunityList());
        initChatsTable();
    }


    private ObservableList<JLocalCommunity> getObservableCommunityList(){
        ObservableList<JLocalCommunity> observableList = FXCollections.observableArrayList();
        getUserStateRef().get().communities.stream().forEach(community -> observableList.add(new JLocalCommunity(community)));
        return observableList;
    }

    private void initChatsTable(){
        communityID.setCellValueFactory(cellDate -> new SimpleLongProperty(cellDate.getValue().getCommunityID()).asObject() );
        communityNameColumn.setCellValueFactory(cellData -> cellData.getValue().getStringPropertyCommunityName());
        service.shutdown();
    }

//refactor later
    @FXML
    private void toMoreInfoWindowAction(){
        JLocalCommunity localCommunity = communitiesTable.getSelectionModel().getSelectedItem();
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(EncryWindow.class.getResource(EncryWindow.pathToCreateSubmitPrivateChatFXML));
        Stage dialogStage = new Stage();
        try {
            AnchorPane startOverview = loader.load();
            Scene scene = new Scene(startOverview);
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);
            CreatePrivateCommonDialogController controller = loader.getController();
            controller.setEncryWindow(getEncryWindow());
            controller.setDialogStage(dialogStage);
            controller.setLocalCommunity(localCommunity);
            controller.setState(getUserStateRef());
            dialogStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void toMainWindowAction(){
        getEncryWindow().launchWindowByPathToFXML(EncryWindow.pathToMainWindowFXML);
    }

}