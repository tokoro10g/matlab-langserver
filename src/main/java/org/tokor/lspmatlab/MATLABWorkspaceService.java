package org.tokor.lspmatlab;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.CompletableFutures;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MATLABWorkspaceService implements WorkspaceService {

    public static final Logger logger = LoggerFactory.getLogger(MATLABWorkspaceService.class);

    private void engineRun(String src, StringWriter writer) {
        MATLABEngineSingleton.getInstance().evalInMATLAB(src, writer);
    }

    private void engineRunSection(String src, int lineNumber, StringWriter writer) {
        int cursor = lineNumber;
        String[] lines = src.split("\n");
        String line = lines[lineNumber];
        StringBuilder codeBuilder = new StringBuilder(line);
        while (cursor > 0 && !line.matches("^\\s*%%")) {
            cursor--;
            line = lines[cursor];
            codeBuilder.insert(0, line + "\n");
        }
        if (lineNumber + 1 < lines.length) {
            cursor = lineNumber + 1;
            line = lines[cursor];
            codeBuilder.append("\n" + line);
            while (cursor < lines.length - 1 && !line.matches("^\\s*%%")) {
                cursor++;
                line = lines[cursor];
                codeBuilder.append("\n" + line);
            }
        }
        //logger.info("line no:" + lineNumber + ", str:" + line);
        MATLABEngineSingleton.getInstance().evalInMATLAB(codeBuilder.toString(), writer);
    }

    private void engineRunLine(String src, int lineNumber, StringWriter writer) {
        String line = src.split("\n")[lineNumber];
        logger.info("line no:" + lineNumber + ", str:" + line);
        MATLABEngineSingleton.getInstance().evalInMATLAB(line, writer);
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {

    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {

    }

    @Override
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
        return CompletableFutures.computeAsync(checker -> {
            String command = params.getCommand();
            List<Object> args = params.getArguments();
            StringWriter writer = new StringWriter();
            switch (command) {
                case "engine.run":
                    engineRun(((JsonObject) args.get(0)).get("value").getAsString(), writer);
                    break;
                case "engine.runSection":
                    engineRunSection(((JsonObject) args.get(0)).get("value").getAsString(), ((JsonPrimitive) args.get(2)).getAsInt(), writer);
                    break;
                case "engine.runLine":
                    engineRunLine(((JsonObject) args.get(0)).get("value").getAsString(), ((JsonPrimitive) args.get(2)).getAsInt(), writer);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + command);
            }
            String result = writer.toString();
            logger.info("Result:\n" + result);
            return result;
        });
    }

    @Override
    public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
        List<WorkspaceFolder> newWorkspace = params.getEvent().getAdded();
        String location = newWorkspace.get(0).getUri().replaceAll("file://", "");
        logger.info("Folder changed to: " + location);
        if (MATLABEngineSingleton.getInstance().isEngineReady()) {
            MATLABEngineSingleton.getInstance().evalInMATLAB("cd " + location);
        }
    }
}
