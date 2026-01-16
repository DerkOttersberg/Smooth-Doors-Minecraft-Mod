package io.github.yxmna.fancydooranim;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.block.DoorBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(value=EnvType.CLIENT)
public class FancyDoorAnimClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("fancy-door-anim");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Fancy Door Anim client initializing...");
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null) {
                return;
            }
            DoorAnimationTracker.pruneExpired();
        });
        
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> 
            DoorAnimationTracker.clearAll()
        );
        
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> 
            DoorAnimationTracker.clearAll()
        );
        
        // Model hiding temporarily disabled for 1.21.11 - model API changed significantly
        // TODO: Re-implement when Fabric model API stabilizes for 1.21.11+
        // ModelLoadingPlugin.register(ctx -> 
        //     ctx.modifyBlockModelAfterBake().register((model, context) -> {
        //         if (context.state().getBlock() instanceof DoorBlock) {
        //             return new DoorHidingModel(model);
        //         }
        //         return model;
        //     })
        // );
    }
}
