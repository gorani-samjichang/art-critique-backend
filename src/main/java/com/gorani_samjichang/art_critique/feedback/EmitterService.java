package com.gorani_samjichang.art_critique.feedback;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class EmitterService {
    private final EmitterRepository emitterRepository;
    private static final Long DEFAULT_TIMEOUT = 120L * 1000 * 60; // SSE 유효시간

    public SseEmitter connection(String serialNumber, HttpServletResponse response) {

        String userid = serialNumber; // 로그인 정보를 기반으로 만들어야하는 곳이다.(로그인을 구현하지않아서 user라고 고정함)
        String id = userid + "@" + System.currentTimeMillis(); // 데이터 유실 시점 파악 위함

        // 클라이언트의 sse 연결 요청에 응답하기 위한 SseEmitter 객체 생성
        // 유효시간 지정으로 시간이 지나면 클라이언트에서 자동으로 재연결 요청함
        SseEmitter emitter = emitterRepository.save(id, new SseEmitter(DEFAULT_TIMEOUT));
        response.setHeader("X-Accel-Buffering", "no"); // NGINX PROXY 에서의 필요설정 불필요한 버퍼링방지

        // SseEmitter 의 완료/시간초과/에러로 인한 전송 불가 시 sseEmitter 삭제
        emitter.onCompletion(() -> emitterRepository.deleteAllStartByWithId(id));
        emitter.onTimeout(() -> emitterRepository.deleteAllStartByWithId(id));
        emitter.onError((e) -> emitterRepository.deleteAllStartByWithId(id));

        // 연결 직후, 데이터 전송이 없을 시 503 에러 발생. 에러 방지 위한 더미데이터 전송
        sendToClient(emitter, id, "연결되었어요!");

        return emitter;
    }
    public void sendToClient(SseEmitter emitter, String id, Object data) {

        try {
            emitter.send(SseEmitter.event()
                    .name("sse")
                    .data(data));
        } catch (IOException e) {
            emitterRepository.deleteAllStartByWithId(id);
        }
    }
}
