package red.ria.zthevent2611.listener;

import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import red.ria.zthevent2611.flow.TransferFlowManager;
import red.ria.zthevent2611.message.NoticeDialogService;

/**
 * 处理进服提示对话框的确认与取消按钮。
 *
 * <p>对话框按钮使用固定的自定义动作标识，通过 {@code PlayerCustomClickEvent} 接收点击；
 * 因此同一个对话框可以被任意多玩家、任意多次点击，不受一次性回调限制。
 * 确认后开始跨服流程；取消则踢出玩家（由其所在群组网络自动带回云端）。
 */
public final class NoticeDialogListener implements Listener {

    private final NoticeDialogService noticeDialogs;
    private final TransferFlowManager flows;

    public NoticeDialogListener(NoticeDialogService noticeDialogs, TransferFlowManager flows) {
        this.noticeDialogs = noticeDialogs;
        this.flows = flows;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onCustomClick(PlayerCustomClickEvent event) {
        Key identifier = event.getIdentifier();
        boolean confirm;
        if (NoticeDialogService.CONFIRM_KEY.equals(identifier)) {
            confirm = true;
        } else if (NoticeDialogService.CANCEL_KEY.equals(identifier)) {
            confirm = false;
        } else {
            return;
        }
        if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) {
            // 配置阶段（登录尚未完成）的连接不处理，本插件只处理游戏内对话框。
            return;
        }
        Player player = connection.getPlayer();
        if (player == null) {
            return;
        }
        if (!confirm) {
            noticeDialogs.handleCancel(player);
            return;
        }
        DialogResponseView response = event.getDialogResponseView();
        boolean dismissed = noticeDialogs.isCheckboxChecked(response);
        noticeDialogs.handleConfirm(player, dismissed, () -> flows.startAfterConfirm(player));
    }
}
