package red.ria.zthevent2611.flow;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import red.ria.zthevent2611.config.PluginSettings;
import red.ria.zthevent2611.message.AnnouncementService;

/**
 * 管理当前所有玩家的跨服流程。
 *
 * <p>流程状态以玩家 UUID 为键保存；玩家退出或流程被中止后会被移除。
 */
public final class TransferFlowManager {

    private final Plugin plugin;
    private final AnnouncementService announcements;
    private final Supplier<PluginSettings> settings;
    private final Map<UUID, TransferFlow> active = new ConcurrentHashMap<>();

    public TransferFlowManager(Plugin plugin, AnnouncementService announcements,
                               Supplier<PluginSettings> settings) {
        this.plugin = plugin;
        this.announcements = announcements;
        this.settings = settings;
    }

    /**
     * 为玩家开始一次新流程；已在流程中的玩家会先中止旧流程。
     *
     * <p>应在玩家所属的 EntityScheduler 线程上调用。
     */
    public void start(Player player) {
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
        TransferFlow flow = new TransferFlow(plugin, player, current, announcements);
        TransferFlow previous = active.put(player.getUniqueId(), flow);
        if (previous != null) {
            previous.stop();
            plugin.getLogger().info(() -> "玩家 " + player.getName() + " 原有的跨服流程已被新流程替换");
        }
        plugin.getLogger().info(() -> "开始跨服流程：玩家=" + player.getName()
                + "，间隔=" + current.intervalSeconds() + " 秒，最多尝试=" + current.attempts() + " 次");
        flow.start();
    }

    /**
     * 移除玩家的流程状态；应在玩家退出时调用。
     */
    public void stop(Player player) {
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
