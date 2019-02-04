package eu.stamp.botsing.graphs.cfg;

import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BotsingControlFlowGraph extends RawControlFlowGraph {
    private static Logger LOG = LoggerFactory.getLogger(BotsingControlFlowGraph.class);

    public BotsingControlFlowGraph() {
        super(null, null, null, 0);
    }
}
