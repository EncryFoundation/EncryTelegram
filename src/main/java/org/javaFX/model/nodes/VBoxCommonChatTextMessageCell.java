package org.javaFX.model.nodes;

import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.javaFX.model.JMessage;
import org.javaFX.util.RandomChooser;

public class VBoxCommonChatTextMessageCell extends VBoxDialogTextMessageCell{

    private int indent = 70;

    private Circle authorCircle ;
    private Text abbreviationText;

    public VBoxCommonChatTextMessageCell(JMessage jMessage) {
        super(jMessage);
    }

    public VBoxCommonChatTextMessageCell(JMessage jMessage, int parentWidth) {
        super(jMessage, parentWidth);
    }

    @Override
    protected void initNodes(JMessage jMessage) {
        super.initNodes(jMessage);
        if(!jMessage.isMine()){
            initAuthorCircle(jMessage);
            initAbbreviationText(jMessage);
        }
    }

    @Override
    protected void setNodesToRootPane() {
        if(authorCircle != null && abbreviationText!= null){
            getRootPane().getChildren().add(authorCircle);
            getRootPane().getChildren().add(abbreviationText);
        }
        super.setNodesToRootPane();
    }

    @Override
    protected void initContentNode(JMessage jMessage) {
         setContentLabel(new Label() );
        String textContent = jMessage.getContent().toString()
                .substring(jMessage.getContent().toString().indexOf(":")+2);
        getContentLabel().setText(textContent);
        getContentLabel().setWrapText(true);
        int multiplier =
                textContent.length()%40 == 0 ? textContent.length()/40: (textContent.length()/40) +1;
        double width = getParentWidth() - (getParentWidth() / 3) ;
        double height = 27 * multiplier;
        if( jMessage.isMine() ){
            getContentLabel().setLayoutX(getParentWidth() / 3 );
            getContentLabel().setLayoutY(1);
            getContentLabel().setTextFill(Color.WHITE);
        }
        else{
            getContentLabel().setLayoutX(1 + indent);
            getContentLabel().setLayoutY(1);
            getContentLabel().setTextFill(Color.BLACK);
        }
        getContentLabel().setPrefSize(width - 10 , height);
        setContentNode( getContentLabel());
    }

    @Override
    protected void initMessageRectangle(JMessage jMessage) {
        Rectangle messageRectangle = new Rectangle();
        messageRectangle.setWidth(getCellWidth() - 13);
        messageRectangle.setHeight(getCellHeight());
        messageRectangle.setLayoutY(0);
        double ownerIndent = getParentWidth()/3;
        double otherIndent = indent + getCellHeight()/2;
        setFigureProperties(jMessage, messageRectangle, ownerIndent, otherIndent);
        setMessageRectangle(messageRectangle);
    }

    @Override
    protected void initBorderRectangle(JMessage jMessage) {
        Rectangle borderRectangle = new Rectangle();
        borderRectangle.setWidth(3);
        borderRectangle.setHeight(getCellHeight()-26);
        borderRectangle.setLayoutY(0);
        double ownerIndent = getParentWidth()/3;
        double otherIndent = indent;
        setFigureProperties(jMessage, borderRectangle, ownerIndent, otherIndent);
        setBorderRectangle(borderRectangle);
    }

    @Override
    protected void initOuterTopCircle(JMessage jMessage) {
        initOuterCircle(jMessage, 13);
    }

    @Override
    protected void initOuterBotCircle(JMessage jMessage) {
        initOuterCircle(jMessage, getCellHeight()-13);
    }

    protected Circle initOuterCircle(JMessage jMessage, double layoutY){
        Circle outerCircle = new Circle();
        outerCircle.setRadius(13);
        outerCircle.setLayoutY(layoutY);
        double ownerIndent = getParentWidth()/3;
        double otherIndent =  getCellWidth() + 1 + indent;
        setFigureProperties(jMessage, outerCircle, ownerIndent, otherIndent );
        return outerCircle;
    }

    @Override
    protected void initInnerCircle(JMessage jMessage) {
        Circle innerCircle = new Circle();
        innerCircle.setRadius(13);
        innerCircle.setLayoutY(13);
        double ownerIndent = getParentWidth() - 13;
        double otherIndent = indent + 13  ;
        setFigureProperties(jMessage, innerCircle, ownerIndent, otherIndent);
        setInnerCircle(innerCircle);
    }

    @Override
    protected void initAngleRectangle(JMessage jMessage) {
        Rectangle miniRectangle = new Rectangle();
        miniRectangle.setWidth(13);
        miniRectangle.setHeight(getCellHeight()-13);
        miniRectangle.setLayoutY(getCellHeight()/2);
        double ownerIndent = getParentWidth() - getCellHeight()/2;
        double otherIndent =  indent ;
        setFigureProperties(jMessage, miniRectangle, ownerIndent, otherIndent );
        setAngleRectangle(miniRectangle);
    }

    protected void setFigureProperties(JMessage jMessage, Shape shape, double ownerIndent, double otherIndent){
        if(jMessage.isMine()){
            shape.setFill(Color.valueOf(getYouMessagesBackgroundColor()));
            shape.setLayoutX(ownerIndent);
        }
        else{
            shape.setFill(Color.valueOf(getOtherMessageBackgroundColor()));
            shape.setLayoutX(otherIndent);
        }
    }

    private void initAuthorCircle(JMessage jMessage){
        authorCircle = new Circle();
        authorCircle.setRadius(13);
        authorCircle.setLayoutY(13);
        authorCircle.setFill(RandomChooser.getRandomColor());
        authorCircle.setLayoutX(30);
    }

    private void initAbbreviationText(JMessage jMessage){
        abbreviationText = new Text();
        abbreviationText.setLayoutX(38);
        abbreviationText.setLayoutY(16);
        String [] strings = jMessage.getAuthor().split(" ");
        String result = " ";
        if(strings.length > 1){
            result = ""+strings[0].charAt(0)+ strings[1].charAt(0);
        }
        else {
            result += ""+strings[0].charAt(0);
        }
        abbreviationText.setText(result.toUpperCase());
        abbreviationText.setFont(Font.font("Times New Roman", FontWeight.BOLD ,16) );
        abbreviationText.setFill(Color.WHITE);
    }

}