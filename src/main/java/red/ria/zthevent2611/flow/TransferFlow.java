package red.ria.zthevent2611.flow;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import red.ria.zthevent2611.config.PluginSettings;
import red.ria.zthevent2611.message.ChatNoticeService;

/**
 * 单个玩家的一次跨服流程：发送聊天提示、按间隔执行命令，超时后踢出。
 *
 * <p>流程在玩家确认进服提示对话框后才创建；所有方法都必须在玩家的 EntityScheduler 线程上执行，
 * 定时任务通过 {@code player.getScheduler()} 调度，因此在 Paper 与 Folia 上都能正确运行。
 */
final class TransferFlow {

    private final Plugin plugin;
    private final Player player;
    private final PluginSettings settings;
    private final ChatNoticeService chatNotice;
    private final UUID playerId;
    private final String playerName;
    private int attemptsDone;
    private volatile boolean finished;

    TransferFlow(Plugin plugin, Player player, PluginSettings settings,
                 ChatNoticeService chatNotice) {
        this.plugin = plugin;
        this.player = player;
        this.settings = settings;
        this.chatNotice = chatNotice;
        this.playerId = player.getUniqueId();
        this.playerName = player.getName();
    }

    /**
     * 开始流程：发送聊天提示、立即执行第一次命令，并安排后续尝试与超时踢出。
     */
    void start() {
        if (finished || !player.isOnline()) {
            return;
        }
        chatNotice.sendTransferring(player);
        runAttempt();
        scheduleTimeout();
    }

    /**
     * 中止流程；已经安排的任务会在执行时自行检查 {@link #finished} 后退出。
     */
    void stop() {
        finished = true;
    }

    private void runAttempt() {
        if (finished || !player.isOnline()) {
            return;
        }
        attemptsDone++;
        List<String> commands = settings.commands();
        for (String template : commands) {
            String command = CommandTemplate.render(template, playerName, playerId);
            try {
                if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)) {
                    plugin.getLogger().warning("跨服命令不存在或无法执行：" + command);
                }
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.WARNING, "执行跨服命令时发生异常：" + command, exception);
            }
        }
        plugin.getLogger().info(() -> "玩家 " + playerName + " 第 " + attemptsDone + "/"
                + settings.attempts() + " 次执行跨服命令");
        scheduleNextAttempt();
    }

    private void scheduleNextAttempt() {
        if (finished || attemptsDone >= settings.attempts()) {
            return;
        }
        player.getScheduler().runDelayed(plugin,
                task -> runAttempt(),
                this::retire,
                settings.intervalTicks());
    }

    private void scheduleTimeout() {
        player.getScheduler().runDelayed(plugin,
                task -> handleTimeout(),
                this::retire,
                settings.timeoutTicks());
    }

    private void handleTimeout() {
        if (finished || !player.isOnline()) {
            return;
        }
        finished = true;
        if (settings.managed()) {
            plugin.getLogger().info(() -> "玩家 " + playerName + " 跨服超时（每 "
                    + settings.intervalSeconds() + " 秒尝试，共 " + settings.attempts() + " 次），已踢出");
            player.kick(settings.kickMessage());
            return;
        }
        plugin.getLogger().warning(() -> "玩家 " + playerName + " 跨服命令已尝试 " + settings.attempts()
                + " 次，仍未离开本服；当前为通用模式（managed=false），不踢出玩家");
    }

    private void retire() {
        finished = true;
    }
}

