package dk.rfg.fleetmanager.util;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class DivisionTeamData {
    private static final LinkedHashMap<String, List<String>> DATA = new LinkedHashMap<>();
    static {
        DATA.put("Deltagere", List.of("Foreninger","RF Experience Safety","Ildsjælesupport","Organisation","Sikkerhed","Affald & Genbrug","Ankomst & Adgang","Beredskab","Camping","Diversitet & Lighed","Programsikkerhed","Publikumsservice","Toilet & Badservice"));
        DATA.put("Kommunikation & Partnerskaber", List.of("Check-In Samspillet","Divisionssekretariat","Information","Kampagner & Donastioner","Mediehus Roskilde","Partnerskaber","Presse","Samspillet"));
        DATA.put("Byplan & Logistik", List.of("Administration & People","Artist Village - Vaskeri","Byggesag","Camping","Construction","Det Cirkulære Laboratorium","Divisionsledelse","El","Enheder","Festival IT","Festival VVS","Frivillighed & Organisation","GIS & Geografi","Hegn","Håndværkerkontor","Indre Plads","Køreplader","Kørende Materiel","Logistikplan","Materiel","Omkringliggende områder","Plan & Proces","RF Experience Logistik","Skilte","Transport","Trælasthandel","Tømrer"));
        DATA.put("Handel & Gastronomi", List.of("Administration","Divisionsledelse","Drikkevarer","Forretningsudvikling","Frivillig Faciliteter","Gastronomi","Nonfood","Områder & Fysik","Organisation & Ledelse","Service"));
        DATA.put("Program", List.of("Afvikling","Arena","Art & Activism","Art Production","Artist Check-in","Artist Village","Avalon","Backline","Event","Gaia","Gloria","Hotel & Transfer","Music","Orange","Produktion","Stadion","Telt & Sejl","Vestscener"));
    }
    public List<String> getDivisions() { return new ArrayList<>(DATA.keySet()); }
    public List<String> getTeamsForDivision(String division) {
        if (division == null || division.isBlank()) return List.of();
        List<String> teams = DATA.get(division);
        if (teams == null) return List.of();
        List<String> result = new ArrayList<>(teams);
        result.add("Ved ikke");
        return result;
    }
    public Map<String, List<String>> getAllData() { return Collections.unmodifiableMap(DATA); }
}
