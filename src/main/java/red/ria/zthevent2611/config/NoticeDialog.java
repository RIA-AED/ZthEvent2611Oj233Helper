package red.ria.zthevent2611.config;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 进服提示对话框的配置。
 *
 * <p>对话框由正文、「不再提示」复选框（默认不勾选）、确认与取消按钮组成；
 * 玩家确认后才开始跨服流程，因此提示不会在跨服过程中被聊天栏清掉；
 * 点击取消会踢出玩家（由其所在群组网络自动带回云端），不开始跨服流程。
 *
 * @param title 对话框标题
 * @param bodyLines 对话框正文的每一行
 * @param checkboxKey 复选框在表单中的字段名，一般无需修改
 * @param checkboxLabel 复选框文字
 * @param checkboxInitial 复选框默认是否勾选
 * @param checkboxOnTrue 勾选时提交的值
 * @param checkboxOnFalse 未勾选时提交的值
 * @param confirmLabel 确认按钮文字
 * @param confirmTooltip 确认按钮的悬停提示
 * @param cancelLabel 取消按钮文字
 * @param cancelTooltip 取消按钮的悬停提示
 * @param cancelKickMessage 点击取消后踢出玩家时显示的消息
 * @param canCloseWithEscape 是否允许按 Esc 关闭对话框；{@code false} 时必须点击按钮
 */
public record NoticeDialog(
        Component title,
        List<Component> bodyLines,
        String checkboxKey,
        Component checkboxLabel,
        boolean checkboxInitial,
        String checkboxOnTrue,
        String checkboxOnFalse,
        Component confirmLabel,
        Component confirmTooltip,
        Component cancelLabel,
        Component cancelTooltip,
        Component cancelKickMessage,
        boolean canCloseWithEscape
) {

    public NoticeDialog {
        bodyLines = List.copyOf(bodyLines);
    }

    /**
     * @throws IllegalArgumentException 配置无效时抛出，消息为可直接展示给管理员的说明
     */
    public static NoticeDialog load(FileConfiguration config) {
        Component title = PluginSettings.miniMessage(config, "messages.dialog.title",
                "<white>元竞 鱼来 KOIKOI</white>");

        List<Component> bodyLines = new ArrayList<>();
        List<String> rawBody = config.getStringList("messages.dialog.body");
        int from = 0;
        int to = rawBody.size();
        while (from < to && isBlankLine(rawBody.get(from))) {
            from++;
        }
        while (to > from && isBlankLine(rawBody.get(to - 1))) {
            to--;
        }
        for (int i = from; i < to; i++) {
            String line = rawBody.get(i);
            if (isBlankLine(line)) {
                // 空行保留为段落间距。
                bodyLines.add(Component.empty());
            } else {
                bodyLines.add(PluginSettings.miniMessage(line.trim(), "messages.dialog.body[" + i + "]"));
            }
        }
        if (bodyLines.isEmpty()) {
            String single = config.getString("messages.dialog.body");
            if (single != null && !single.isBlank()) {
                bodyLines.add(PluginSettings.miniMessage(single.trim(), "messages.dialog.body"));
            }
        }
        if (bodyLines.isEmpty()) {
            throw new IllegalArgumentException("messages.dialog.body 不能为空");
        }

        String checkboxKey = config.getString("messages.dialog.checkbox.key", "dont_show_again");
        if (checkboxKey == null || !checkboxKey.matches("[A-Za-z0-9_.]+")) {
            throw new IllegalArgumentException(
                    "messages.dialog.checkbox.key 只能包含字母、数字、下划线和点：" + checkboxKey);
        }
        Component checkboxLabel = PluginSettings.miniMessage(config,
                "messages.dialog.checkbox.label", "不再提示");
        boolean checkboxInitial = config.getBoolean("messages.dialog.checkbox.initial", false);
        String checkboxOnTrue = config.getString("messages.dialog.checkbox.on-true", "true");
        String checkboxOnFalse = config.getString("messages.dialog.checkbox.on-false", "false");

        Component confirmLabel = PluginSettings.miniMessage(config,
                "messages.dialog.confirm.label", "确认");
        Component confirmTooltip = PluginSettings.miniMessage(config,
                "messages.dialog.confirm.tooltip", "<gray>点击确认后开始跨服流程.</gray>");
        Component cancelLabel = PluginSettings.miniMessage(config,
                "messages.dialog.cancel.label", "取消");
        Component cancelTooltip = PluginSettings.miniMessage(config,
                "messages.dialog.cancel.tooltip", "<gray>取消并返回云端.</gray>");
        Component cancelKickMessage = PluginSettings.miniMessage(config,
                "messages.dialog.cancel.kick-message", "<gray>已取消，正在返回云端.</gray>");
        boolean canCloseWithEscape = config.getBoolean("messages.dialog.can-close-with-escape", false);

        return new NoticeDialog(title, bodyLines, checkboxKey, checkboxLabel, checkboxInitial,
                checkboxOnTrue, checkboxOnFalse, confirmLabel, confirmTooltip,
                cancelLabel, cancelTooltip, cancelKickMessage, canCloseWithEscape);
    }

    private static boolean isBlankLine(String line) {
        return line == null || line.isBlank();
    }

    /**
     * 合并为一个文本组件，用换行分隔（对话框每个正文组件都自带边距，合并后排版更紧凑）。
     * 正文中的空行会保留为段落间距。
     */
    public Component body() {
        Component result = Component.empty();
        for (int i = 0; i < bodyLines.size(); i++) {
            if (i > 0) {
                result = result.append(Component.newline());
            }
            result = result.append(bodyLines.get(i));
        }
        return result;
    }
}
