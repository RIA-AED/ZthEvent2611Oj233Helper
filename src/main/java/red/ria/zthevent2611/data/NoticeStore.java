package red.ria.zthevent2611.data;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * 玩家偏好数据：记录哪些玩家点击过 [不再提示]。
 *
 * <p>数据保存在插件数据目录下独立的 {@code player-data.yml}，与 config.yml 分开。
 * 内存中使用并发集合，写盘通过异步调度器完成，不阻塞服务器线程；玩家很少点击，写盘频率很低。
 */
public final class NoticeStore {

    private static final String HIDDEN_KEY = "notice-hidden";

    private final Plugin plugin;
    private final File file;
    private final Set<UUID> hidden = ConcurrentHashMap.newKeySet();

    public NoticeStore(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "player-data.yml");
    }

    /**
     * 读取已保存的数据，应在插件启用时调用一次。
     */
    public void load() {
        hidden.clear();
        if (!file.isFile()) {
            return;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        for (String raw : data.getStringList(HIDDEN_KEY)) {
            try {
                hidden.add(UUID.fromString(raw));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("player-data.yml 中存在无效的 UUID，已忽略：" + raw);
            }
        }
    }

    public boolean isHidden(UUID playerId) {
        return hidden.contains(playerId);
    }

    public int hiddenCount() {
        return hidden.size();
    }

    /**
     * 记录玩家点击过 [不再提示]，并异步写盘。
     */
    public void hide(UUID playerId) {
        if (!hidden.add(playerId)) {
            return;
        }
        if (!plugin.isEnabled()) {
            return;
        }
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> save());
    }

    /**
     * 立即写盘，用于插件停用前刷新状态。
     */
    public synchronized void saveNow() {
        save();
    }

    private synchronized void save() {
        YamlConfiguration data = new YamlConfiguration();
        List<String> values = hidden.stream().map(UUID::toString).sorted().toList();
        data.set(HIDDEN_KEY, values);
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) {
                plugin.getLogger().warning("无法创建插件数据目录：" + parent);
                return;
            }
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "保存 player-data.yml 失败", exception);
        }
    }
}
