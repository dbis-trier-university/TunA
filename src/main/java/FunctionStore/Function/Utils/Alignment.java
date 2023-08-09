package FunctionStore.Function.Utils;

import java.util.List;
import java.util.Objects;

public class Alignment {
    private List<LocalPath> localRelations;
    private List<String> remoteRelations;
    private double reliability;

    public Alignment(List<LocalPath> localRelations, List<String> remoteRelations, double reliability) {
        this.localRelations = localRelations;
        this.remoteRelations = remoteRelations;
        this.reliability = reliability;
    }

    public List<LocalPath> getLocalRelations() {
        return localRelations;
    }

    public List<String> getRemoteRelations() {
        return remoteRelations;
    }

    public String getRemoteRelation(String relation) {
        int i = 0;
        for (; i < localRelations.size(); i++) {
            if(localRelations.get(i).isLast(relation)) break;
        }

        return remoteRelations.get(i);
    }

    public double getReliability() {
        return reliability;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Alignment alignment = (Alignment) o;
        return Double.compare(alignment.reliability, reliability) == 0
                && Objects.equals(localRelations, alignment.localRelations)
                && Objects.equals(remoteRelations, alignment.remoteRelations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localRelations, remoteRelations, reliability);
    }

    public static boolean overlaps(Alignment a1, Alignment a2){
        for(LocalPath path : a1.getLocalRelations()){
            if(a2.getLocalRelations().contains(path)) return true;
        }

        for(LocalPath path : a2.getLocalRelations()){
            if(a1.getLocalRelations().contains(path)) return true;
        }

        return false;
    }
}
