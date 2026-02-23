package message.chat;

import jakarta.validation.constraints.NotBlank;

public class MessageUpdateRequest {

    @NotBlank(message = "content는 필수입니다.")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
