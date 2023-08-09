package FunctionStore.Function.Utils;

import java.util.HashSet;
import java.util.List;

public class LocalPath {
    private List<String> path;

    public LocalPath(List<String> path) {
        this.path = path;
    }

    public List<String> getPath() {
        return path;
    }

    public String get(int i){
        return this.path.get(i);
    }

    public String getLast() {
        return this.path.get(this.path.size()-1);
    }

    public int size(){
        return this.path.size();
    }

    public boolean contains(String relation){
        return this.path.contains(relation);
    }

    public boolean isLast(String relation){
        return this.path.get(this.path.size()-1).equals(relation);
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof LocalPath path && path.size() == this.size()){
            return new HashSet<>(this.getPath()).containsAll(path.getPath());
        }

        return false;
    }

    @Override
    public String toString(){
        StringBuilder tmp = new StringBuilder();

        for(String string : path){
            tmp.append(string).append(" ");
        }

        return tmp.toString().trim();
    }
}
