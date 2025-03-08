package com.jj.chat.controller;

import com.jj.chat.dto.ChatDto;
import com.jj.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.ArrayList;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {
    private final SimpMessageSendingOperations template;
    private final ChatService chatService;

    /**
     * MessageMapping을 통해 webSocket으로 들어오는 메시지를 발신 처리한다.
     * 이때 클라이언트에서는 /pub/chat/message로 요청하게되고 이것을 Controller가 받아서 처리
     * 처리가 완료되면 /sub/chat/room/roomId 로 메시지가 전송된다.
     */
    @MessageMapping("/chat/enterUser")
    public void enterUser(@Payload ChatDto chatDto, SimpMessageHeaderAccessor headerAccessor) {
        // 채팅방 유저 + 1
        chatService.increaseUserCnt(chatDto.getRoomId());

        // 채팅방에 유저 추가 및 UserUUID 반환
        String userUUID = chatService.addUser(chatDto.getRoomId(), chatDto.getSender());

        // 반환 결과를 socket session 에 userUUID 로 저장
        headerAccessor.getSessionAttributes().put("userUUID", userUUID);
        headerAccessor.getSessionAttributes().put("roomId", chatDto.getRoomId());

        chatDto.setMessage(chatDto.getSender() + " 님이 입장하셨습니다.");
        template.convertAndSend("/sub/chat/room/" + chatDto.getRoomId(), chatDto);

    }

    // 해당유저
    @MessageMapping("/chat/sendMessage")
    public void sendMessage(@Payload ChatDto chatDto, SimpMessageHeaderAccessor headerAccessor) {
        log.info("chat : {}", chatDto);
        template.convertAndSend("/sub/chat/room/" + chatDto.getRoomId(), chatDto);
    }

    // 유저 퇴장 시에는 EventListener 을 통해서 유저 퇴장을 확인
    @EventListener
    public void webSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();

        // stomp 세션에 있던 uuid와 roomId를 확인해서 채팅방 유저  리스트와 room에서 해당유저 삭제
        String userUUID = (String) sessionAttributes.get("userUUID");
        String roomId = (String) sessionAttributes.get("roomId");

        // 채팅방 유저 -1
        chatService.decreaseUserCnt(roomId);

        String username = chatService.getUserName(roomId, userUUID);
        chatService.delUser(roomId, userUUID);

        if (username != null) {
            log.info("User Disconnected : " + username);

            // builder 어노테이션 활용
            ChatDto chat = ChatDto.builder()
                    .type(ChatDto.MessageType.LEAVE)
                    .sender(username)
                    .message(username + " 님이 퇴장하셨습니다.")
                    .build();

            template.convertAndSend("/sub/chat/room/" + roomId, chat);
        }
    }

    // 채팅에 참여한 유저 리스트 반환
    @GetMapping("/chat/userlist")
    @ResponseBody
    public ArrayList<String> userList(String roomId) {

        return chatService.getUserList(roomId);
    }

    // 채팅에 참여한 유저 닉네임 중복 확인
    @GetMapping("/chat/duplicateName")
    @ResponseBody
    public String isDuplicateName(@RequestParam("roomId") String roomId, @RequestParam("username") String username) {

        // 유저 이름 확인
        String userName = chatService.isDuplicateName(roomId, username);
        log.info("동작확인 {}", userName);

        return userName;
    }
}
