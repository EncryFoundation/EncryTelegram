package org.javaFX.model;

import javafx.beans.property.*;

public class JChat {

    private StringProperty title;
    private StringProperty lastMessage;
    private final LongProperty chatId;

    public JChat(StringProperty title, StringProperty lastMessage, LongProperty chatId) {
        this.title = title;
        this.lastMessage = lastMessage;
        this.chatId = chatId;
    }

    public JChat(String titleStr, String lastMessageStr, Long chatId) {
        this(new SimpleStringProperty(titleStr), new SimpleStringProperty(lastMessageStr), new SimpleLongProperty(chatId));
    }

    public StringProperty getTitle() {
        return title;
    }

    public void setTitle(StringProperty title) {
        this.title = title;
    }

    public StringProperty getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(StringProperty lastMessage) {
        this.lastMessage = lastMessage;
    }

    public LongProperty chatIdProperty() {
        return chatId;
    }

    @Override
    public String toString() {
        return "title = " + title.toString() + ", lastMessage = " + lastMessage.toString() + ", chatId = " + chatId.toString() ;
    }
}
