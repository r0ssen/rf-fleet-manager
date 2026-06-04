package dk.rfg.fleetmanager.entity;
import jakarta.persistence.*;
import jakarta.persistence.Version;
import java.math.BigDecimal;

@Entity
@Table(name = "vehicles")
@IdClass(VehicleId.class)
public class Vehicle {

    @Id @Column(name = "festival_year", columnDefinition = "SMALLINT")
    private int festivalYear;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vehicles_vehicle_id_seq_gen")
    @SequenceGenerator(name = "vehicles_vehicle_id_seq_gen", sequenceName = "vehicles_vehicle_id_seq", allocationSize = 1)
    @Column(name = "vehicle_id")
    private int vehicleId;
    @Column(name = "vehicle_no", nullable = false) private String vehicleNo;
    @Column(name = "registration")  private String registration;
    @Column(name = "model")         private String model;
    @Column(name = "total_weight_kg", precision = 8) private BigDecimal totalWeightKg;
    @Column(name = "payload_kg",      precision = 8) private BigDecimal payloadKg;
    @Column(name = "vehicle_length_mm", precision = 7) private BigDecimal vehicleLengthMm;
    @Column(name = "vehicle_width_mm",  precision = 7) private BigDecimal vehicleWidthMm;
    @Column(name = "vehicle_height_mm", precision = 7) private BigDecimal vehicleHeightMm;
    @Column(name = "cargo_length_mm",   precision = 7) private BigDecimal cargoLengthMm;
    @Column(name = "cargo_width_mm",    precision = 7) private BigDecimal cargoWidthMm;
    @Column(name = "cargo_height_mm",   precision = 7) private BigDecimal cargoHeightMm;
    @Column(name = "has_pallet_lifter", nullable = false) private boolean hasPalletLifter;
    @Column(name = "has_sack_truck",    nullable = false) private boolean hasSackTruck;
    @Column(name = "description") private String description;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public Vehicle() {}
    public String displayLabel() { return "Bil " + vehicleNo + (model != null ? ": " + model : ""); }

    public int getFestivalYear()  { return festivalYear; }
    public void setFestivalYear(int v) { this.festivalYear = v; }
    public int getVehicleId()     { return vehicleId; }
    public void setVehicleId(int v)    { this.vehicleId = v; }
    public String getVehicleNo()  { return vehicleNo; }
    public void setVehicleNo(String v) { this.vehicleNo = v; }
    public String getRegistration() { return registration; }
    public void setRegistration(String v) { this.registration = v; }
    public String getModel()      { return model; }
    public void setModel(String v)     { this.model = v; }
    public BigDecimal getTotalWeightKg()    { return totalWeightKg; }
    public void setTotalWeightKg(BigDecimal v)    { this.totalWeightKg = v; }
    public BigDecimal getPayloadKg()        { return payloadKg; }
    public void setPayloadKg(BigDecimal v)        { this.payloadKg = v; }
    public BigDecimal getVehicleLengthMm()  { return vehicleLengthMm; }
    public void setVehicleLengthMm(BigDecimal v)  { this.vehicleLengthMm = v; }
    public BigDecimal getVehicleWidthMm()   { return vehicleWidthMm; }
    public void setVehicleWidthMm(BigDecimal v)   { this.vehicleWidthMm = v; }
    public BigDecimal getVehicleHeightMm()  { return vehicleHeightMm; }
    public void setVehicleHeightMm(BigDecimal v)  { this.vehicleHeightMm = v; }
    public BigDecimal getCargoLengthMm()    { return cargoLengthMm; }
    public void setCargoLengthMm(BigDecimal v)    { this.cargoLengthMm = v; }
    public BigDecimal getCargoWidthMm()     { return cargoWidthMm; }
    public void setCargoWidthMm(BigDecimal v)     { this.cargoWidthMm = v; }
    public BigDecimal getCargoHeightMm()    { return cargoHeightMm; }
    public void setCargoHeightMm(BigDecimal v)    { this.cargoHeightMm = v; }
    public boolean isHasPalletLifter() { return hasPalletLifter; }
    public void setHasPalletLifter(boolean v)  { this.hasPalletLifter = v; }
    public boolean isHasSackTruck()    { return hasSackTruck; }
    public void setHasSackTruck(boolean v)     { this.hasSackTruck = v; }
    public String getDescription()     { return description; }
    public void setDescription(String v)       { this.description = v; }
    public long getVersion()           { return version; }
    public void setVersion(long v)             { this.version = v; }
}
