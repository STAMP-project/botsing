package eu.stamp.botsing.model.generation.model;

import be.vibes.dsl.io.Xml;
import be.vibes.ts.UsageModel;
import be.yami.exception.ModelGenerationException;
import be.yami.exception.SessionBuildException;
import be.yami.java.ClassMethodParametersKeyGenerator;
import be.yami.java.JsonMethodCallsSequenceBuilder;
import be.yami.java.MethodCallSequence;
import be.yami.java.MultipleModelsProcessor;
import be.yami.ngram.Bigram;
import be.yami.ngram.NGram;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.stamp.botsing.model.generation.callsequence.MethodCall;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModelGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(ModelGenerator.class);

    public void generate(Map<String, Set<List<MethodCall>>> pool, String outputFolder) throws IOException,
            SessionBuildException {
        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(pool, pool.getClass());
        // The bigram which will construct the model
        final MultipleModelsProcessor processor = new MultipleModelsProcessor() {
            @Override
            protected NGram<be.yami.java.MethodCall> buildNewNGram(String name) {
                LOG.trace("Bigram created for class {}", name);
                return new Bigram<>(name, ClassMethodParametersKeyGenerator.getInstance());
            }
        };

        JsonMethodCallsSequenceBuilder builder = JsonMethodCallsSequenceBuilder.newInstance();
        builder.addListener(processor);

        final List<Integer> sizes = Lists.newArrayList();
        builder.addListener((MethodCallSequence seq) -> {
            sizes.add(seq.size());
            LOG.trace("Sequences processed: {}", sizes.size());
        });

        InputStream in = IOUtils.toInputStream(json, "UTF-8");
        builder.buildSessions(in);
        File outFolder = new File(outputFolder.replace(".JSON", "").replace(".Json", ""));
        LOG.trace("Printing models in folder {}", outFolder);
        if(!outFolder.exists()) {
            outFolder.mkdirs();
        }
        processor.getNGrams().forEach((ngram) -> {
            try {
                LOG.info("Printing model {}", ngram.getName());
                UsageModel um = ngram.getModel();
                File output = new File(outFolder, ngram.getName() + ".xml");
                Xml.print(um, output);
            } catch(ModelGenerationException ex) {
                LOG.error("Exception while retrieving usage model for {}!", ngram.getName(), ex);
            }
        });
    }
}
