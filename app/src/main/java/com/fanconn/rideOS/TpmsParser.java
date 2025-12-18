package com.fanconn.rideOS;


public class TpmsParser {

    /**
     * @param raw BLE advertising bytes
     * @return parsed TPMS data or invalid()
     */
    public static TpmsParsedData parse(byte[] raw) {

        if (raw == null || raw.length == 0) {
            return TpmsParsedData.invalid();
        }

        String[] b = bytesToHex(raw).split(" ");

        try {
            /* ================= SENASIC ================= */
            if (b.length >= 25 && "4D".equalsIgnoreCase(b[12])) {

                // Pressure
                int pVal = Integer.parseInt(b[22], 16);
                double psi = (pVal * 3.13) / 6.895;

                // Temperature
                int temp = Integer.parseInt(b[23], 16);

                // Voltage (Hex → Octal → Voltage)
                int battRaw = Integer.parseInt(b[24], 16);
                int oct = Integer.parseInt(Integer.toOctalString(battRaw));
                double voltage = oct / 100.0;

                boolean lowBattery = voltage < 2.40;

                return new TpmsParsedData(
                        psi,
                        temp,
                        voltage,
                        lowBattery,
                        TpmsParsedData.TYPE_SENASIC
                );
            }

            /* ================= iBEACON ================= */
            if (b.length >= 29 && "4C".equalsIgnoreCase(b[5])) {

                int minorHigh = Integer.parseInt(b[27], 16);
                int minorLow  = Integer.parseInt(b[28], 16);

                int minor = (minorHigh << 8) | minorLow;

                int rawP = (minor >> 8) & 0xFF;
                int rawT = minor & 0xFF;

                double psi = rawP * 2.5 * 0.145038;
                int temp   = rawT - 40;

                // iBeacon has NO real voltage, only low-battery flag
                boolean lowBattery = "7F".equalsIgnoreCase(b[28]);

                return new TpmsParsedData(
                        psi,
                        temp,
                        -1,              // voltage not available
                        lowBattery,
                        TpmsParsedData.TYPE_IBEACON
                );
            }

        } catch (Exception e) {
            // silently ignore malformed packets
        }

        return TpmsParsedData.invalid();
    }

    /* ================= UTILS ================= */

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}
