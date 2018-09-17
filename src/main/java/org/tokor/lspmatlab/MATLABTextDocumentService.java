package org.tokor.lspmatlab;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.CompletableFutures;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.mathworks.engine.*;

public class MATLABTextDocumentService implements TextDocumentService {

    public static final Logger logger = LoggerFactory.getLogger(MATLABTextDocumentService.class);

    private AtomicReference<String> src = new AtomicReference(ImmutableList.of());

    private static final Map<String, CompletionItemKind> kindMap = ImmutableMap.<String, CompletionItemKind>builder()
            .put("method", CompletionItemKind.Method)
            .put("mFile", CompletionItemKind.Function)
            .put("pFile", CompletionItemKind.Function)
            .put("package", CompletionItemKind.Module)
            .put("variable", CompletionItemKind.Variable)
            .put("mex", CompletionItemKind.Function)
            .put("slxFile", CompletionItemKind.File)
            .put("mdlFile", CompletionItemKind.File)
            .put("folder", CompletionItemKind.Folder)
            .put("filename", CompletionItemKind.File)
            .put("pathItem", CompletionItemKind.File)
            .put("literal", CompletionItemKind.Property)
            .put("logical", CompletionItemKind.Value)
            .put("fieldname", CompletionItemKind.Field)
            .build();

    private static int lineNumToByteNum(String str, int lineIndex, int charIndex) {
        int pos = 0;
        while (lineIndex-- > 0)
            pos = str.indexOf('\n', pos + 1);
        if (pos == -1) return charIndex;
        return pos + charIndex + (pos == 0 ? 0 : 1);
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
        return CompletableFutures.computeAsync(checker -> {
            String src = this.src.get();
            int currentLineIndex = position.getPosition().getLine();
            int currentCharIndex = position.getPosition().getCharacter();

            int byteIndex = lineNumToByteNum(src, currentLineIndex, currentCharIndex);

            logger.info("l" + Integer.toString(currentLineIndex) + ":" + Integer.toString(currentCharIndex) + "/" + Integer.toString(byteIndex));

            if (!MATLABEngineSingleton.getInstance().isEngineReady()) {
                logger.info("MATLAB Engine not ready");
                return Either.forLeft(new ArrayList());
            }

            String rs;
            try {
                MatlabEngine eng = MATLABEngineSingleton.getInstance().engine;
                eng.putVariable("str___", src);
                eng.eval("import com.mathworks.jmi.tabcompletion.*;" +
                        "tc___ = TabCompletionImpl();" +
                        "f___ = tc___.getJSONCompletions(str___, " + Integer.toString(byteIndex) + ");" +
                        "while ~f___.isDone(); pause(0.01); end;" +
                        "result___ = f___.get();", MatlabEngine.NULL_WRITER, MatlabEngine.NULL_WRITER);
                rs = eng.getVariable("result___");
            } catch (Exception e) {
                logger.info("", e);
                return Either.forLeft(new ArrayList());
            }

            JSONObject obj = new JSONObject(rs);
            JSONArray arr;
            List<CompletionItem> comp = new ArrayList<>();
            try {
                arr = obj.getJSONArray("finalCompletions");
                int cnt = 0;
                for (Object o : arr) {
                    String label = (String) ((JSONObject) o).get("popupCompletion");
                    //logger.info("item:" + label);
                    CompletionItem item = new CompletionItem(label);
                    item.setLabel(label);
                    item.setInsertText(label);
                    CompletionItemKind kind;
                    try {
                        kind = kindMap.get((String) ((JSONObject) o).get("matchType"));
                    } catch (Exception e) {
                        kind = CompletionItemKind.Text;
                    }
                    item.setKind(kind);
                    item.setData(++cnt);
                    comp.add(item);
                }
            } catch (JSONException je) {
                logger.info("Invalid result", je);
                return Either.forLeft(new ArrayList());
            }
            return Either.forLeft(comp);
        });
    }

    @Override
    public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Hover> hover(TextDocumentPositionParams position) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<SignatureHelp> signatureHelp(TextDocumentPositionParams position) {
        return CompletableFutures.computeAsync(checker -> {
            String src = this.src.get();
            int currentLineIndex = position.getPosition().getLine();
            int currentCharIndex = position.getPosition().getCharacter();

            int byteIndex = lineNumToByteNum(src, currentLineIndex, currentCharIndex);

            logger.info("l" + Integer.toString(currentLineIndex) + ":" + Integer.toString(currentCharIndex) + "/" + Integer.toString(byteIndex));

            if (!MATLABEngineSingleton.getInstance().isEngineReady()) {
                logger.info("MATLAB Engine not ready");
                return null;
            }

            String rs;
            SignatureHelp signatureHelp = null;
            try {
                MatlabEngine eng = MATLABEngineSingleton.getInstance().engine;
                eng.putVariable("str___", src.substring(0, byteIndex));
                eng.eval("import com.mathworks.mlwidgets.help.functioncall.*;" +
                        "fc___ = MFunctionCall.getInstance(str___);" +
                        "result___ = char(fc___.createSignatureString());", MatlabEngine.NULL_WRITER, MatlabEngine.NULL_WRITER);
                rs = eng.getVariable("result___");

                List<SignatureInformation> items = new ArrayList<>();

                final Matcher matcher = Pattern.compile("<div>(.+?)</div>").matcher(rs);
                int activeSignature = 0;
                int activeParameter = 0;
                while(matcher.find()) {
                    String item = matcher.group(1);

                    int boldIndex = item.indexOf("<b>");
                    if (boldIndex > -1){
                        int commaIndex = 0, activeArgIndex = -1;
                        while (commaIndex != -1 && commaIndex < boldIndex) {
                            commaIndex = item.indexOf(",", commaIndex + 1);
                            activeArgIndex++;
                        }
                        if (activeParameter < activeArgIndex)
                            activeParameter = activeArgIndex;
                    } else {
                        activeParameter = -1;
                    }

                    item = item.replaceAll("\\<[^>]*>","");
                    SignatureInformation info = new SignatureInformation(item);

                    final Matcher matcher2 = Pattern.compile("\\((.*?)\\)").matcher(item);
                    if(matcher2.find()) {
                        info.setParameters(Arrays.asList(matcher2.group(1).split(",")).stream().map(ParameterInformation::new).collect(Collectors.toList()));
                    }
                    items.add(info);
                }
                signatureHelp = new SignatureHelp(items, activeSignature, activeParameter);

                logger.info(rs);
            } catch (Exception e) {
                logger.info("", e);
                return null;
            }
            return signatureHelp;
        });
    }

    @Override
    public CompletableFuture<List<? extends Location>> definition(TextDocumentPositionParams position) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(TextDocumentPositionParams position) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams params) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> onTypeFormatting(DocumentOnTypeFormattingParams params) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        src.set(params.getTextDocument().getText());
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        for (TextDocumentContentChangeEvent changeEvent : params.getContentChanges()) {
            // Will be full update because we specified that is all we support
            if (changeEvent.getRange() != null) {
                throw new UnsupportedOperationException("Range should be null for full document update.");
            }
            if (changeEvent.getRangeLength() != null) {
                throw new UnsupportedOperationException("RangeLength should be null for full document update.");
            }
            src.set(changeEvent.getText());
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {

    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {

    }
}
