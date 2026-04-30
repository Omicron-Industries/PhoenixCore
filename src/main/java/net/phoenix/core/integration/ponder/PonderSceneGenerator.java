package net.phoenix.core.integration.ponder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PonderSceneGenerator {

    private final StringBuilder scriptBuilder = new StringBuilder();
    private String modId;
    private String sceneId;
    private String title;
    private String structureId;
    private final List<String> sceneSteps = new ArrayList<>();

    public PonderSceneGenerator(String modId, String sceneId, String title, String structureId) {
        this.modId = modId;
        this.sceneId = sceneId;
        this.title = title;
        this.structureId = structureId;
    }

    public void setMetadata(String modId, String sceneId, String title, String structureId) {
        this.modId = modId;
        this.sceneId = sceneId;
        this.title = title;
        this.structureId = structureId;
    }

    public PonderSceneGenerator addBlock(int x, int y, int z, String blockId) {
        sceneSteps.add(String.format("    let pos%s%s%s = p.pos(%d, %d, %d);", x, y, z, x, y, z));
        sceneSteps.add(String.format("    p.block(pos%s%s%s, '%s');", x, y, z, blockId));
        return this;
    }

    public PonderSceneGenerator addText(String text, int x, int y, int z, String palette) {
        sceneSteps
                .add(String.format("    p.text('%s', p.pos(%d, %d, %d), { palette: '%s' });", text, x, y, z, palette));
        return this;
    }

    public PonderSceneGenerator addIdle(int ticks) {
        sceneSteps.add(String.format("    p.idle(%d);", ticks));
        return this;
    }

    public String generateScript() {
        scriptBuilder.setLength(0);
        scriptBuilder.append("Ponder.registry((event) => {\n");
        scriptBuilder.append(String.format("  GTPonder.scene(event, '%s:%s', '%s', '%s', (p) => {\n",
                modId, sceneId, structureId, title));

        for (String step : sceneSteps) {
            scriptBuilder.append(step).append("\n");
        }

        scriptBuilder.append("  });\n");
        scriptBuilder.append("});\n");
        return scriptBuilder.toString();
    }

    public void writeScript(Path outputPath) throws IOException {
        Files.writeString(outputPath, generateScript());
    }
}
