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
                    MATLABEngineSingleton.getInstance().evalInMATLAB(((JsonObject) args.get(0)).get("value").getAsString(), writer);
                    break;
                case "engine.runLine":
                    String src = ((JsonObject) args.get(0)).get("value").getAsString();
                    int lineNumber = ((JsonPrimitive) args.get(2)).getAsInt();
                    String line = src.split("\n")[lineNumber];
                    logger.info("line no:" + Integer.toString(lineNumber) + ", str:" + line);
                    MATLABEngineSingleton.getInstance().evalInMATLAB(line, writer);
                    break;
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
