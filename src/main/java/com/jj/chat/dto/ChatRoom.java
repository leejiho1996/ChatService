package com.jj.chat.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.UUID;

@Data
public class ChatRoom {
    private String roomId; // 채팅방 id
    private String roomName; // 채팅방 이름
    private long userCount; // 인원
    private HashMap<String ,String> userList = new HashMap<String, String >();

    public static ChatRoom create(String roomName) {
        ChatRoom chatroomDto = new ChatRoom();
        chatroomDto.setRoomId(UUID.randomUUID().toString());
        chatroomDto.setRoomName(roomName);

        return chatroomDto;
    }
}
