package Configuration.Database;

public class DbObject {
    private String label, indexPath, sourcePath;

    public DbObject(String label, String indexPath, String sourcePath) {
        this.label = label;
        this.indexPath = indexPath;
        this.sourcePath = sourcePath;
    }

    /*******************************************************************************************************************
     * Getter and Setter
     ******************************************************************************************************************/

    public String getLabel() {
        return label;
    }

    public String getIndexPath() {
        return indexPath;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setIndexPath(String indexPath) {
        this.indexPath = indexPath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }
}
