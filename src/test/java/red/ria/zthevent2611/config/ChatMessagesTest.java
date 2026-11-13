package red.ria.zthevent2611.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.junit.jupiter.api.Test;

class ChatMessagesTest {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Component TRANSFERRING =
            MINI_MESSAGE.deserialize("<gray>正在进行跨服......</gray>");

    @Test
    void composesGapsAroundTransferring() {
        List<Component> lines = new ChatMessages(1, TRANSFERRING).compose();

        assertEquals(3, lines.size());
        assertEquals(Component.empty(), lines.get(0));
        assertEquals(TRANSFERRING, lines.get(1));
        assertEquals(Component.empty(), lines.get(2));
    }

    @Test
    void supportsNoGapLines() {
        List<Component> lines = new ChatMessages(0, TRANSFERRING).compose();

        assertEquals(1, lines.size());
        assertEquals(TRANSFERRING, lines.get(0));
    }

    @Test
    void supportsMultipleGapLines() {
        List<Component> lines = new ChatMessages(2, TRANSFERRING).compose();

        assertEquals(5, lines.size());
        assertEquals(Component.empty(), lines.get(0));
        assertEquals(Component.empty(), lines.get(1));
        assertEquals(TRANSFERRING, lines.get(2));
        assertEquals(Component.empty(), lines.get(3));
        assertEquals(Component.empty(), lines.get(4));
    }
}
