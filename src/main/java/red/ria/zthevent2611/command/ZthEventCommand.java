package red.ria.zthevent2611.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import red.ria.zthevent2611.Permissions;
import red.ria.zthevent2611.ZthEvent2611Oj233HelperPlugin;

/**
 * {@code /zth-event}：重载配置与手动启动跨服流程。
 *
 * <p>命令通过 Paper 的 Brigadier API 注册，因此不在 plugin.yml 的 {@code commands}
 * 中声明；权限节点在 plugin.yml 中声明。
 */
public final class ZthEventCommand {

    private final ZthEvent2611Oj233HelperPlugin plugin;

    public ZthEventCommand(ZthEvent2611Oj233HelperPlugin plugin) {
        this.plugin = plugin;
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("zth-event")
                .requires(source -> canReload(source.getSender()) || canStart(source.getSender()))
                .executes(this::sendHelp)
                .then(Commands.literal("reload")
                        .requires(source -> canReload(source.getSender()))
                        .executes(this::reload))
                .then(Commands.literal("start")
                        .requires(source -> canStart(source.getSender()))
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .executes(this::start)))
                .build();
    }

    private int sendHelp(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        sender.sendMessage(Component.text("ZthEvent2611Oj233Helper", NamedTextColor.WHITE)
                .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                .append(Component.text("跨服流程管理", NamedTextColor.GRAY)));
        List<Component> lines = new ArrayList<>();
        if (canReload(sender)) {
            lines.add(commandLine("/zth-event reload", "重载配置",
                    "重新读取 config.yml；配置无效时保留旧配置。"));
        }
        if (canStart(sender)) {
            lines.add(commandLine("/zth-event start <player>", "为玩家启动跨服流程",
                    "立即为指定玩家发送提示并开始执行跨服命令。"));
        }
        lines.forEach(sender::sendMessage);
        return Command.SINGLE_SUCCESS;
    }

    private int reload(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String error = plugin.reloadSettings();
        if (error != null) {
            sender.sendMessage(Component.text("配置重载失败：", NamedTextColor.RED)
                    .append(Component.text(error, NamedTextColor.WHITE)));
            return 0;
        }
        sender.sendMessage(Component.text("配置已重载。", NamedTextColor.GRAY));
        return Command.SINGLE_SUCCESS;
    }

    private int start(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        CommandSender sender = source.getSender();
        Player target = context.getArgument("player", PlayerSelectorArgumentResolver.class)
                .resolve(source)
                .getFirst();
        if (target == null) {
            sender.sendMessage(Component.text("找不到该玩家。", NamedTextColor.GRAY));
            return 0;
        }
        if (!plugin.startFlow(target)) {
            sender.sendMessage(Component.text("该玩家已在跨服流程中。", NamedTextColor.GRAY));
            return 0;
        }
        sender.sendMessage(Component.text("已为 ", NamedTextColor.GRAY)
                .append(Component.text(target.getName(), NamedTextColor.WHITE))
                .append(Component.text(" 启动跨服流程。", NamedTextColor.GRAY)));
        return Command.SINGLE_SUCCESS;
    }

    private static Component commandLine(String usage, String description, String hover) {
        Component command = Component.text(usage, NamedTextColor.WHITE)
                .clickEvent(ClickEvent.suggestCommand(usage))
                .hoverEvent(HoverEvent.showText(Component.text(hover, NamedTextColor.GRAY)));
        return Component.text("  ", NamedTextColor.DARK_GRAY)
                .append(command)
                .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                .append(Component.text(description, NamedTextColor.GRAY));
    }

    private static boolean canReload(CommandSender sender) {
        return sender.hasPermission(Permissions.COMMAND_RELOAD);
    }

    private static boolean canStart(CommandSender sender) {
        return sender.hasPermission(Permissions.COMMAND_START);
    }
}
