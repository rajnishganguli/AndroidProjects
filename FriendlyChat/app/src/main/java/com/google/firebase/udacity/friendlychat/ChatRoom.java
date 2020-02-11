package com.google.firebase.udacity.friendlychat;

public class ChatRoom {

    private String name = "";
    private int messageCount = 0;

    public ChatRoom(String name, int messageCount){
        this.name = name;
        this.messageCount = messageCount;
    }

    public String getName() {
        return name;
    }

    public int getMessageCount() {
        return messageCount;
    }
}
