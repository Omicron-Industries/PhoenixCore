package net.phoenix.core.integration.ponder;

import net.createmod.ponder.api.registration.SceneRegistryAccess;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderStoryBoardEntry;

import java.util.*;

public class PonderStoriesManager {

    private final List<PonderStoryBoardEntry> entries = Collections.synchronizedList(new ArrayList<>());

    public void add(PonderStoryBoardEntry entry) {
        entries.add(entry);
    }

    public void clear() {
        // SceneRegistryAccess only exposes read methods — direct removal is not part of the public API.
        // If you need reload support, you will need a mixin accessor on the concrete scene registry
        // to get the underlying map and mutate it directly, similar to PonderTagRegistryAccessor.
        entries.clear();
    }

    public void compileLang() {
        // compile() runs each storyboard and registers its localization as a side effect
        SceneRegistryAccess access = PonderIndex.getSceneAccess();
        for (PonderStoryBoardEntry entry : entries) {
            try {
                access.compile(Collections.singletonList(entry));
            } catch (Exception ignored) {
                // compilation may fail if called outside a proper client context
            }
        }
    }
}
