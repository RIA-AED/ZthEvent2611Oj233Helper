package red.ria.zthevent2611.message;

import java.util.List;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import red.ria.zthevent2611.config.Announcements;
import red.ria.zthevent2611.data.NoticeStore;

/**
 * 发送聊天栏提示，并负责把 [不再提示] 按钮接到 {@link NoticeStore}。
 *
 * <p>按钮使用 Paper 的点击回调（Adventure {@link ClickCallback}），点击后无需执行命令即可
 * 记录玩家选择。
 */
public final class AnnouncementService {

    private final Plugin plugin;
    private final NoticeStore noticeStore;
    private volatile Announcements announcements;
    private volatile Component dismissButton;

    public AnnouncementService(Plugin plugin, NoticeStore noticeStore) {
        this.plugin = plugin;
        this.noticeStore = noticeStore;
    }

    /**
     * 应用（或热重载后重新应用）提示配置。
     */
    public void apply(Announcements next) {
        this.announcements = next;
        ClickCallback<Audience> callback = audience -> {
            if (!(audience instanceof Player player)) {
                return;
            }
            noticeStore.hide(player.getUniqueId());
            plugin.getLogger().info(() -> "玩家 " + player.getName() + " 点击了 [不再提示]");
            player.sendMessage(next.dismissConfirmed());
        };
        ClickEvent click = ClickEvent.callback(callback, options -> options.uses(1));
        this.dismissButton = next.dismissText()
                .hoverEvent(HoverEvent.showText(next.dismissHover()))
                .clickEvent(click);
    }

    /**
     * 向玩家发送一次完整提示。
     *
     * <p>应在玩家所属的 EntityScheduler 线程上调用。
     */
    public void send(Player player) {
        Announcements current = announcements;
        if (current == null) {
            return;
        }
        List<Component> lines = current.compose(dismissButton,
                noticeStore.isHidden(player.getUniqueId()));
        for (Component line : lines) {
            player.sendMessage(line);
        }
    }
}
