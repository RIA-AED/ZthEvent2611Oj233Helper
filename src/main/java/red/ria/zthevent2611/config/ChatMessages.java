package red.ria.zthevent2611.config;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 聊天栏提示：确认对话框后显示的跨服进行中消息。
 *
 * <p>消息开头、结尾各留 {@code gapLines} 行空行。
 *
 * @param gapLines 空行数量
 * @param transferring 跨服进行中的提示
 */
public record ChatMessages(int gapLines, Component transferring) {

    public ChatMessages {
        gapLines = Math.max(0, gapLines);
    }

    /**
     * @throws IllegalArgumentException 配置无效时抛出，消息为可直接展示给管理员的说明
     */
    public static ChatMessages load(FileConfiguration config) {
        int gapLines = config.getInt("messages.gap-lines", 1);
        Component transferring = PluginSettings.miniMessage(config, "messages.transferring",
                "<gray>正在进行跨服......</gray>");
        return new ChatMessages(gapLines, transferring);
    }

    /**
     * 组装一次完整提示：开头空行、跨服提示、结尾空行。
     */
    public List<Component> compose() {
        List<Component> lines = new ArrayList<>();
        addGaps(lines);
        lines.add(transferring);
        addGaps(lines);
        return List.copyOf(lines);
    }

    private void addGaps(List<Component> lines) {
        for (int i = 0; i < gapLines; i++) {
            lines.add(Component.empty());
        }
    }
}
