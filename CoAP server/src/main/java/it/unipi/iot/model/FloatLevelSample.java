package it.unipi.iot.model;

public class FloatLevelSample {
    private int node;
    private boolean lowLevel;

    public FloatLevelSample(int node, boolean lowLevel) {
        this.node = node;
        this.lowLevel = lowLevel;
    }

    public boolean getLowLevel() {
        return lowLevel;
    }

    public void setQuantity(boolean lowLevel) {
        this.lowLevel = lowLevel;
    }

    @Override
    public String toString() {
        return "PresenceSample{ " +
                "lowLevel=" + lowLevel +
                '}';
    }

    public int getNode() {
        return node;
    }

    public void setNode(int node) {
        this.node = node;
    }
}
