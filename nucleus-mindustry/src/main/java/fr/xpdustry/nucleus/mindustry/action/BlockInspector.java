/*
 * Nucleus, the software collection powering Xpdustry.
 * Copyright (C) 2022  Xpdustry
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package fr.xpdustry.nucleus.mindustry.action;

import arc.math.geom.Point2;
import arc.util.CommandHandler;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import fr.xpdustry.distributor.api.command.argument.PlayerArgument;
import fr.xpdustry.distributor.api.command.sender.CommandSender;
import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.distributor.api.util.MoreEvents;
import fr.xpdustry.nucleus.mindustry.NucleusPlugin;
import fr.xpdustry.nucleus.mindustry.util.Pair;
import java.io.Serial;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.ConstructBlock;
import mindustry.world.blocks.distribution.DuctRouter;
import mindustry.world.blocks.distribution.ItemBridge;
import mindustry.world.blocks.distribution.MassDriver;
import mindustry.world.blocks.distribution.Sorter;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.payloads.PayloadMassDriver;
import mindustry.world.blocks.power.PowerNode;
import mindustry.world.blocks.sandbox.ItemSource;
import mindustry.world.blocks.sandbox.LiquidSource;
import mindustry.world.blocks.units.UnitFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.ocpsoft.prettytime.PrettyTime;

// TODO Cleanup
// https://github.com/Pointifix/HistoryPlugin
public final class BlockInspector implements PluginListener {

    private static final Comparator<Pair<Integer, PlayerAction>> ACTION_COMPARATOR =
            Comparator.<Pair<Integer, PlayerAction>, Instant>comparing(
                            pair -> pair.second().timestamp())
                    .reversed();

    private final Set<String> inspectors = new HashSet<>();
    private final Map<Integer, LimitedList<PlayerAction>> data = new HashMap<>();
    private final NucleusPlugin nucleus;

    public BlockInspector(final NucleusPlugin nucleus) {
        this.nucleus = nucleus;
    }

    @Override
    public void onPluginInit() {
        MoreEvents.subscribe(EventType.WorldLoadEvent.class, event -> {
            this.data.clear();
            this.inspectors.clear();
        });

        MoreEvents.subscribe(EventType.PlayerLeave.class, event -> {
            this.inspectors.remove(event.player.uuid());
        });

        MoreEvents.subscribe(EventType.BlockBuildEndEvent.class, event -> {
            if (!event.unit.isPlayer()) {
                return;
            }

            final var block = event.breaking && event.tile.build != null
                    ? ((ConstructBlock.ConstructBuild) event.tile.build).current
                    : event.tile.block();

            this.getLinkedTiles(event.tile, block, tile -> this.getTileActions(tile)
                    .add(ImmutableBlockAction.builder()
                            .author(event.unit.getPlayer().uuid())
                            .type(event.breaking ? BlockAction.Type.BREAK : BlockAction.Type.PLACE)
                            .block(block)
                            .timestamp(Instant.now())
                            .virtual(event.tile.pos() != tile.pos())
                            .build()));

            if (event.config != null) {
                this.addConfigAction(event.unit.getPlayer(), event.tile, event.config);
            }
        });

        MoreEvents.subscribe(EventType.ConfigEvent.class, event -> {
            if (event.player == null) {
                return;
            }
            this.addConfigAction(event.player, event.tile.tile(), event.value);
        });

        MoreEvents.subscribe(EventType.TapEvent.class, event -> {
            if (this.inspectors.contains(event.player.uuid())) {
                // Nice effect
                Call.effect(
                        event.player.con(),
                        Fx.placeBlock,
                        event.tile.worldx(),
                        event.tile.worldy(),
                        0,
                        event.player.team().color);

                if (!this.data.containsKey(event.tile.pos())
                        || this.getTileActions(event.tile).isEmpty()) {
                    event.player.sendMessage("No data for this tile");
                    return;
                }

                final var builder = new StringBuilder()
                        .append("[yellow]History of Block (")
                        .append(event.tile.x)
                        .append(",")
                        .append(event.tile.y)
                        .append(")");

                getTileActions(event.tile).stream()
                        .map(action -> actionToString(action, event.player.admin()))
                        .forEach(string -> builder.append('\n').append(string));

                event.player.sendMessage(builder.toString());
            }
        });
    }

    @Override
    public void onPluginClientCommandsRegistration(final CommandHandler handler) {
        final var manager = this.nucleus.getClientCommands();
        manager.command(manager.commandBuilder("inspector")
                .meta(CommandMeta.DESCRIPTION, "Toggle inspector mode.")
                .handler(ctx -> {
                    if (this.inspectors.add(ctx.getSender().getPlayer().uuid())) {
                        ctx.getSender().sendMessage("Inspector mode enabled.");
                    } else {
                        this.inspectors.remove(ctx.getSender().getPlayer().uuid());
                        ctx.getSender().sendMessage("Inspector mode disabled.");
                    }
                }));

        manager.command(manager.commandBuilder("inspect")
                .meta(CommandMeta.DESCRIPTION, "Inspect the actions of a specific player.")
                .argument(PlayerArgument.of("player"))
                .argument(IntegerArgument.<CommandSender>newBuilder("limit")
                        .withMin(1)
                        .withMax(100)
                        .asOptionalWithDefault(10)
                        .build())
                .handler(ctx -> {
                    final Player player = ctx.get("player");
                    final var builder = new StringBuilder()
                            .append("[yellow]History of Player (")
                            .append(player.name())
                            .append(")");

                    this.data.entrySet().stream()
                            .flatMap(entry -> entry.getValue().stream()
                                    .filter(action -> action.author().equals(player.uuid()) && !action.virtual())
                                    .map(action -> new Pair<>(entry.getKey(), action)))
                            .sorted(ACTION_COMPARATOR)
                            .map(pair -> actionToString(
                                    pair.second(),
                                    ctx.getSender().getPlayer().admin(),
                                    Point2.x(pair.first()),
                                    Point2.y(pair.first())))
                            .limit(ctx.<Integer>get("limit"))
                            .forEach(string -> builder.append('\n').append(string));

                    ctx.getSender().sendMessage(builder.toString());
                }));
    }

    private LimitedList<PlayerAction> getTileActions(final Tile tile) {
        return this.data.computeIfAbsent(
                tile.pos(),
                key -> new LimitedList<>(this.nucleus.getConfiguration().getInspectorHistoryLimit()));
    }

    private void addConfigAction(final Player player, final Tile tile, final @Nullable Object config) {
        final var connectConfig = this.isConnectConfig(tile.block(), config);
        if (connectConfig && config instanceof Point2[] points) {
            for (final var point : points) {
                final var pos = Point2.pack(tile.x + point.x, tile.y + point.y);
                addConfigAction(player, tile, pos);
            }
            return;
        } else if (connectConfig && config instanceof Point2 point) {
            final var pos = Point2.pack(tile.x + point.x, tile.y + point.y);
            addConfigAction(player, tile, pos);
            return;
        }

        final var actions = this.getTileActions(tile);
        final var last = actions.isEmpty() ? null : actions.get(actions.size() - 1);
        if (last instanceof ConfigAction action && action.author().equals(player.uuid())) {
            if (connectConfig && action.connect() == this.isConnected(tile.build, config)) {
                return;
            }
        }

        this.getLinkedTiles(tile, tile.block(), t -> this.getTileActions(t)
                .add(ImmutableConfigAction.builder()
                        .author(player.uuid())
                        .block(tile.block())
                        .config(config)
                        .connect(connectConfig && isConnected(tile.build, config))
                        .timestamp(Instant.now())
                        .virtual(t.pos() != tile.pos())
                        .build()));
    }

    private boolean isConnectConfig(final Block block, final @Nullable Object config) {
        if (config == null) {
            return false;
        }
        return (block instanceof LogicBlock && config instanceof Integer)
                || block instanceof PowerNode
                || block instanceof MassDriver
                || block instanceof PayloadMassDriver
                || block instanceof ItemBridge;
    }

    private boolean isConnected(final Building building, final @Nullable Object config) {
        if (!(config instanceof Integer)) {
            throw new IllegalStateException("Config is not an integer");
        }

        final int pos = (int) config;
        if (pos == -1 || building.pos() == pos) {
            return false;
        }
        final var other = Vars.world.tile(pos);
        if (other == null) {
            return false;
        }

        if (building instanceof LogicBlock.LogicBuild build) {
            final var link = build.links.find(l -> Point2.pack(l.x, l.y) == pos);
            return link != null && link.active;
        } else if (building instanceof PowerNode.PowerNodeBuild build) {
            return build.power().links.contains(pos);
        } else if (building instanceof MassDriver.MassDriverBuild build) {
            return build.link == pos && other.build instanceof MassDriver.MassDriverBuild;
        } else if (building instanceof PayloadMassDriver.PayloadDriverBuild build) {
            return build.link == pos && other.build instanceof PayloadMassDriver.PayloadDriverBuild;
        } else if (building instanceof ItemBridge.ItemBridgeBuild build) {
            return build.link == pos && other.build instanceof ItemBridge.ItemBridgeBuild;
        } else {
            return false;
        }
    }

    private void getLinkedTiles(final Tile tile, final Block block, final Consumer<Tile> consumer) {
        if (block.isMultiblock()) {
            int size = block.size, offset = block.sizeOffset;
            for (int dx = 0; dx < size; dx++) {
                for (int dy = 0; dy < size; dy++) {
                    final var other = Vars.world.tile(tile.x + dx + offset, tile.y + dy + offset);
                    if (other != null) {
                        consumer.accept(other);
                    }
                }
            }
        } else {
            consumer.accept(tile);
        }
    }

    private String actionToString(final PlayerAction action, final boolean uuid, final int x, final int y) {
        final var builder = new StringBuilder();

        builder.append("[white]> ").append(Vars.netServer.admins.getInfo(action.author()).lastName);
        if (x != -1 && y != -1) {
            builder.append(" [green](").append(x).append(",").append(y).append(")");
        }
        if (uuid) {
            builder.append(" [lightgray](").append(action.author()).append(")");
        }

        if (action instanceof BlockAction block) {
            if (block.type() == BlockAction.Type.BREAK) {
                builder.append(" [red]broke[white] ").append(block.block().name);
            } else {
                builder.append(" [accent]placed[white] ").append(block.block().name);
            }
        } else if (action instanceof ConfigAction config) {
            if (this.isConnectConfig(config.block(), config.config())) {
                final var pos = (int) config.config();
                if (config.connect()) {
                    final var point = Point2.unpack(pos);
                    builder.append(" [accent]connected[white] to [green]")
                            .append(point.x)
                            .append(",")
                            .append(point.y);
                } else {
                    if (pos < 0 || pos == Point2.pack(x, y)) {
                        builder.append(" [accent]disconnected[white] this tile");
                    } else {
                        final var point = Point2.unpack(pos);
                        builder.append(" [accent]disconnected[white] from [green]")
                                .append(point.x)
                                .append(",")
                                .append(point.y);
                    }
                }
            } else if (this.isItemConfigurableBlock(action.block())) {
                if (config.config() == null) {
                    builder.append(" [accent]configured[white] to default");
                } else {
                    builder.append(" [accent]configured[white] to [green]")
                            .append(config.config().toString());
                }
            } else if (action.block() instanceof UnitFactory factory) {
                if (config.config() == null || (int) config.config() < 0) {
                    builder.append(" [accent]configured[white] to default");
                }
                builder.append(" [accent]configured[white] to [green]")
                        .append(factory.plans.get((int) config.config()).unit.name);
            } else {
                builder.append(" [accent]configured[white] to [green]").append(config.config());
            }
        }

        final var pretty = new PrettyTime();
        pretty.setReference(Instant.now());
        pretty.setLocale(Locale.ENGLISH);
        builder.append(" [lightgray]").append(pretty.format(action.timestamp()));

        return builder.toString();
    }

    private String actionToString(final PlayerAction action, final boolean uuid) {
        return actionToString(action, uuid, -1, -1);
    }

    private boolean isItemConfigurableBlock(final Block block) {
        return block instanceof Sorter
                || block instanceof ItemSource
                || block instanceof LiquidSource
                || block instanceof DuctRouter;
    }

    @SuppressWarnings("JdkObsolete")
    private static final class LimitedList<E> extends LinkedList<E> {

        @Serial
        private static final long serialVersionUID = -6315128929800347513L;

        private final int limit;

        public LimitedList(final int limit) {
            this.limit = limit;
        }

        @Override
        public boolean add(final E e) {
            if (this.size() >= this.limit) {
                this.removeFirst();
            }
            return super.add(e);
        }
    }
}
