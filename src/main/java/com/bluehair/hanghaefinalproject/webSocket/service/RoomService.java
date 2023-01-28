package com.bluehair.hanghaefinalproject.webSocket.service;

import com.bluehair.hanghaefinalproject.common.exception.Domain;
import com.bluehair.hanghaefinalproject.common.exception.NotFoundException;
import com.bluehair.hanghaefinalproject.member.entity.Member;
import com.bluehair.hanghaefinalproject.member.repository.MemberRepository;
import com.bluehair.hanghaefinalproject.webSocket.dto.response.MessageListDto;
import com.bluehair.hanghaefinalproject.webSocket.dto.response.RoomListDto;
import com.bluehair.hanghaefinalproject.webSocket.entity.ChatMessage;
import com.bluehair.hanghaefinalproject.webSocket.entity.ChatRoom;
import com.bluehair.hanghaefinalproject.webSocket.repository.MessageRepository;
import com.bluehair.hanghaefinalproject.webSocket.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.bluehair.hanghaefinalproject.common.exception.Layer.SERVICE;
import static com.bluehair.hanghaefinalproject.common.response.error.ErrorCode.MEMBER_NOT_FOUND;
import static com.bluehair.hanghaefinalproject.webSocket.mapper.RoomMapStruct.ROOM_MAPPER;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomService {

    private final MemberRepository memberRepository;
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;

    public List<RoomListDto> findAllRoom(String nickname) {

        Member member = memberRepository.findByNickname(nickname).orElseThrow(
                () -> new NotFoundException(Domain.ROOM, SERVICE,MEMBER_NOT_FOUND, "Nickname : " + nickname)
        );

        List<RoomListDto> roomList = new ArrayList<>();

        // 사용자가 member1인 채팅방 조회
        List<ChatRoom> chatRoomList1 = roomRepository.findByMember1_Id(member.getId());

        for (ChatRoom c : chatRoomList1){
            RoomListDto roomListDto = new RoomListDto(c.getRoomId(),c.getMember2().getNickname(), c.getMember2().getProfileImg());
            roomList.add(roomListDto);
        }

        List<ChatRoom> chatRoomList2 = roomRepository.findByMember2_Id(member.getId());

        for (ChatRoom c : chatRoomList2){
            Member chatMember =memberRepository.findById(c.getMember1().getId()).orElseThrow(
                    () -> new NotFoundException(Domain.ROOM, SERVICE,MEMBER_NOT_FOUND, "Nickname : " + nickname)
            );
            RoomListDto roomListDto = new RoomListDto(c.getRoomId(),chatMember.getNickname(), chatMember.getProfileImg());
            roomList.add(roomListDto);
        }
        return roomList;
    }
    @Transactional
    public ChatRoom createRoom(String memberNickname1, String memberNickname2) {

        Member member1 = memberRepository.findByNickname(memberNickname1).orElseThrow(
                () -> new NotFoundException(Domain.ROOM, SERVICE,MEMBER_NOT_FOUND, "Nickname : " + memberNickname2)
        );
        Member member2 = memberRepository.findByNickname(memberNickname2).orElseThrow(
                () -> new NotFoundException(Domain.ROOM, SERVICE,MEMBER_NOT_FOUND, "Nickname : " + memberNickname2)
        );

        ChatRoom chatRoom = ROOM_MAPPER.ChatRoomToRoomDto(member1, member2);

        roomRepository.save(chatRoom);
        return chatRoom;
    }

    public List<MessageListDto> entranceRoom(Long roomId) {

        List<ChatMessage> message = messageRepository.findByChatRoom_RoomIdOrderByCreatedAtDesc(roomId);
        List<MessageListDto> messageList = new ArrayList<>();
        for (ChatMessage m : message){
            MessageListDto messageListDto = new MessageListDto(m.getMessage(), m.getNickname(), m.getProfileImg());
            messageList.add(messageListDto);
        }

        return messageList;
    }
}