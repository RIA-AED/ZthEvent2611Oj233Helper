package red.ria.zthevent2611.flow;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import red.ria.zthevent2611.config.PluginSettings;
import red.ria.zthevent2611.message.AnnouncementService;

/**
 * 单个玩家的一次跨服流程：切换游戏模式、发送提示、按间隔执行命令，超时后踢出。
 *
 * <p>所有方法都必须在玩家的 EntityScheduler 线程上执行；定时任务通过
 * {@code player.getScheduler()} 调度，因此在 Paper 与 Folia 上都能正确运行。
 */
final class TransferFlow {

    private final Plugin plugin;
    private final Player player;
    private final PluginSettings settings;
    private final AnnouncementService announcements;
    private final UUID playerId;
    private final String playerName;
    private int attemptsDone;
    private volatile boolean finished;

    TransferFlow(Plugin plugin, Player player, PluginSettings settings,
                 AnnouncementService announcements) {
        this.plugin = plugin;
        this.player = player;
        this.settings = settings;
        this.announcements = announcements;
        this.playerId = player.getUniqueId();
        this.playerName = player.getName();
    }

    /**
     * 开始流程：切换游戏模式、发送提示、立即执行第一次命令，并安排后续尝试与超时踢出。
     */
    void start() {
        if (finished || !player.isOnline()) {
            return;
        }
        applyGamemode();
        announcements.send(player);
        runAttempt();
        scheduleKick();
    }

    /**
     * 中止流程；已经安排的任务会在执行时自行检查 {@link #finished} 后退出。
     */
    void stop() {
        finished = true;
    }

    private void applyGamemode() {
        GameMode target = settings.gamemode();
        if (target == null || player.getGameMode() == target) {
            return;
        }
        GameMode previous = player.getGameMode();
        player.setGameMode(target);
        plugin.getLogger().info(() -> "玩家 " + playerName + " 的游戏模式由 " + previous + " 切换为 " + target);
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

    private void scheduleKick() {
        player.getScheduler().runDelayed(plugin,
                task -> kickIfStillHere(),
                this::retire,
                settings.timeoutTicks());
    }

    private void kickIfStillHere() {
        if (finished || !player.isOnline()) {
            return;
        }
        finished = true;
        plugin.getLogger().info(() -> "玩家 " + playerName + " 跨服超时（每 " + settings.intervalSeconds()
                + " 秒尝试，共 " + settings.attempts() + " 次），已踢出");
        player.kick(settings.kickMessage());
    }

    private void retire() {
        finished = true;
    }
}
