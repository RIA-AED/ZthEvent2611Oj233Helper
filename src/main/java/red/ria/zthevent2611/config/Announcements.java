package red.ria.zthevent2611.config;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 聊天栏提示的静态内容。
 *
 * <p>空行（gap 行）的位置与数量在此定义：消息开头、上半部分与下半部分之间、消息结尾各
 * {@code gapLines} 行。
 *
 * @param gapLines 空行数量
 * @param noticeLines 上半部分（活动说明）的每一行
 * @param transferring 下半部分（跨服进行中）的提示
 * @param dismissText “不再提示”按钮文字
 * @param dismissHover “不再提示”按钮的悬停提示
 * @param dismissConfirmed 点击“不再提示”后的反馈消息
 */
public record Announcements(
        int gapLines,
        List<Component> noticeLines,
        Component transferring,
        Component dismissText,
        Component dismissHover,
        Component dismissConfirmed
) {

    public Announcements {
        gapLines = Math.max(0, gapLines);
    }

    /**
     * @throws IllegalArgumentException 配置无效时抛出，消息为可直接展示给管理员的说明
     */
    public static Announcements load(FileConfiguration config) {
        int gapLines = config.getInt("messages.gap-lines", 1);

        List<Component> noticeLines = new ArrayList<>();
        List<String> rawNotice = config.getStringList("messages.notice");
        for (int i = 0; i < rawNotice.size(); i++) {
            String line = rawNotice.get(i);
            if (line == null || line.isBlank()) {
                continue;
            }
            noticeLines.add(PluginSettings.miniMessage(line.trim(), "messages.notice[" + i + "]"));
        }
        if (noticeLines.isEmpty()) {
            // 也允许把上半部分写成单个字符串。
            String single = config.getString("messages.notice");
            if (single != null && !single.isBlank()) {
                noticeLines.add(PluginSettings.miniMessage(single.trim(), "messages.notice"));
            }
        }
        if (noticeLines.isEmpty()) {
            throw new IllegalArgumentException("messages.notice 不能为空");
        }

        Component transferring = PluginSettings.miniMessage(config, "messages.transferring",
                "<gray>正在进行跨服......</gray>");
        Component dismissText = PluginSettings.miniMessage(config, "messages.dismiss.text",
                "<u>[不再提示]</u>");
        Component dismissHover = PluginSettings.miniMessage(config, "messages.dismiss.hover",
                "<gray>点击后不再显示上半部分提示.</gray>");
        Component dismissConfirmed = PluginSettings.miniMessage(config, "messages.dismiss.confirmed",
                "<gray>已隐藏上半部分提示.</gray>");

        return new Announcements(gapLines, List.copyOf(noticeLines), transferring,
                dismissText, dismissHover, dismissConfirmed);
    }

    /**
     * 组装一次完整提示：空行、上半部分（未隐藏时）、空行、下半部分、空行。
     *
     * @param dismissButton 已绑定点击事件的“不再提示”按钮，{@code null} 表示不显示按钮
     * @param noticeHidden 玩家是否已点击过“不再提示”
     */
    public List<Component> compose(Component dismissButton, boolean noticeHidden) {
        List<Component> lines = new ArrayList<>();
        addGaps(lines);
        if (!noticeHidden) {
            Component notice = joinLines(noticeLines);
            if (dismissButton != null) {
                notice = notice.append(Component.space()).append(dismissButton);
            }
            lines.add(notice);
            addGaps(lines);
        }
        lines.add(transferring);
        addGaps(lines);
        return List.copyOf(lines);
    }

    private void addGaps(List<Component> lines) {
        for (int i = 0; i < gapLines; i++) {
            lines.add(Component.empty());
        }
    }

    private static Component joinLines(List<Component> lines) {
        Component result = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                result = result.append(Component.newline());
            }
            result = result.append(lines.get(i));
        }
        return result;
    }
}
