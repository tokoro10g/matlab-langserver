package org.tokor.lspmatlab;

import com.mathworks.engine.FutureMatlab;
import com.mathworks.engine.MatlabEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

public class MATLABEngineSingleton {
    private static MATLABEngineSingleton instance = null;

    public static final Logger logger = LoggerFactory.getLogger(MATLABEngineSingleton.class);
    public MatlabEngine engine;
    private Future<MatlabEngine> future;

    private MATLABEngineSingleton() {
        this.future = MatlabEngine.startMatlabAsync();
        this.engine = null;
        logger.info("Starting MATLAB Engine...");
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

    public static MATLABEngineSingleton getInstance() {
        if (instance == null) {
            instance = new MATLABEngineSingleton();
        }
        return instance;
    }
}
