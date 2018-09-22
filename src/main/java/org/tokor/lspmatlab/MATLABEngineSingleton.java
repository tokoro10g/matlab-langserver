package org.tokor.lspmatlab;

import com.mathworks.engine.MatlabEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Writer;
import java.util.concurrent.Future;

public class MATLABEngineSingleton {
    private static MATLABEngineSingleton instance = null;

    public static final Logger logger = LoggerFactory.getLogger(MATLABEngineSingleton.class);
    public MatlabEngine engine;
    private Future<MatlabEngine> future;

    private MATLABEngineSingleton() {
        String[] engines = {};
        try {
            engines = MatlabEngine.findMatlab();
        } catch (Exception e) {
            logger.info("", e);
        }
        if (engines.length == 0) {
            logger.info("Could not find existing sessions. Starting MATLAB Engine...");
            String[] options = {"-r", "matlab.engine.shareEngine"};
            this.future = MatlabEngine.startMatlabAsync(options);
        } else {
            logger.info("Connecting to MATLAB Engine (" + engines[0] + ")...");
            this.future = MatlabEngine.connectMatlabAsync(engines[0]);
        }
        this.engine = null;
    }

    public boolean isEngineReady() {
        if (future.isDone()) {
            try {
                engine = future.get();
            } catch (Exception e) {
                // TODO: handle properly
                return false;
            }
        }
        return engine != null;
    }

    public Object evalInMATLAB(String statement) {
        return evalInMATLAB(statement, null, null, null);
    }

    public Object evalInMATLAB(String statement, Writer writer) {
        return evalInMATLAB(statement, null, null, null, writer);
    }

    public Object evalInMATLAB(String statement, String varIn, Object valueIn, String varOut) {
        return evalInMATLAB(statement, varIn, valueIn, varOut, MatlabEngine.NULL_WRITER);
    }

    public Object evalInMATLAB(String statement, String varIn, Object valueIn, String varOut, Writer writer) {
        if (!isEngineReady()) {
            logger.info("MATLAB Engine not ready");
            return null;
        }
        Object result = null;
        try {
            if (varIn != null) {
                engine.putVariable(varIn, valueIn);
            }
            engine.eval(statement, writer, writer);
            if (varOut != null) {
                result = engine.getVariable(varOut);
            }
        } catch (Exception e) {
            logger.info("", e);
            return null;
        }
        return result;
    }

    public static MATLABEngineSingleton getInstance() {
        if (instance == null) {
            instance = new MATLABEngineSingleton();
        }
        return instance;
    }
}
