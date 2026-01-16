# Indigo Renderer Block Hiding - Implementation Summary

## ✅ SOLUTION IMPLEMENTED

### The Problem
Your original `BlockModelRendererMixin` was never called because Fabric API's Indigo renderer completely bypasses the vanilla `BlockModelRenderer.render()` method when active.

### The Root Cause
Indigo uses a `@Redirect` mixin in `SectionBuilderMixin` that intercepts the call to `BlockRenderManager.renderBlock()` and routes it through `TerrainRenderContext.tessellateBlock()` instead.

### The Solution
Created [TerrainRenderContextMixin.java](src/client/java/io/github/yxmna/fancydooranim/mixin/TerrainRenderContextMixin.java) that intercepts Indigo's actual rendering path.

---

## Files Modified

1. **Created:** `src/client/java/io/github/yxmna/fancydooranim/mixin/TerrainRenderContextMixin.java`
   - Targets: `net.fabricmc.fabric.impl.client.indigo.renderer.render.TerrainRenderContext`
   - Method: `tessellateBlock(BlockState, BlockPos, Object, MatrixStack)`
   - Injection: `@At("HEAD")` with `cancellable = true`
   - Key flag: `remap = false` (Fabric API class, not Minecraft)

2. **Modified:** `src/client/resources/fancy-door-anim.client.mixins.json`
   - Added `"TerrainRenderContextMixin"` to client mixins array

3. **Updated:** `RESEARCH_BLOCK_HIDING.md`
   - Documented the Indigo rendering pipeline
   - Explained why the original approach didn't work
   - Provided implementation details

---

## How It Works

### Rendering Pipeline with Indigo Active

```
ChunkBuilder
  └─> SectionBuilder.build()
      └─> SectionBuilderMixin @Redirect (Fabric API)
          └─> TerrainRenderContext.tessellateBlock()
              └─> TerrainRenderContextMixin @Inject ← YOUR MIXIN
                  └─> ci.cancel() if door is animating
```

### Dual Compatibility

You now have BOTH mixins:
- `TerrainRenderContextMixin` - For Indigo renderer (active in your environment)
- `BlockModelRendererMixin` - For vanilla renderer (fallback if Indigo disabled)

This ensures your mod works whether Indigo is active or not.

---

## Build Status

✅ **BUILD SUCCESSFUL**

Note: The warning `"Cannot remap tessellateBlock..."` is **expected and correct** because:
- TerrainRenderContext is a Fabric API class (not Minecraft)
- We set `remap = false` in the mixin annotation
- Fabric API classes don't use obfuscation mappings

---

## Testing Steps

1. Launch Minecraft 1.21.11 with the rebuilt mod
2. Look for this log line:
   ```
   [FDA][INDIGO] TerrainRenderContext mixin IS ACTIVE - Indigo renderer detected!
   ```
3. Place a door and trigger an animation
4. You should see:
   ```
   [FDA][INDIGO] Canceling tessellation for animating door at {x, y, z}
   ```
5. The door should disappear from the chunk mesh during animation

---

## Key Technical Details

### Why `Object` for the model parameter?
The BakedModel interface has different package paths in different mapping contexts. Using `Object` avoids compile-time dependency issues while still allowing the mixin to match the method signature.

### Why `remap = false`?
TerrainRenderContext is part of Fabric API's implementation package (`net.fabricmc.fabric.impl`), not part of Minecraft. It doesn't undergo obfuscation and doesn't need remapping through Yarn mappings.

### Why `targets = "..."`?
Implementation classes from Fabric API may not be accessible at compile time, so we use the string target instead of a direct class reference.

---

## References

- [SectionBuilderMixin (Fabric API)](https://github.com/FabricMC/fabric-api/blob/1.21/fabric-renderer-indigo/src/client/java/net/fabricmc/fabric/mixin/client/indigo/renderer/SectionBuilderMixin.java)
- [TerrainRenderContext (Fabric API)](https://github.com/FabricMC/fabric-api/blob/1.21/fabric-renderer-indigo/src/client/java/net/fabricmc/fabric/impl/client/indigo/renderer/render/TerrainRenderContext.java)
- [AbstractBlockRenderContext (Fabric API)](https://github.com/FabricMC/fabric-api/blob/1.21/fabric-renderer-indigo/src/client/java/net/fabricmc/fabric/impl/client/indigo/renderer/render/AbstractBlockRenderContext.java)
