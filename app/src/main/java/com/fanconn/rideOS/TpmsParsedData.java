package com.fanconn.rideOS;

public class TpmsParsedData {

    public static final int TYPE_INVALID = 0;
    public static final int TYPE_SENASIC = 1;
    public static final int TYPE_IBEACON = 2;

    public final double psi;
    public final int temperature;
    public final double voltage;     // -1 if not available
    public final boolean lowBattery;
    public final int type;
    public final boolean valid;

    public TpmsParsedData(double psi, int temperature,
                          double voltage, boolean lowBattery, int type) {
        this.psi = psi;
        this.temperature = temperature;
        this.voltage = voltage;
        this.lowBattery = lowBattery;
        this.type = type;
        this.valid = true;
    }

    private TpmsParsedData() {
        this.psi = 0;
        this.temperature = 0;
        this.voltage = -1;
        this.lowBattery = false;
        this.type = TYPE_INVALID;
        this.valid = false;
    }

    public static TpmsParsedData invalid() {
        return new TpmsParsedData();
    }
}
