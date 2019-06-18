package org.tokor.lspmatlab;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public final class MATLABLanguageServer implements LanguageServer, LanguageClientAware {

    private static final Logger logger = LoggerFactory.getLogger(MATLABLanguageServer.class);

    private final Runnable shutdownHandler;
    private LanguageClient client;

    @Override
    public void initialized(InitializedParams params) {
        client.logMessage(new MessageParams(MessageType.Info, "MATLAB Language Server Initialized"));
    }

    public MATLABLanguageServer(Runnable shutdownHandler) {
        this.shutdownHandler = shutdownHandler;
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        ServerCapabilities capabilities = new ServerCapabilities();
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);

        CompletionOptions completionOptions = new CompletionOptions();
        completionOptions.setResolveProvider(true);
        capabilities.setCompletionProvider(completionOptions);

        SignatureHelpOptions signatureHelpOptions = new SignatureHelpOptions();
        signatureHelpOptions.setTriggerCharacters(Collections.singletonList("("));
        capabilities.setSignatureHelpProvider(signatureHelpOptions);

        capabilities.setDefinitionProvider(true);

        capabilities.setCodeActionProvider(true);
        ExecuteCommandOptions executeCommandOptions = new ExecuteCommandOptions(
                Arrays.asList(
                        "engine.run",
                        "engine.runSection",
                        "engine.runLine"
                )
        );
        capabilities.setExecuteCommandProvider(executeCommandOptions);

        WorkspaceServerCapabilities workspace = new WorkspaceServerCapabilities();
        WorkspaceFoldersOptions foldersOptions = new WorkspaceFoldersOptions();
        foldersOptions.setSupported(true);
        foldersOptions.setChangeNotifications(true);
        workspace.setWorkspaceFolders(foldersOptions);
        capabilities.setWorkspace(workspace);

        InitializeResult result = new InitializeResult(capabilities);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        logger.info("Shutdown Received");
        try {
            MATLABEngineSingleton.getInstance().engine.close();
        } catch (Exception e) {
            logger.info("", e);
        }
        shutdownHandler.run();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        logger.info("Exit Received");
        try {
            MATLABEngineSingleton.getInstance().engine.close();
        } catch (Exception e) {
            logger.info("", e);
        }
        shutdownHandler.run();
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        MATLABEngineSingleton.getInstance();
        return new MATLABTextDocumentService();
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return new MATLABWorkspaceService();
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = client;
    }
}
