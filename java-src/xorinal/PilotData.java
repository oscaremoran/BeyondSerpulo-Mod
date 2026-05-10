package xorinal;

public class PilotData {
    public String name;
    public String unitTypeName;
    public float baseHealth = 150f;
    public int killPoints;
    public boolean alive = true;
    public transient int activeUnitId = -1;

    public PilotData() {}

    public int threshold(int tier) {
        return Math.round(baseHealth * 3f * tier);
    }

    public int tier() {
        if (killPoints >= threshold(3)) return 3;
        if (killPoints >= threshold(2)) return 2;
        if (killPoints >= threshold(1)) return 1;
        return 0;
    }

    public PilotData copy() {
        PilotData p = new PilotData();
        p.name = name; p.unitTypeName = unitTypeName;
        p.baseHealth = baseHealth;
        p.killPoints = killPoints; p.alive = alive;
        return p;
    }
}
