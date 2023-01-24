package com.bluehair.hanghaefinalproject.sse.service;

import com.bluehair.hanghaefinalproject.common.exception.InvalidRequestException;
import com.bluehair.hanghaefinalproject.common.exception.NotFoundException;
import com.bluehair.hanghaefinalproject.member.entity.Member;
import com.bluehair.hanghaefinalproject.sse.dto.ResponseNotificationDto;
import com.bluehair.hanghaefinalproject.sse.entity.Notification;
import com.bluehair.hanghaefinalproject.sse.entity.NotificationType;
import com.bluehair.hanghaefinalproject.sse.entity.RedirectionType;
import com.bluehair.hanghaefinalproject.sse.repository.EmitterRepository;
import com.bluehair.hanghaefinalproject.sse.repository.EmitterRepositoryImpl;
import com.bluehair.hanghaefinalproject.sse.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.bluehair.hanghaefinalproject.common.exception.Domain.SSE;
import static com.bluehair.hanghaefinalproject.common.exception.Layer.SERVICE;
import static com.bluehair.hanghaefinalproject.common.response.error.ErrorCode.*;
import static com.bluehair.hanghaefinalproject.sse.mapper.SseMapStruct.SSE_MAPPER;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final EmitterRepository emitterRepository = new EmitterRepositoryImpl();
    private final NotificationRepository notificationRepository;

    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;

    @Transactional
    public SseEmitter subscribe(String lastEventId, Long memberId) {
        String emitterId = memberId + "_" + System.currentTimeMillis();
        SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));

        emitter.onCompletion(() -> emitterRepository.deleteById(emitterId));
        emitter.onTimeout(() -> emitterRepository.deleteById(emitterId));

        sendToClient(emitter, emitterId, "EventStream Created. [memberId=" + memberId + "]");

        if (!lastEventId.isEmpty()) {
            Map<String, Object> events = emitterRepository.findAllEventCacheStartWithByMemberId(String.valueOf(memberId));
            events.entrySet().stream()
                    .filter(entry -> lastEventId.compareTo(entry.getKey()) < 0)
                    .forEach(entry -> sendToClient(emitter, entry.getKey(), entry.getValue()));
        }

        return emitter;
    }
    @Transactional
    public void send(Member receiver, Member sender, NotificationType notificationType, String content, RedirectionType type, Long typeId, Long postId) {
        Notification notification = notificationRepository.save(new Notification(receiver, notificationType, content, type, typeId, postId, sender));
        String memberId = String.valueOf(receiver.getId());

        Map<String, SseEmitter> sseEmitters = emitterRepository.findAllEmitterStartWithByMemberId(memberId);
        sseEmitters.forEach(
                (key, emitter) -> {
                    emitterRepository.saveEventCache(key, notification);
                    sendToClient(emitter, key, SSE_MAPPER.NotificationtoResponseNotificationDto(notification));
                }
        );
    }

    private void sendToClient(SseEmitter emitter, String emitterId, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(emitterId)
                    .data(data));
        } catch (IOException exception) {
            emitterRepository.deleteById(emitterId);
            throw new InvalidRequestException(SSE, SERVICE, UNHANDLED_SERVER_ERROR);
        }
    }

    @Transactional
    public List<ResponseNotificationDto> getNotificationList(Member member) {
        List<Notification> notificationList = notificationRepository.findAllByReceiverOrderByCreatedAtDesc(member);
        List<ResponseNotificationDto> responseNotificationDtoList= new ArrayList<>();
        for (Notification notification: notificationList) {
            responseNotificationDtoList.add(SSE_MAPPER.NotificationtoResponseNotificationDto(notification));
        }
        return responseNotificationDtoList;
    }

    @Transactional
    public void readNotification(Long notificationid, Member member) {
        Notification notification = notificationRepository.findById(notificationid)
                .orElseThrow(()-> new NotFoundException(SSE, SERVICE, NOTIFICATION_NOT_FOUND));
        if(member.getId().equals(notification.getReceiver().getId())) {
            notification.read();
        }
        notificationRepository.save(notification);
    }
}
