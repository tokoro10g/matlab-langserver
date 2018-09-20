package org.tokor.lspmatlab;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        String command = params.getCommand();
        List<Object> args = params.getArguments();

        logger.info("command: ", command, args);
        return null;
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
