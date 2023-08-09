package DataManagement.Clustering;

import org.opencompare.hac.experiment.Experiment;

import java.util.List;

public class GapExperiment implements Experiment {
    private List<String> elements;

    public GapExperiment(List<String> elements) {
        this.elements = elements;
    }

    @Override
    public int getNumberOfObservations() {
        return elements.size();
    }

    public String get(int i){
        return elements.get(i);
    }
}
