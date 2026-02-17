package pl.grzegorz2047.hytale.hungergames.commands.hg;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;

import javax.annotation.Nonnull;
import java.util.concurrent.ThreadLocalRandom;

public class ArenaBorderCommand extends AbstractPlayerCommand {

    private final RequiredArg<Integer> sizeArg;

    public ArenaBorderCommand(String commandname, String description, ArenaManager arenaManager) {
        super(commandname, description);
        this.sizeArg = this.withRequiredArg("size", "border size", ArgTypes.INTEGER);

    }

    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());

        assert transformComponent != null;

        Vector3d position = transformComponent.getPosition();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vector3f color = new Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat());
        float sizeScale = sizeArg.get(context) * 2f;
        DebugUtils.addCube(world, position, color, sizeScale, 30.0F);
        context.sendMessage(Message.raw("successfully added debug cube at " + position));
    }
}
