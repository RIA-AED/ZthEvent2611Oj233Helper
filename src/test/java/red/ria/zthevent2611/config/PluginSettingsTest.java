package red.ria.zthevent2611.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.GameMode;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class PluginSettingsTest {

    private static final String MINIMAL = """
            messages:
              dialog:
                body: '正文'
            """;

    private static PluginSettings load(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (InvalidConfigurationException exception) {
            throw new IllegalArgumentException("测试 YAML 不合法", exception);
        }
        return PluginSettings.load(config);
    }

    @Test
    void managedModeDefaultsToFalse() {
        assertFalse(load(MINIMAL).managed(), "默认应为通用模式（managed=false）");
    }

    @Test
    void managedModeCanBeEnabled() {
        assertTrue(load("managed: true\n" + MINIMAL).managed());
    }

    @Test
    void loadsFlowDefaults() {
        PluginSettings settings = load(MINIMAL);

        assertEquals(GameMode.SPECTATOR, settings.gamemode());
        assertEquals(10, settings.intervalSeconds());
        assertEquals(3, settings.attempts());
        assertTrue(settings.commands().isEmpty());
        assertEquals(1, settings.chatMessages().gapLines());
    }

    @Test
    void rejectsInvalidInterval() {
        assertThrows(IllegalArgumentException.class,
                () -> load("flow:\n  interval-seconds: 0\n" + MINIMAL));
    }
}
