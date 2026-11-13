package red.ria.zthevent2611.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CommandTemplateTest {

    private static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void replacesPlayerName() {
        assertEquals("omt Alice koikoi",
                CommandTemplate.render("omt {player} koikoi", "Alice", PLAYER_ID));
    }

    @Test
    void replacesUuid() {
        assertEquals("say " + PLAYER_ID,
                CommandTemplate.render("say {uuid}", "Alice", PLAYER_ID));
    }

    @Test
    void replacesAllOccurrences() {
        assertEquals("tp Alice Alice",
                CommandTemplate.render("tp {player} {player}", "Alice", PLAYER_ID));
    }

    @Test
    void leavesUnknownPlaceholdersUntouched() {
        assertEquals("say %player%", CommandTemplate.render("say %player%", "Alice", PLAYER_ID));
    }

    @Test
    void stripsLeadingSlash() {
        assertEquals("say Alice", CommandTemplate.render(" /say {player} ", "Alice", PLAYER_ID));
    }
}
