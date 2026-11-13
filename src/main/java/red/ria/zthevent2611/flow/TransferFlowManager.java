package red.ria.zthevent2611.flow;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import red.ria.zthevent2611.config.PluginSettings;
import red.ria.zthevent2611.data.NoticeStore;
import red.ria.zthevent2611.message.ChatNoticeService;
import red.ria.zthevent2611.message.NoticeDialogService;

/**
 * 管理玩家的进服引导与跨服流程。
 *
 * <p>流程分两步：先切换到旁观模式并弹出活动说明对话框（已选择「不再提示」的玩家跳过），
 * 玩家确认后才开始执行跨服命令。流程状态以玩家 UUID 为键保存，玩家退出或流程被中止后移除。
 */
public final class TransferFlowManager {

    private final Plugin plugin;
    private final ChatNoticeService chatNotice;
    private final NoticeDialogService noticeDialogs;
    private final NoticeStore noticeStore;
    private final Supplier<PluginSettings> settings;
    private final Map<UUID, TransferFlow> active = new ConcurrentHashMap<>();

    public TransferFlowManager(Plugin plugin, ChatNoticeService chatNotice,
                               NoticeDialogService noticeDialogs, NoticeStore noticeStore,
                               Supplier<PluginSettings> settings) {
        this.plugin = plugin;
        this.chatNotice = chatNotice;
        this.noticeDialogs = noticeDialogs;
        this.noticeStore = noticeStore;
        this.settings = settings;
    }

    /**
     * 开始进服引导：切换旁观模式；未选择过「不再提示」时先弹出提示对话框，确认后开始跨服尝试。
     *
     * <p>应在玩家所属的 EntityScheduler 线程上调用。
     */
    public void begin(Player player) {
        if (!player.isOnline()) {
            return;
        }
        PluginSettings current = settings.get();
        if (current == null) {
            return;
        }
        applyGamemode(player, current);
        if (noticeStore.isHidden(player.getUniqueId())) {
            plugin.getLogger().info(() -> "玩家 " + player.getName()
                    + " 已选择不再提示，直接开始跨服流程");
            startAttempts(player);
            return;
        }
        plugin.getLogger().info(() -> "向玩家 " + player.getName() + " 展示活动提示对话框");
        noticeDialogs.show(player, () -> startAttempts(player));
    }

    /**
     * 展示对话框后开始跨服尝试；已在流程中的玩家会先中止旧流程。
     */
    private void startAttempts(Player player) {
        if (!player.isOnline()) {
            return;
        }
        PluginSettings current = settings.get();
        if (current == null) {
            return;
        }
        if (!current.hasCommands()) {
            plugin.getLogger().warning("flow.commands 为空：玩家 " + player.getName()
                    + " 的跨服流程不会执行任何命令");
        }
        TransferFlow flow = new TransferFlow(plugin, player, current, chatNotice);
        TransferFlow previous = active.put(player.getUniqueId(), flow);
        if (previous != null) {
            previous.stop();
            plugin.getLogger().info(() -> "玩家 " + player.getName() + " 原有的跨服流程已被新流程替换");
        }
        plugin.getLogger().info(() -> "开始跨服流程：玩家=" + player.getName()
                + "，间隔=" + current.intervalSeconds() + " 秒，最多尝试=" + current.attempts() + " 次");
        flow.start();
    }

    private void applyGamemode(Player player, PluginSettings current) {
        GameMode target = current.gamemode();
        if (target == null || player.getGameMode() == target) {
            return;
        }
        GameMode previous = player.getGameMode();
        player.setGameMode(target);
        plugin.getLogger().info(() -> "玩家 " + player.getName() + " 的游戏模式由 " + previous
                + " 切换为 " + target);
    }

    /**
     * 移除玩家的流程状态；应在玩家退出时调用。
     */
    public void stop(Player player) {
        noticeDialogs.forget(player);
        TransferFlow flow = active.remove(player.getUniqueId());
        if (flow != null) {
            flow.stop();
        }
    }

    public boolean isActive(UUID playerId) {
        return active.containsKey(playerId);
    }

    public int activeCount() {
        return active.size();
    }

    /**
     * 插件停用时中止所有流程。
     */
    public void shutdown() {
        active.values().forEach(TransferFlow::stop);
        active.clear();
    }
}

