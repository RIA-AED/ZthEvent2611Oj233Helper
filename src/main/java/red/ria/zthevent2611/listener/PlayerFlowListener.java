package red.ria.zthevent2611.listener;

import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import red.ria.zthevent2611.Permissions;
import red.ria.zthevent2611.config.PluginSettings;
import red.ria.zthevent2611.flow.TransferFlowManager;

/**
 * 玩家进服时开始引导流程，退出时清理流程状态。
 *
 * <p>进服事件不保证在玩家所属的区域线程上触发，因此对玩家状态的改动全部通过
 * {@code player.getScheduler()} 调度到正确的线程执行。
 */
public final class PlayerFlowListener implements Listener {

    private final Plugin plugin;
    private final TransferFlowManager flows;
    private final Supplier<PluginSettings> settings;

    public PlayerFlowListener(Plugin plugin, TransferFlowManager flows,
                              Supplier<PluginSettings> settings) {
        this.plugin = plugin;
        this.flows = flows;
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PluginSettings current = settings.get();
        if (current == null || !current.managed()) {
            // 通用模式：进服不自动触发，由命令打开提示对话框。
            return;
        }
        if (!current.startOnJoin()) {
            return;
        }
        if (player.hasPermission(Permissions.BYPASS)) {
            plugin.getLogger().info(() -> "玩家 " + player.getName() + " 拥有 "
                    + Permissions.BYPASS + "，跳过旁观与跨服流程");
            return;
        }
        player.getScheduler().execute(plugin, () -> {
            PluginSettings latest = settings.get();
            if (latest == null || !latest.managed() || !latest.startOnJoin() || !player.isOnline()) {
                return;
            }
            flows.begin(player);
        }, null, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        flows.stop(event.getPlayer());
    }
}
