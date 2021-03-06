package ml.dmlc.xgboost4j.java;

import hex.tree.xgboost.XGBoostModel;
import hex.tree.xgboost.matrix.DenseMatrixFactory;
import org.apache.log4j.Logger;
import water.*;

public class XGBoostUpdateTask extends AbstractXGBoostTask<XGBoostUpdateTask> {

    private static final Logger LOG = Logger.getLogger(DenseMatrixFactory.class);

    private final int _tid;

    public XGBoostUpdateTask(XGBoostSetupTask setupTask, int tid) {
        super(setupTask);
        _tid = tid;
    }

    @Override
    protected void execute() {
        Booster booster = XGBoostUpdater.getUpdater(_modelKey).doUpdate(_tid);
        if (booster == null)
            throw new IllegalStateException("Boosting iteration didn't produce a valid Booster.");
    }

    // This is called from driver
    public byte[] getBoosterBytes() {
        final H2ONode boosterNode = getBoosterNode();
        final byte[] boosterBytes;
        if (H2O.SELF.equals(boosterNode)) {
            boosterBytes = XGBoostUpdater.getUpdater(_modelKey).getBoosterBytes();
        } else {
            LOG.debug("Booster will be retrieved from a remote node, node=" + boosterNode);
            FetchBoosterTask t = new FetchBoosterTask(_modelKey);
            boosterBytes = new RPC<>(boosterNode, t).call().get()._boosterBytes;
        }
        return boosterBytes;
    }

    private static class FetchBoosterTask extends DTask<FetchBoosterTask> {
        private final Key<XGBoostModel> _modelKey;

        // OUT
        private byte[] _boosterBytes;

        private FetchBoosterTask(Key<XGBoostModel> modelKey) {
            _modelKey = modelKey;
        }

        @Override
        public void compute2() {
            _boosterBytes = XGBoostUpdater.getUpdater(_modelKey).getBoosterBytes();
            tryComplete();
        }
    }

}
