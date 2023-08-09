package DataManagement.Clustering;

import org.opencompare.hac.experiment.DissimilarityMeasure;
import org.opencompare.hac.experiment.Experiment;

public class TwoGramOverlapDissimilarity implements DissimilarityMeasure {
    @Override
    public double computeDissimilarity(Experiment experiment, int i, int j) {
        GapExperiment gapExperiment = (GapExperiment) experiment;
        return (1 - org.sotorrent.stringsimilarity.set.Variants.twoGramOverlapNormalized(gapExperiment.get(i), gapExperiment.get(j)));
    }
}
