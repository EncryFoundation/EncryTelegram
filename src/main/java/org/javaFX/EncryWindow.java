package org.javaFX;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.javaFX.controller.DataHandler;
import org.javaFX.model.JUserState;
import org.javaFX.util.DelayAuthentication;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class EncryWindow extends Application {

    public final static int afterInitializationWidth = 920;
    public final static int afterInitializationHeight = 645;

    private Stage primaryStage;
    private BorderPane rootLayout;

    private final String windowTitle = "ENCRY Telegram Client";
    private final String pathToLogoImage = "file:src/main/resources/images/logo.png";

    private final static String pathToRootLayout = "view/rootLayout.fxml";

    public final static String pathToEnterPhoneNumberWindowFXML = "view/enterPhoneNumberWindow.fxml";
    public final static String pathToEnterVerificationCodeWindowFXML = "view/enterVerificationCodeWindow.fxml";
    public final static String pathToEnterPasswordWindowFXML = "view/enterPasswordWindow.fxml";
    public final static String pathToChatsWindowFXML = "view/chatsWindow.fxml";
    public final static String pathToCreateNewCommunityWindowFXML = "view/createNewLocalCommunity.fxml";
    public final static String pathToEmptyCommunitiesListWindowFXML = "view/emptyCommunitiesListWindow.fxml";
    public final static String pathToCommunitiesListWindowFXML = "view/communitiesListWindow.fxml";

    public final static String pathToStartWindowFXML = "view/startWindow.fxml";

    private DataHandler rootLayoutHandler;

    private DataHandler currentController;

    public static AtomicReference<JUserState> state = new AtomicReference<>(new JUserState());

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        initBasicFields(primaryStage);
        initRootLayout();
        launchStartWindow();
        primaryStage.show();
    }

    public void launchStartWindow(){
        launchWindowByPathToFXML(pathToStartWindowFXML);
        new DelayAuthentication(this, 5000).start();
    }

    private void initBasicFields(Stage primaryStage){
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle(windowTitle);
        this.primaryStage.getIcons().add(new Image(pathToLogoImage));
    }

    private void initRootLayout() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(EncryWindow.class.getResource(pathToRootLayout));
            rootLayout = loader.load();
            initRootController(loader);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initRootController(FXMLLoader loader){
        rootLayoutHandler = loader.getController();
        Scene scene = new Scene(rootLayout);
        primaryStage.setScene(scene);
        updateController(loader);
        primaryStage.show();
    }

    public void launchWindowByPathToFXML(String pathToTemplate){
        try{
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(EncryWindow.class.getResource(pathToTemplate));
            AnchorPane startOverview = loader.load();
            rootLayout.setCenter(startOverview);
            updateController(loader);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void launchWindowByPathToFXML(String pathToTemplate, int newWidth, int newHeight){
        try{
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(EncryWindow.class.getResource(pathToTemplate));
            AnchorPane startOverview = loader.load();
            rootLayout.setCenter(startOverview);
            primaryStage.setHeight(newHeight);
            primaryStage.setWidth(newWidth);
            updateController(loader);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateController(FXMLLoader loader){
        currentController = loader.getController();
        currentController.setUserStateRef(state);
        currentController.setEncryWindow(this);
        currentController.setStage(primaryStage);
        currentController.setRootLayout(rootLayout);
    }

    public DataHandler getCurrentController(){
        if (currentController != null){
            return currentController;
        }
        return null;
    }

    public Stage getPrimaryStage(){
        return primaryStage;
    }

}