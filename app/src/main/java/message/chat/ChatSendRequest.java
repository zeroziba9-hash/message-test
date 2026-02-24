package message.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChatSendRequest {

    @NotBlank(message = "보내는 사람은 필수입니다.")
    @Size(max = 30, message = "보내는 사람 이름은 30자를 넘을 수 없습니다.")
    private String sender;

    @NotBlank(message = "메시지 내용은 필수입니다.")
    @Size(max = 5_000_000, message = "메시지는 5,000,000자 이하여야 합니다.")
    private String content;

    public ChatSendRequest() {
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
