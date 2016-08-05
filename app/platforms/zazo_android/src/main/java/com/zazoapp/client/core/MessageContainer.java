package com.zazoapp.client.core;

import com.zazoapp.client.model.IncomingMessage;
import com.zazoapp.client.model.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Message container may contain messages of one type and class ordered in a chain
 * Provides util methods to handle such containers
 * Created by skamenkovych@codeminders.com on 8/4/2016.
 */
public class MessageContainer<T extends Message> {
    private List<T> messages = new ArrayList<>();
    private MessageContainer() {}
    public static <T extends Message> List<MessageContainer<T>> splitToMessageContainer(List<T> messages) {
        List<MessageContainer<T>> messageContainerList = new ArrayList<>();
        MessageContainer<T> c = new MessageContainer<>();
        T previous;
        for (T message : messages) {
            if (messageContainerList.isEmpty()) {
                c.messages.add(message);
                messageContainerList.add(c);
            } else {
                previous = c.messages.get(0);
                MessageType cType = MessageType.get(message);
                MessageType previousType = MessageType.get(previous);
                if (cType.equals(previousType) && !MessageType.VIDEO.equals(cType)) {
                    c.messages.add(message);
                } else {
                    c = new MessageContainer<>();
                    c.messages.add(message);
                    messageContainerList.add(c);
                }
            }
        }
        return messageContainerList;
    }

    public MessageType getType() {
        if (messages.isEmpty()) {
            return null;
        }
        return MessageType.get(messages.get(0));
    }

    public int getSize() {
        return messages.size();
    }

    public T getAt(int index) {
        return messages.get(index);
    }

    public static <T extends Message> String getNextMessageIdInList(String id, List<MessageContainer<T>> messages){
        boolean found = false;
        for (MessageContainer<T> container : messages) {
            for (T message : container.messages) {
                if (found) {
                    return message.getId();
                }
                if (message.getId().equals(id))
                    found = true;
            }
        }
        // As videoList may not contain videoId at all, for example it gets deleted during playing,
        // or between stop and start methods of player we decided to play first item from the list if it is
        if (!found) {
            return getFirstMessageIdInList(messages);
        }
        return null;
    }

    public static <T extends Message> String getFirstMessageIdInList(List<MessageContainer<T>> messages){
        if(messages.size()==0)
            return null;
        else
            return messages.get(0).getAt(0).getId();
    }

    public static <T extends Message> int getNextMessagePositionInList(String id, List<MessageContainer<T>> messages) {
        if (messages.size() == 0 || messages.get(messages.size() - 1).getAt(0).getId().equals(id)) {
            return -1;
        }
        for (int i = 0; i < messages.size() - 1; i++) {
            MessageContainer<T> container = messages.get(i);
            for (T message : container.messages) {
                if (message.getId().equals(id)) {
                    return i + 1;
                }
            }
        }
        return 0;
    }

    public static <T extends Message> int getCurrentMessagePositionInList(String id, List<MessageContainer<T>> messages) {
        if (messages.size() == 0) {
            return -1;
        }
        for (int i = 0; i < messages.size(); i++) {
            MessageContainer<T> container = messages.get(i);
            for (T message : container.messages) {
                if (message.getId().equals(id)) {
                    return i;
                }
            }
        }
        return 0;
    }
}
