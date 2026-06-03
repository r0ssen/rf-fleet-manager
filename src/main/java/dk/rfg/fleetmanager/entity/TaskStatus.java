package dk.rfg.fleetmanager.entity;
public enum TaskStatus {
    ORDERED, STARTED, DONE, CANCELLED;
    public String danishLabel() {
        return switch (this) {
            case ORDERED   -> "Bestilt";
            case STARTED   -> "Startet";
            case DONE      -> "Udført";
            case CANCELLED -> "Slettet";
        };
    }
}
