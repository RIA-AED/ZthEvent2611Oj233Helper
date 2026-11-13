package red.ria.zthevent2611.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class NoticeDialogTest {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static YamlConfiguration parse(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (InvalidConfigurationException exception) {
            throw new IllegalArgumentException("测试 YAML 不合法", exception);
        }
        return config;
    }

    @Test
    void mergesBodyLinesWithNewline() {
        NoticeDialog dialog = new NoticeDialog(
                Component.text("标题"),
                List.of(MINI_MESSAGE.deserialize("第一行"), MINI_MESSAGE.deserialize("第二行")),
                "dont_show_again",
                Component.text("不再提示"),
                false,
                "true",
                "false",
                Component.text("确认"),
                Component.text("提示"),
                false);

        Component expected = Component.empty()
                .append(MINI_MESSAGE.deserialize("第一行"))
                .append(Component.newline())
                .append(MINI_MESSAGE.deserialize("第二行"));

        assertEquals(expected, dialog.body());
    }

    @Test
    void loadsDefaultsFromConfig() {
        YamlConfiguration config = parse("""
                messages:
                  dialog:
                    title: '<white>测试标题</white>'
                    body:
                      - '第一行'
                      - '第二行'
                """);

        NoticeDialog dialog = NoticeDialog.load(config);

        assertFalse(dialog.checkboxInitial(), "复选框默认不应勾选");
        assertFalse(dialog.canCloseWithEscape(), "默认不允许 Esc 关闭对话框");
        assertEquals("dont_show_again", dialog.checkboxKey());
        assertEquals("true", dialog.checkboxOnTrue());
        assertEquals("false", dialog.checkboxOnFalse());
        assertEquals("第二行", MINI_MESSAGE.serialize(dialog.bodyLines().get(1)));
    }

    @Test
    void keepsBlankLineAsParagraphGap() {
        YamlConfiguration config = parse("""
                messages:
                  dialog:
                    body:
                      - '第一段'
                      - ''
                      - '第二段'
                """);

        NoticeDialog dialog = NoticeDialog.load(config);

        assertEquals(3, dialog.bodyLines().size());
        assertEquals(Component.empty(), dialog.bodyLines().get(1));
    }

    @Test
    void acceptsCustomCheckboxSettings() {
        YamlConfiguration config = parse("""
                messages:
                  dialog:
                    body: '单行正文'
                    checkbox:
                      key: hide
                      label: '以后不再显示'
                      initial: true
                    can-close-with-escape: true
                """);

        NoticeDialog dialog = NoticeDialog.load(config);

        assertTrue(dialog.checkboxInitial());
        assertTrue(dialog.canCloseWithEscape());
        assertEquals("hide", dialog.checkboxKey());
        assertEquals("单行正文", MINI_MESSAGE.serialize(dialog.bodyLines().get(0)));
    }
}

