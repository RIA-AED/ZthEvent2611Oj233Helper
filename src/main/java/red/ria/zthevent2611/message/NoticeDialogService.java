package red.ria.zthevent2611.message;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import java.util.logging.Level;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import red.ria.zthevent2611.config.NoticeDialog;
import red.ria.zthevent2611.data.NoticeStore;

/**
 * 进服提示对话框：「不再提示」复选框 + 确认按钮。
 *
 * <p>对话框在配置应用时构建一次。确认 / 取消按钮使用固定的自定义动作（{@link #CONFIRM_KEY}、{@link #CANCEL_KEY}），
 * 由 {@link NoticeDialogListener} 监听 {@code PlayerCustomClickEvent} 处理——这样同一个对话框
 * 可以被任意多玩家、任意多次点击，不像一次性点击回调那样在第一次点击后失效。
 */
public final class NoticeDialogService {

    /** 对话框正文的最大宽度（GUI 像素），默认 200 会让中文长句折行生硬。 */
    private static final int BODY_WIDTH = 280;

    /** 确认按钮的自定义动作标识。 */
    public static final Key CONFIRM_KEY = Key.key("zthevent2611", "notice_confirm");

    /** 取消按钮的自定义动作标识。 */
    public static final Key CANCEL_KEY = Key.key("zthevent2611", "notice_cancel");

    private final Plugin plugin;
    private final NoticeStore noticeStore;
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
        try {
            this.dialog = build(next);
        } catch (RuntimeException exception) {
            this.dialog = null;
            plugin.getLogger().log(Level.WARNING, "构建进服提示对话框失败，将退回聊天栏提示", exception);
        }
    }

    /**
     * 向玩家展示提示对话框；对话框不可用时把正文发到聊天栏并立即执行 {@code onFallback}。
     *
     * <p>已在对话框中勾选过「不再提示」的玩家由调用方自行跳过，本方法不判断持久化状态。
     * 应在玩家所属的 EntityScheduler 线程上调用。
     */
    public void show(Player player, Runnable onFallback) {
        NoticeDialog current = config;
        Dialog built = dialog;
        if (current == null || built == null) {
            sendBodyToChat(player, current);
            onFallback.run();
            return;
        }
        try {
            player.showDialog(built);
        } catch (RuntimeException exception) {
            // 客户端不支持对话框（1.21.6 以下）或发送失败时退回聊天栏提示，避免玩家卡在等待。
            plugin.getLogger().log(Level.WARNING,
                    "向玩家 " + player.getName() + " 展示对话框失败，改用聊天栏提示", exception);
            sendBodyToChat(player, current);
            onFallback.run();
        }
    }

    /**
     * 读取点击确认时提交的复选框状态。
     */
    public boolean isCheckboxChecked(DialogResponseView response) {
        NoticeDialog current = config;
        if (current == null || response == null) {
            return false;
        }
        String text = response.getText(current.checkboxKey());
        if (text != null) {
            return current.checkboxOnTrue().equals(text);
        }
        return Boolean.TRUE.equals(response.getBoolean(current.checkboxKey()));
    }

    /**
     * 处理玩家确认：记录「不再提示」状态，然后在玩家所属线程上执行 {@code onConfirmed}。
     *
     * <p>由 {@link NoticeDialogListener} 在收到 {@code PlayerCustomClickEvent} 时调用。
     */
    public void handleConfirm(Player player, boolean confirmed, Runnable onConfirmed) {
        player.getScheduler().execute(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            plugin.getLogger().info(() -> "玩家 " + player.getName() + " 确认了活动提示（不再提示="
                    + confirmed + "）");
            if (confirmed) {
                noticeStore.hide(player.getUniqueId());
            }
            onConfirmed.run();
        }, null, 1L);
    }

    /**
     * 处理玩家点击取消：踢出玩家（由其所在群组网络自动带回云端），不开始跨服流程。
     *
     * <p>由 {@link NoticeDialogListener} 在收到 {@code PlayerCustomClickEvent} 时调用。
     */
    public void handleCancel(Player player) {
        NoticeDialog current = config;
        player.getScheduler().execute(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            plugin.getLogger().info(() -> "玩家 " + player.getName() + " 取消了进服提示，已踢出（返回云端）");
            if (current != null) {
                player.kick(current.cancelKickMessage());
            } else {
                player.kick();
            }
        }, null, 1L);
    }

    private void sendBodyToChat(Player player, NoticeDialog current) {
        if (current == null) {
            return;
        }
        for (var line : current.bodyLines()) {
            player.sendMessage(line);
        }
    }

    private Dialog build(NoticeDialog initial) {
        ActionButton confirm = ActionButton.builder(initial.confirmLabel())
                .tooltip(initial.confirmTooltip())
                .action(DialogAction.customClick(CONFIRM_KEY, null))
                .build();
        ActionButton cancel = ActionButton.builder(initial.cancelLabel())
                .tooltip(initial.cancelTooltip())
                .action(DialogAction.customClick(CANCEL_KEY, null))
                .build();

        DialogBase base = DialogBase.builder(initial.title())
                .canCloseWithEscape(initial.canCloseWithEscape())
                .pause(false)
                .afterAction(DialogBase.DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(initial.body(), BODY_WIDTH)))
                .inputs(List.of(DialogInput.bool(initial.checkboxKey(), initial.checkboxLabel())
                        .initial(initial.checkboxInitial())
                        .onTrue(initial.checkboxOnTrue())
                        .onFalse(initial.checkboxOnFalse())
                        .build()))
                .build();

        return Dialog.create(factory -> factory.empty()
                .base(base)
                .type(DialogType.multiAction(List.of(confirm, cancel), null, 2)));
    }
}


