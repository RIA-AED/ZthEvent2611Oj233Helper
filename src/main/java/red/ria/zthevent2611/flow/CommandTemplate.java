package red.ria.zthevent2611.flow;

import java.util.UUID;

/**
 * 跨服命令模板的占位符展开。
 */
public final class CommandTemplate {

    private CommandTemplate() {
    }

    /**
     * 展开占位符并去掉可能多余的斜杠前缀，得到可直接交给控制台执行的命令。
     *
     * @param template 命令模板，支持 {@code {player}}（玩家名）与 {@code {uuid}}（玩家 UUID）
     * @param playerName 玩家名
     * @param playerId 玩家 UUID
     * @return 展开后的命令
     */
    public static String render(String template, String playerName, UUID playerId) {
        String command = template
                .replace("{player}", playerName)
                .replace("{uuid}", playerId.toString())
                .trim();
        return command.startsWith("/") ? command.substring(1) : command;
    }
}
