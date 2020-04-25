package org.triplea.modules.chat.event.processing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import javax.websocket.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ChatReceivedMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ChatSentMessage;
import org.triplea.modules.chat.Chatters;
import org.triplea.web.socket.WebSocketMessageContext;

@ExtendWith(MockitoExtension.class)
class ChatMessageListenerTest {

  @Mock private Chatters chatters;
  @InjectMocks private ChatMessageListener chatMessageListener;

  @Mock private Session session;
  @Mock private WebSocketMessageContext<ChatSentMessage> messageContext;

  private ArgumentCaptor<ChatReceivedMessage> messageCaptor =
      ArgumentCaptor.forClass(ChatReceivedMessage.class);

  @Test
  @DisplayName("If a player is not in the chatter session, then we do not relay their message")
  void ifPlayerSessionDoesNotExistThenDoNotRelayTheirMessage() {
    when(messageContext.getSenderSession()).thenReturn(session);
    when(chatters.lookupPlayerBySession(session)).thenReturn(Optional.empty());

    chatMessageListener.accept(messageContext);

    verify(messageContext, never()).broadcastMessage(any());
  }

  @Test
  @DisplayName("If a player is in the chatter session, then we do relay their message")
  void ifPlayerSessionDoesExistThenRelayTheirMessage() {
    when(messageContext.getSenderSession()).thenReturn(session);
    when(messageContext.getMessage()).thenReturn(new ChatSentMessage("message"));
    givenChatterSession(
        session,
        ChatParticipant.builder()
            .playerChatId(PlayerChatId.newId().getValue())
            .userName("user-name")
            .build());

    chatMessageListener.accept(messageContext);

    verify(messageContext).broadcastMessage(messageCaptor.capture());
    verifyMessageContents(messageCaptor.getValue());
  }

  private void givenChatterSession(final Session session, final ChatParticipant chatParticipant) {
    when(chatters.lookupPlayerBySession(session)) //
        .thenReturn(Optional.of(chatParticipant));
  }

  private static void verifyMessageContents(final ChatReceivedMessage chatReceivedMessage) {
    assertThat(chatReceivedMessage.getMessage(), is("message"));
    assertThat(chatReceivedMessage.getSender(), is(UserName.of("user-name")));
  }
}