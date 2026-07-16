package com.fanconn.rideOS;

public class PetrolCostManager {

    private static final double PETROL_PRICE = 110.0;
    private static final double PETROL_MILEAGE = 45.0;

    private static final double EV_CHARGE_COST = 30.0;
    private static final double EV_RANGE = 170.0;

    private static final double PETROL_COST_PER_KM = PETROL_PRICE / PETROL_MILEAGE; // 2.26
    private static final double EV_COST_PER_KM = EV_CHARGE_COST / EV_RANGE;         // 0.176

    public static double getPetrolSavedLitres(double km) {
        return km / PETROL_MILEAGE;
    }

    public static double getChargingCost(double odoKm, double regenKm) {
        double effectiveKm = Math.max(0, odoKm - regenKm);
        return effectiveKm * EV_COST_PER_KM;
    }

    public static double getMoneySaved(double odoKm, double regenKm) {
        double effectiveKm = Math.max(0, odoKm - regenKm);
        return (odoKm * PETROL_COST_PER_KM) - (effectiveKm * EV_COST_PER_KM);
    }
}