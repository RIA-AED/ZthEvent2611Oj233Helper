package red.ria.zthevent2611;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import red.ria.zthevent2611.command.ZthEventCommand;
import red.ria.zthevent2611.config.PluginSettings;
import red.ria.zthevent2611.data.NoticeStore;
import red.ria.zthevent2611.flow.TransferFlowManager;
import red.ria.zthevent2611.listener.PlayerFlowListener;
import red.ria.zthevent2611.message.AnnouncementService;

/**
 * 元竞 鱼来 KOIKOI 活动服务器的进服旁观与跨服引导。
 *
 * <p>玩家进服后被切换为旁观模式并收到活动说明，随后插件按配置的间隔重复执行跨服命令；
 * 超过设定时间仍未离开本服的玩家会被踢出。真正的跨服由其他插件完成，本插件只负责
 * 触发命令与流程控制。
 */
public final class ZthEvent2611Oj233HelperPlugin extends JavaPlugin {

    private volatile PluginSettings settings;
    private NoticeStore noticeStore;
    private AnnouncementService announcements;
    private TransferFlowManager flows;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        PluginSettings loaded;
        try {
            loaded = PluginSettings.load(getConfig());
        } catch (IllegalArgumentException exception) {
            getLogger().severe("config.yml 无效，插件将禁用：" + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        settings = loaded;

        noticeStore = new NoticeStore(this);
        noticeStore.load();
        announcements = new AnnouncementService(this, noticeStore);
        announcements.apply(loaded.announcements());
        flows = new TransferFlowManager(this, announcements, this::settings);

        getServer().getPluginManager().registerEvents(
                new PlayerFlowListener(this, flows, this::settings), this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(new ZthEventCommand(this).build(), "管理跨服流程"));

        logStartup();
    }

    @Override
    public void onDisable() {
        if (flows != null) {
            flows.shutdown();
        }
        if (noticeStore != null) {
            noticeStore.saveNow();
        }
        getLogger().info("ZthEvent2611Oj233Helper 已禁用");
    }

    /**
     * 当前生效的配置。热重载后返回新对象。
     */
    public PluginSettings settings() {
        return settings;
    }

    /**
     * 为玩家启动跨服流程。可在任意线程调用，内部会调度到玩家所属线程。
     *
     * @return 玩家已在流程中或插件未启用时返回 {@code false}
     */
    public boolean startFlow(Player player) {
        TransferFlowManager manager = flows;
        if (manager == null || !player.isOnline() || manager.isActive(player.getUniqueId())) {
            return false;
        }
        player.getScheduler().execute(this, () -> manager.start(player), null, 1L);
        return true;
    }

    /**
     * 重新读取 config.yml。新配置无效时保留旧配置。
     *
     * @return 成功时返回 {@code null}，失败时返回错误说明
     */
    public String reloadSettings() {
        reloadConfig();
        PluginSettings loaded;
        try {
            loaded = PluginSettings.load(getConfig());
        } catch (IllegalArgumentException exception) {
            getLogger().warning("配置重载失败，继续使用旧配置：" + exception.getMessage());
            return exception.getMessage();
        }
        settings = loaded;
        AnnouncementService service = announcements;
        if (service != null) {
            service.apply(loaded.announcements());
        }
        getLogger().info(() -> "配置已重载：自动开始=" + loaded.startOnJoin()
                + "，游戏模式=" + (loaded.gamemode() == null ? "不改变" : loaded.gamemode())
                + "，间隔=" + loaded.intervalSeconds() + " 秒，尝试=" + loaded.attempts() + " 次"
                + "，命令数=" + loaded.commands().size());
        return null;
    }

    private void logStartup() {
        PluginSettings current = settings;
        getLogger().info("========== ZthEvent2611Oj233Helper 启动 ==========");
        getLogger().info("版本：" + getPluginMeta().getVersion());
        getLogger().info("进服自动开始流程：" + current.startOnJoin());
        getLogger().info("流程切换游戏模式：" + (current.gamemode() == null ? "不改变" : current.gamemode()));
        getLogger().info("跨服命令：每 " + current.intervalSeconds() + " 秒执行一次，最多 "
                + current.attempts() + " 次（总超时 " + current.timeoutTicks() / 20 + " 秒）");
        getLogger().info("跨服命令列表：" + (current.hasCommands()
                ? String.join(" | ", current.commands()) : "（未配置）"));
        getLogger().info("提示空行：" + current.announcements().gapLines() + " 行；已隐藏提示的玩家："
                + noticeStore.hiddenCount() + " 人");
        getLogger().info("================================================");
    }
}
