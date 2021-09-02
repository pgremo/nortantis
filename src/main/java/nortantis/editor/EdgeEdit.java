package nortantis.editor;

import java.io.Serializable;

public class EdgeEdit implements Serializable {

    public int riverLevel;
    public final int index;

    public EdgeEdit(int index, int riverLevel) {
        this.riverLevel = riverLevel;
        this.index = index;
    }
}
