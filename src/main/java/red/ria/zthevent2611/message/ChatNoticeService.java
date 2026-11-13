package red.ria.zthevent2611.message;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import red.ria.zthevent2611.config.ChatMessages;

/**
 * 发送聊天栏提示。应在玩家所属的 EntityScheduler 线程上调用。
 */
public final class ChatNoticeService {

    private volatile ChatMessages messages;

    /**
     * 应用（或热重载后重新应用）消息配置。
     */
    public void apply(ChatMessages next) {
        this.messages = next;
    }

    /**
     * 发送「正在进行跨服......」及前后的空行。
     */
    public void sendTransferring(Player player) {
        ChatMessages current = messages;
        if (current == null) {
            return;
        }
        List<Component> lines = current.compose();
        for (Component line : lines) {
            player.sendMessage(line);
        }
    }
}
