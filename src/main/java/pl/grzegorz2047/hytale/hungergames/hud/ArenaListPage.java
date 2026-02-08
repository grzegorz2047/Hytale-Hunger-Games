package pl.grzegorz2047.hytale.hungergames.hud;

import au.ellie.hyui.builders.HyUIPage;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;
import pl.grzegorz2047.hytale.hungergames.arena.stat.ArenaStat;
import pl.grzegorz2047.hytale.hungergames.config.MainConfig;

import java.util.LinkedList;

public class ArenaListPage {

    private final ArenaManager arenaManager;
    private final MainConfig mainConfig;
    private String interactionWindowArena = """
            <div class="page-overlay">
                <div class="container" data-hyui-title="{{$title}}" style="anchor-height: 800;anchor-width: 900;">
                    <div class="container-contents" style="layout-mode: Top; ">
                        <p id="summary" style="padding: 4;">{{$summary}}</p>
                        <div id="list" style="layout-mode: Top; padding: 6; ">
                            {{#each arenas}}
                            <div class="bounty-card" style="layout-mode: Left; padding: 10;">
                                <p style="flex-weight: 2;">{{$worldName}}</p>
                                <p style="flex-weight: 1;">{{$activePlayerCount}}/{{$arenaSize}}</p>
                                <p style="flex-weight: 1;">{{$ingameLabel}}: {{$ingame}}</p>
                                <button value="aaa" data="bbb" id="btn-{{$worldName}}" class="small-tertiary-button">{{$joinLabel}}</button>
                            </div>
                            {{/each}}
                        </div>
                    </div>
                </div>
            </div>
            """;

    public ArenaListPage(ArenaManager arenaManager, MainConfig mainConfig) {
        this.arenaManager = arenaManager;
        this.mainConfig = mainConfig;
    }

    public PageBuilder prepareArenaListPage(PlayerRef playerRef, Player player, LinkedList<ArenaStat> arenaStats) {
        String title = getTranslationOrDefault("hungergames.ui.arenaList.title", "Arena list");
        String summaryTpl = getTranslationOrDefault("hungergames.ui.arenaList.summary", "Showing {count} arenas");
        String ingameLabel = getTranslationOrDefault("hungergames.ui.arenaList.ingameLabel", "ingame");
        String joinLabel = getTranslationOrDefault("hungergames.ui.arenaList.joinLabel", "Join");

        TemplateProcessor template = new TemplateProcessor()
                .setVariable("title", title)
                .setVariable("summary", summaryTpl.replace("{count}", String.valueOf(arenaStats.size())))
                .setVariable("ingameLabel", ingameLabel)
                .setVariable("joinLabel", joinLabel)
                .setVariable("arenas", arenaStats);

        PageBuilder pageBuilder = PageBuilder.pageForPlayer(playerRef)
                .fromTemplate(interactionWindowArena, template)
                .withLifetime(CustomPageLifetime.CanDismissOrCloseThroughInteraction);
        arenaStats.forEach(arenaStat -> {
            pageBuilder.addEventListener("btn-" + arenaStat.worldName(), CustomUIEventBindingType.Activating, (data, ctx) -> {
                ctx.getPage().ifPresent(HyUIPage::close);
                arenaManager.joinArena(arenaStat.worldName(), player);
            });
        });
        return pageBuilder;
    }

    private String getTranslationOrDefault(String key, String fallback) {
        String value = this.mainConfig.getTranslation(key);
        return value == null ? fallback : value;
    }
}
