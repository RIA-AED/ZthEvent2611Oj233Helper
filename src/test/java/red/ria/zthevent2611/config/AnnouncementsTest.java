package red.ria.zthevent2611.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.junit.jupiter.api.Test;

class AnnouncementsTest {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Component DISMISS = MINI_MESSAGE.deserialize("<u>[不再提示]</u>");

    private static Announcements sample(int gapLines) {
        return new Announcements(
                gapLines,
                List.of(MINI_MESSAGE.deserialize("<gray>上半部分一</gray>"),
                        MINI_MESSAGE.deserialize("<gray>上半部分二</gray>")),
                MINI_MESSAGE.deserialize("<gray>正在进行跨服......</gray>"),
                DISMISS,
                MINI_MESSAGE.deserialize("<gray>悬停提示</gray>"),
                MINI_MESSAGE.deserialize("<gray>已隐藏</gray>"));
    }

    @Test
    void composesGapsAtStartBetweenPartsAndAtEnd() {
        List<Component> lines = sample(1).compose(DISMISS, false);

        Component expectedNotice = Component.empty()
                .append(MINI_MESSAGE.deserialize("<gray>上半部分一</gray>"))
                .append(Component.newline())
                .append(MINI_MESSAGE.deserialize("<gray>上半部分二</gray>"))
                .append(Component.space())
                .append(DISMISS);

        assertEquals(5, lines.size());
        assertEquals(Component.empty(), lines.get(0));
        assertEquals(expectedNotice, lines.get(1));
        assertEquals(Component.empty(), lines.get(2));
        assertEquals(MINI_MESSAGE.deserialize("<gray>正在进行跨服......</gray>"), lines.get(3));
        assertEquals(Component.empty(), lines.get(4));
    }

    @Test
    void hidesNoticePartWhenDismissed() {
        List<Component> lines = sample(1).compose(DISMISS, true);

        assertEquals(3, lines.size());
        assertEquals(Component.empty(), lines.get(0));
        assertEquals(MINI_MESSAGE.deserialize("<gray>正在进行跨服......</gray>"), lines.get(1));
        assertEquals(Component.empty(), lines.get(2));
    }

    @Test
    void supportsMultipleGapLines() {
        List<Component> lines = sample(2).compose(DISMISS, true);

        // 上半部分隐藏时：开头 2 行空行、下半部分、结尾 2 行空行
        assertEquals(5, lines.size());
        assertEquals(Component.empty(), lines.get(0));
        assertEquals(Component.empty(), lines.get(1));
        assertEquals(MINI_MESSAGE.deserialize("<gray>正在进行跨服......</gray>"), lines.get(2));
    }

    @Test
    void supportsNoGapLines() {
        Announcements announcements = new Announcements(
                0,
                List.of(MINI_MESSAGE.deserialize("上半部分")),
                MINI_MESSAGE.deserialize("正在进行跨服......"),
                DISMISS,
                MINI_MESSAGE.deserialize("悬停提示"),
                MINI_MESSAGE.deserialize("已隐藏"));

        assertEquals(2, announcements.compose(DISMISS, false).size());
    }
}
