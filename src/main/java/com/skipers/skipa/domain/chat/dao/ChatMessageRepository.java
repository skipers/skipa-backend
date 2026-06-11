package com.skipers.skipa.domain.chat.dao;

import com.skipers.skipa.domain.chat.domain.ChatMessage;
import com.skipers.skipa.domain.chat.domain.ChatTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByTargetTypeAndTargetIdOrderByCreatedAtAsc(ChatTargetType targetType, Long targetId);

    void deleteAllByTargetTypeAndTargetId(ChatTargetType targetType, Long targetId);
}
