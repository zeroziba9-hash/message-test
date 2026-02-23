package message.chat;

public class ChannelPermission {
    private boolean canRead;
    private boolean canWrite;

    public ChannelPermission() {
    }

    public ChannelPermission(boolean canRead, boolean canWrite) {
        this.canRead = canRead;
        this.canWrite = canWrite;
    }

    public boolean isCanRead() {
        return canRead;
    }

    public void setCanRead(boolean canRead) {
        this.canRead = canRead;
    }

    public boolean isCanWrite() {
        return canWrite;
    }

    public void setCanWrite(boolean canWrite) {
        this.canWrite = canWrite;
    }
}
