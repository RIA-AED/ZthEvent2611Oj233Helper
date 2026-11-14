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
 * {@code /zth-event}：打开提示对话框、开始引导流程与重载配置。
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
                .requires(source -> canOpen(source.getSender())
                        || canReload(source.getSender())
                        || canStart(source.getSender()))
                .executes(this::sendHelp)
                .then(Commands.literal("open")
                        .requires(source -> canOpen(source.getSender()))
                        .executes(context -> openSelf(context, false))
                        .then(Commands.literal("force")
                                .executes(context -> openSelf(context, true)))
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .requires(source -> canOpenOthers(source.getSender()))
                                .executes(context -> openOther(context, false))
                                .then(Commands.literal("force")
                                        .executes(context -> openOther(context, true)))))
                .then(Commands.literal("start")
                        .requires(source -> canStart(source.getSender()))
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .executes(this::start)))
                .then(Commands.literal("reload")
                        .requires(source -> canReload(source.getSender()))
                        .executes(this::reload))
                .build();
    }

    private int sendHelp(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        sender.sendMessage(Component.text("ZthEvent2611Oj233Helper", NamedTextColor.WHITE)
                .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                .append(Component.text("跨服提示与流程", NamedTextColor.GRAY)));
        List<Component> lines = new ArrayList<>();
        if (canOpen(sender)) {
            lines.add(commandLine("/zth-event open [force]", "打开提示对话框",
                    "打开进服提示对话框；已选择不再提示的玩家会直接开始跨服流程，force 强制显示。"));
        }
        if (canOpenOthers(sender)) {
            lines.add(commandLine("/zth-event open <player> [force]", "为玩家打开提示对话框",
                    "为指定玩家打开提示对话框；已选择不再提示的玩家会直接开始跨服流程，force 强制显示。"));
        }
        if (canStart(sender)) {
            lines.add(commandLine("/zth-event start <player>", "开始托管引导",
                    "托管模式下切换旁观并开始引导；通用模式下等同于 open。"));
        }
        if (canReload(sender)) {
            lines.add(commandLine("/zth-event reload", "重载配置",
                    "重新读取 config.yml；配置无效时保留旧配置。"));
        }
        lines.forEach(sender::sendMessage);
        return Command.SINGLE_SUCCESS;
    }

    private int openSelf(CommandContext<CommandSourceStack> context, boolean force) {
        CommandSender sender = context.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("只有玩家可以直接打开对话框，请使用 /zth-event open <player>。",
                    NamedTextColor.GRAY));
            return 0;
        }
        boolean hidden = !force && plugin.isNoticeHidden(player.getUniqueId());
        if (!plugin.openDialog(player, force)) {
            sender.sendMessage(Component.text("你已在跨服流程中。", NamedTextColor.GRAY));
            return 0;
        }
        if (hidden) {
            sender.sendMessage(Component.text("你已选择不再提示，不再显示提示，直接开始跨服流程。"
                    + "如需重新查看提示，请使用 /zth-event open force。", NamedTextColor.GRAY));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int openOther(CommandContext<CommandSourceStack> context, boolean force)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Player target = resolveTarget(context, source);
        if (target == null) {
            return 0;
        }
        boolean hidden = !force && plugin.isNoticeHidden(target.getUniqueId());
        if (!plugin.openDialog(target, force)) {
            source.getSender().sendMessage(Component.text("该玩家已在跨服流程中。", NamedTextColor.GRAY));
            return 0;
        }
        Component feedback = Component.text("已为 ", NamedTextColor.GRAY)
                .append(Component.text(target.getName(), NamedTextColor.WHITE))
                .append(Component.text(hidden
                        ? " 跳过提示并开始跨服流程（该玩家已选择不再提示，force 可强制显示）。"
                        : " 打开提示对话框。", NamedTextColor.GRAY));
        source.getSender().sendMessage(feedback);
        return Command.SINGLE_SUCCESS;
    }

    private int start(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Player target = resolveTarget(context, source);
        if (target == null) {
            return 0;
        }
        boolean managed = plugin.settings() != null && plugin.settings().managed();
        boolean hidden = !managed && plugin.isNoticeHidden(target.getUniqueId());
        boolean started = managed ? plugin.beginFlow(target) : plugin.openDialog(target);
        if (!started) {
            source.getSender().sendMessage(Component.text("该玩家已在跨服流程中。", NamedTextColor.GRAY));
            return 0;
        }
        Component feedback = Component.text("已为 ", NamedTextColor.GRAY)
                .append(Component.text(target.getName(), NamedTextColor.WHITE))
                .append(Component.text(managed ? " 开始引导流程。"
                        : hidden ? " 跳过提示并开始跨服流程。"
                        : " 打开提示对话框。", NamedTextColor.GRAY));
        source.getSender().sendMessage(feedback);
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

    private static Player resolveTarget(CommandContext<CommandSourceStack> context,
                                        CommandSourceStack source) throws CommandSyntaxException {
        Player target = context.getArgument("player", PlayerSelectorArgumentResolver.class)
                .resolve(source)
                .getFirst();
        if (target == null) {
            source.getSender().sendMessage(Component.text("找不到该玩家。", NamedTextColor.GRAY));
        }
        return target;
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

    private static boolean canOpen(CommandSender sender) {
        return sender.hasPermission(Permissions.COMMAND_OPEN);
    }

    private static boolean canOpenOthers(CommandSender sender) {
        return sender.hasPermission(Permissions.COMMAND_OPEN_OTHERS);
    }

    private static boolean canReload(CommandSender sender) {
        return sender.hasPermission(Permissions.COMMAND_RELOAD);
    }

    private static boolean canStart(CommandSender sender) {
        return sender.hasPermission(Permissions.COMMAND_START);
    }
}
