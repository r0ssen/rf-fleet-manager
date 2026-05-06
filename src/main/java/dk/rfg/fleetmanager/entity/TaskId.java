package dk.rfg.fleetmanager.entity;
import java.io.Serializable;
public record TaskId(int festivalYear, int taskId) implements Serializable {}
