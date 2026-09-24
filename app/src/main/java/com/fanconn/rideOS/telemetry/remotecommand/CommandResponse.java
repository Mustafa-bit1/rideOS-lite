package com.fanconn.rideOS.telemetry.remotecommand;

public class CommandResponse {

    private String commandId;
    private String vin;
    private String command;
    private String status;
    private String message;
    private String timestamp;

    public CommandResponse(
            String commandId,
            String vin,
            String command,
            String status,
            String message,
            String timestamp) {

        this.commandId = commandId;
        this.vin = vin;
        this.command = command;
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getCommandId() {
        return commandId;
    }

    public String getVin() {
        return vin;
    }

    public String getCommand() {
        return command;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }
}

