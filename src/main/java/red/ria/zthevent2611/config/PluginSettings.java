package red.ria.zthevent2611.config;

import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * config.yml 的解析结果。
 *
 * <p>解析失败时抛出 {@link IllegalArgumentException}，由插件决定是禁用自身还是保留旧配置。
 *
 * @param startOnJoin 玩家进服后是否自动开始跨服流程
 * @param gamemode 开始流程时切换的游戏模式，{@code null} 表示不改变
 * @param intervalSeconds 两次命令尝试之间的间隔（秒）
 * @param attempts 命令最多执行几次
 * @param commands 每次尝试执行的命令模板，支持 {@code {player}} 与 {@code {uuid}}
 * @param kickMessage 超时踢出玩家时显示的消息
 * @param chatMessages 聊天栏提示
 * @param noticeDialog 进服提示对话框
 */
public record PluginSettings(
        boolean startOnJoin,
        GameMode gamemode,
        int intervalSeconds,
        int attempts,
        List<String> commands,
        Component kickMessage,
        ChatMessages chatMessages,
        NoticeDialog noticeDialog
) {

    private static final int TICKS_PER_SECOND = 20;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /** 两次命令尝试之间的间隔（游戏刻）。 */
    public int intervalTicks() {
        return intervalSeconds * TICKS_PER_SECOND;
    }

    /** 从开始流程到踢出玩家的总超时（游戏刻）。 */
    public int timeoutTicks() {
        return intervalTicks() * attempts;
    }

    public boolean hasCommands() {
        return !commands.isEmpty();
    }

    /**
     * @throws IllegalArgumentException 配置无效时抛出，消息为可直接展示给管理员的说明
     */
    public static PluginSettings load(FileConfiguration config) {
        boolean startOnJoin = config.getBoolean("flow.start-on-join", true);
        GameMode gamemode = parseGamemode(config.getString("flow.gamemode", "SPECTATOR"));

        int intervalSeconds = config.getInt("flow.interval-seconds", 10);
        if (intervalSeconds < 1) {
            throw new IllegalArgumentException("flow.interval-seconds 必须大于 0");
        }
        int attempts = config.getInt("flow.attempts", 3);
        if (attempts < 1) {
            throw new IllegalArgumentException("flow.attempts 必须大于 0");
        }

        List<String> commands = config.getStringList("flow.commands").stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();

        Component kickMessage = miniMessage(config, "flow.kick-message",
                "<gray>跨服超时，请重新连接后重试.</gray>");
        ChatMessages chatMessages = ChatMessages.load(config);
        NoticeDialog noticeDialog = NoticeDialog.load(config);
        return new PluginSettings(startOnJoin, gamemode, intervalSeconds, attempts,
                commands, kickMessage, chatMessages, noticeDialog);
    }

    private static GameMode parseGamemode(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (value.isEmpty() || value.equals("NONE") || value.equals("KEEP")) {
            return null;
        }
        try {
            return GameMode.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "flow.gamemode 必须是 SURVIVAL、CREATIVE、ADVENTURE、SPECTATOR 或 NONE：" + raw,
                    exception);
        }
    }

    static Component miniMessage(FileConfiguration config, String path, String fallback) {
        String raw = config.getString(path, fallback);
        return miniMessage(raw == null ? fallback : raw, path);
    }

    /**
     * @param path 配置项路径，仅用于错误提示
     * @throws IllegalArgumentException MiniMessage 无法解析时抛出
     */
    static Component miniMessage(String raw, String path) {
        try {
            return MINI_MESSAGE.deserialize(raw);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "配置项 " + path + " 不是合法的 MiniMessage：" + raw, exception);
        }
    }
}
