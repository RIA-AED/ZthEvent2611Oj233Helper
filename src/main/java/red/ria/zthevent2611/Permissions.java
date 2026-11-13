package red.ria.zthevent2611;

/**
 * 插件使用的权限节点，与 plugin.yml 中的声明保持一致。
 */
public final class Permissions {

    /** 拥有该权限的玩家进服时不会被切换模式，也不会自动开始跨服流程。 */
    public static final String BYPASS = "zthevent2611.bypass";

    /** 允许使用 {@code /zth-event reload}。 */
    public static final String COMMAND_RELOAD = "zthevent2611.command.reload";

    /** 允许使用 {@code /zth-event start}。 */
    public static final String COMMAND_START = "zthevent2611.command.start";

    private Permissions() {
    }
}
