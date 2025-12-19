package io.github.maxencedc.sparsestructures.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.maxencedc.sparsestructures.Constants;
import io.github.maxencedc.sparsestructures.StructureSetsSet;
import net.minecraft.util.Formatting;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import static net.minecraft.server.command.CommandManager.literal;


public class DumpStructureSetsCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {

        dispatcher.register(
            literal("dumpstructuresets")
                .executes(context -> {
                    context.getSource().sendFeedback(() -> Text.translatable("command.sparsestructures.dump.dumping"), false);
                    String fileName = new SimpleDateFormat("'structure_sets_dump_'yy_MM_dd_HH_mm'.txt'").format(new Date());
                    try {
                        dumpStructureSets(fileName);
                        context.getSource().sendFeedback(() -> {
                            boolean isDedicatedServer = context.getSource().getServer().isDedicated();
                            Text underlinedFile = Text.literal(fileName).formatted(Formatting.UNDERLINE);
                            String message = Text.translatable("command.sparsestructures.dump.success", underlinedFile).getString();
                            if (isDedicatedServer) {
                                message += "\n" + Text.translatable("command.sparsestructures.dump.server").getString();
                            }
                            return Text.literal(message);
                        }, false);
                        return 1;
                    } catch (IOException e) {
                        context.getSource().sendError(Text.translatable("command.sparsestructures.dump.failure"));
                        Constants.LOG.error("Failed to dump structure sets\n", e);
                        return 0;
                    }
                })
        );
    }

    private static void dumpStructureSets(String fileName) throws IOException {
        Path dumpPath = Path.of(Constants.MOD_ID);
        StringBuilder dump = new StringBuilder();
        StructureSetsSet.structureSets.forEach(s -> dump.append("{\n  \"structure\": \"").append(s).append("\",\n  \"factor\": 1//REPLACE WITH YOUR CUSTOM SPREADING FACTOR HERE\n},\n"));
        Files.createDirectories(dumpPath);
        Files.writeString(dumpPath.resolve(fileName), dump.toString());
    }
}
