package dk.rfg.fleetmanager.entity;

import jakarta.persistence.*;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

@Entity
@Table(name = "tasks")
@IdClass(TaskId.class)
public class Task {

    @Id
    @Column(name = "festival_year", columnDefinition = "SMALLINT")
    private int festivalYear;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tasks_task_id_seq_gen")
    @SequenceGenerator(name = "tasks_task_id_seq_gen", sequenceName = "tasks_task_id_seq", allocationSize = 1)
    @Column(name = "task_id")
    private int taskId;
    @Column(name = "start_ts", nullable = false)
    private OffsetDateTime startTs;
    @Column(name = "end_ts")
    private OffsetDateTime endTs;
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "start_point")
    @NotBlank(message = "Fra er obligatorisk")
    private String startPoint;
    @Column(name = "end_point")
    @NotBlank(message = "Til er obligatorisk")
    private String endPoint;
    @Column(name = "via_point")
    private String viaPoint;
    @Column(name = "division")
    @NotBlank(message = "Division er obligatorisk")
    private String division;
    @Column(name = "team")
    @NotBlank(message = "Team er obligatorisk")
    private String team;
    @Column(name = "affiliation")
    private String affiliation;
    @Column(name = "booker_name")
    @NotBlank(message = "Bestillerens navn er obligatorisk")
    private String bookerName;
    @Column(name = "booker_phone")
    @NotBlank(message = "Telefon er obligatorisk")
    private String bookerPhone;
    @Column(name = "contact_name")
    private String contactName;
    @Column(name = "contact_phone")
    private String contactPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "festival_year", referencedColumnName = "festival_year", insertable = false, updatable = false),
        @JoinColumn(name = "vehicle_id", referencedColumnName = "vehicle_id", insertable = false, updatable = false)
    })
    private Vehicle vehicle;

    @Column(name = "vehicle_id")
    private Integer vehicleId;
    @Column(name = "driver_name")
    private String driverName;
    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", nullable = false)
    private TaskStatus status = TaskStatus.ORDERED;
    @Column(name = "received_by")
    private String receivedBy;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @PrePersist
    void onCreate() {
        if (createdAt == null) { createdAt = OffsetDateTime.now(); }
        refreshAffiliation();
    }

    @PreUpdate
    void onUpdate() {
        refreshAffiliation();
    }

    private void refreshAffiliation() {
        if (division != null && team != null) { affiliation = division + " / " + team; } else if (division != null) {
            affiliation = division;
        } else { affiliation = team; }
    }

    public Task() {
    }

    public String directionsUrl() {
        if (startPoint == null || endPoint == null) { return null; }
        return "https://maps.google.dk/maps?daddr=" + startPoint.replace(" ", "+") + "+to:" + endPoint.replace(" ", "+");
    }

    public int getFestivalYear() {
        return festivalYear;
    }

    public void setFestivalYear(int v) {
        this.festivalYear = v;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int v) {
        this.taskId = v;
    }

    public OffsetDateTime getStartTs() {
        return startTs;
    }

    public void setStartTs(OffsetDateTime v) {
        this.startTs = v;
    }

    public OffsetDateTime getEndTs() {
        return endTs;
    }

    public void setEndTs(OffsetDateTime v) {
        this.endTs = v;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime v) {
        this.createdAt = v;
    }

    public String getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(String v) {
        this.startPoint = v;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String v) {
        this.endPoint = v;
    }

    public String getViaPoint() {
        return viaPoint;
    }

    public void setViaPoint(String v) {
        this.viaPoint = v;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String v) {
        this.division = v;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String v) {
        this.team = v;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String v) {
        this.affiliation = v;
    }

    public String getBookerName() {
        return bookerName;
    }

    public void setBookerName(String v) {
        this.bookerName = v;
    }

    public String getBookerPhone() {
        return bookerPhone;
    }

    public void setBookerPhone(String v) {
        this.bookerPhone = v;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String v) {
        this.contactName = v;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String v) {
        this.contactPhone = v;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle v) {
        this.vehicle = v;
    }

    public Integer getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Integer v) {
        this.vehicleId = v;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String v) {
        this.driverName = v;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String v) {
        this.description = v;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus v) {
        this.status = v;
    }

    public String getReceivedBy() {
        return receivedBy;
    }

    public void setReceivedBy(String v) {
        this.receivedBy = v;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long v) {
        this.version = v;
    }
}
