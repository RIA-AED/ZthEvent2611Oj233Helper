package red.ria.zthevent2611.message;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import red.ria.zthevent2611.config.NoticeDialog;
import red.ria.zthevent2611.data.NoticeStore;

/**
 * 进服提示对话框：「不再提示」复选框 + 确认按钮。
 *
 * <p>对话框在配置应用时构建一次，玩家点击确认后：若勾选了复选框则记入 {@link NoticeStore}，
 * 随后执行该玩家的确认回调。对话框默认禁止 Esc 关闭，因此玩家一定会看到提示；
 * 回调在玩家所属线程上执行。
 */
public final class NoticeDialogService {

    private final Plugin plugin;
    private final NoticeStore noticeStore;
    private final Map<UUID, Runnable> pendingConfirms = new ConcurrentHashMap<>();
    private volatile NoticeDialog config;
    private volatile Dialog dialog;

    public NoticeDialogService(Plugin plugin, NoticeStore noticeStore) {
        this.plugin = plugin;
        this.noticeStore = noticeStore;
    }

    /**
     * 应用（或热重载后重新应用）对话框配置，并在此时完成构建。
     *
     * <p>构建失败（例如服务端未提供对话框注册表）时记录警告并退回聊天栏提示，不会卡住玩家。
     */
    public void apply(NoticeDialog next) {
        this.config = next;
        pendingConfirms.clear();
        try {
            this.dialog = build(next);
        } catch (RuntimeException exception) {
            this.dialog = null;
            plugin.getLogger().log(Level.WARNING, "构建进服提示对话框失败，将退回聊天栏提示", exception);
        }
    }

    /**
     * 向玩家展示提示对话框，确认后执行 {@code onConfirm}（在玩家所属线程上）。
     *
     * <p>应在玩家所属的 EntityScheduler 线程上调用。
     */
    public void show(Player player, Runnable onConfirm) {
        NoticeDialog current = config;
        Dialog built = dialog;
        if (current == null || built == null) {
            if (current != null) {
                for (var line : current.bodyLines()) {
                    player.sendMessage(line);
                }
            }
            onConfirm.run();
            return;
        }
        pendingConfirms.put(player.getUniqueId(), onConfirm);
        try {
            player.showDialog(built);
        } catch (RuntimeException exception) {
            // 客户端不支持对话框（1.21.6 以下）或发送失败时退回聊天栏提示，避免玩家卡在等待。
            pendingConfirms.remove(player.getUniqueId());
            plugin.getLogger().log(Level.WARNING,
                    "向玩家 " + player.getName() + " 展示对话框失败，改用聊天栏提示", exception);
            for (var line : current.bodyLines()) {
                player.sendMessage(line);
            }
            onConfirm.run();
        }
    }

    /**
     * 玩家退出时丢弃尚未确认的回调，避免残留。
     */
    public void forget(Player player) {
        pendingConfirms.remove(player.getUniqueId());
    }

    /**
     * 插件停用时清理全部等待中的回调。
     */
    public void shutdown() {
        pendingConfirms.clear();
        config = null;
        dialog = null;
    }

    private Dialog build(NoticeDialog initial) {
        DialogActionCallback callback = (response, audience) -> {
            if (!(audience instanceof Player player)) {
                return;
            }
            Runnable onConfirm = pendingConfirms.remove(player.getUniqueId());
            NoticeDialog current = config;
            boolean hidden = current != null && isChecked(current, response);
            player.getScheduler().execute(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                plugin.getLogger().info(() -> "玩家 " + player.getName() + " 确认了活动提示（不再提示="
                        + hidden + "）");
                if (hidden) {
                    noticeStore.hide(player.getUniqueId());
                }
                if (onConfirm != null) {
                    onConfirm.run();
                } else {
                    plugin.getLogger().warning("玩家 " + player.getName()
                            + " 点击的提示对话框已失效（配置可能已重载），未开始跨服流程");
                }
            }, null, 1L);
        };

        ActionButton confirm = ActionButton.builder(initial.confirmLabel())
                .tooltip(initial.confirmTooltip())
                .action(DialogAction.customClick(callback,
                        ClickCallback.Options.builder().uses(1).build()))
                .build();

        DialogBase base = DialogBase.builder(initial.title())
                .canCloseWithEscape(initial.canCloseWithEscape())
                .pause(false)
                .afterAction(DialogBase.DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(initial.body())))
                .inputs(List.of(DialogInput.bool(initial.checkboxKey(), initial.checkboxLabel())
                        .initial(initial.checkboxInitial())
                        .onTrue(initial.checkboxOnTrue())
                        .onFalse(initial.checkboxOnFalse())
                        .build()))
                .build();

        return Dialog.create(factory -> factory.empty()
                .base(base)
                .type(DialogType.multiAction(List.of(confirm), null, 1)));
    }

    private static boolean isChecked(NoticeDialog current, DialogResponseView response) {
        String text = response.getText(current.checkboxKey());
        if (text != null) {
            return current.checkboxOnTrue().equals(text);
        }
        return Boolean.TRUE.equals(response.getBoolean(current.checkboxKey()));
    }
}
