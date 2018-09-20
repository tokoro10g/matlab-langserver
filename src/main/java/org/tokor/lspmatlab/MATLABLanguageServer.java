package org.tokor.lspmatlab;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public final class MATLABLanguageServer implements LanguageServer, LanguageClientAware {

    private static final Logger logger = LoggerFactory.getLogger(MATLABLanguageServer.class);

    private final Runnable shutdownHandler;
    private LanguageClient client;

    @Override
    public void initialized(InitializedParams params) {
        client.logMessage(new MessageParams(MessageType.Info, "hello, world!"));
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
        signatureHelpOptions.setTriggerCharacters(Arrays.asList("("));
        capabilities.setSignatureHelpProvider(signatureHelpOptions);

        capabilities.setDefinitionProvider(true);

        InitializeResult result = new InitializeResult(capabilities);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        logger.info("shutdown received");
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
        logger.info("exit received");
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
        return null;
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = client;
    }
}
