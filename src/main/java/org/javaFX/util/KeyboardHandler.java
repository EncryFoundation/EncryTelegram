package org.javaFX.util;


import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

import java.util.concurrent.atomic.AtomicBoolean;

public class KeyboardHandler {
    public static AtomicBoolean[] handleShiftEnterPressed(TextArea sendMessageArea ){
        AtomicBoolean[] keysPressed = new AtomicBoolean[2];
        keysPressed[0] = new AtomicBoolean(false);
        keysPressed[1] = new AtomicBoolean(false);
        sendMessageArea.setOnKeyPressed(keyEvent -> {
            switch (keyEvent.getCode()) {
                case ENTER:
                    keysPressed[0].set(true);
                    break;
                case SHIFT:
                    keysPressed[1].set(true);
                    break;
            }
        });

        sendMessageArea.setOnKeyReleased(keyEvent -> {
            switch (keyEvent.getCode()) {
                case ENTER:
                    keysPressed[0].set(false);
                    break;
                case SHIFT:
                    keysPressed[1].set(false);
                    break;
            }
        });
        return keysPressed;
    }


    public static AtomicBoolean handleEnterPressed(Node button ){
        AtomicBoolean keyPressed = new AtomicBoolean(false);
        button.setOnKeyPressed(keyEvent -> {
            switch (keyEvent.getCode()) {
                case ENTER:
                    keyPressed.set(true);
                    break;
            }
        });

        button.setOnKeyReleased(keyEvent -> {
            switch (keyEvent.getCode()) {
                case ENTER:
                    keyPressed.set(false);
                    break;

            }
        });
        return keyPressed;
    }


}
