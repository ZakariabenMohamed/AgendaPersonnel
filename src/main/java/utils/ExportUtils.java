package utils;

import model.Event;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ============================================================
 *  CLASSE : ExportUtils  (Utilitaires d'export)
 * ============================================================
 *  Fournit des méthodes pour exporter les événements
 *  vers des fichiers CSV ou générer des sauvegardes.
 * ============================================================
 */
public class ExportUtils {

    private static final DateTimeFormatter FMT_FICHIER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Exporte une liste d'événements vers un fichier CSV.
     *
     * @param events    La liste des événements à exporter
     * @param cheminFichier Le chemin complet du fichier de destination
     * @throws IOException en cas d'erreur d'écriture
     */
    public static void exporterCSV(List<Event> events, String cheminFichier) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(cheminFichier), StandardCharsets.UTF_8))) {

            // En-tête BOM UTF-8 (pour Excel)
            writer.write('\uFEFF');

            // Ligne d'en-tête CSV
            writer.write("ID;Titre;Date;Heure;Description;Catégorie;Priorité;Rappel;Lieu");
            writer.newLine();

            // Une ligne par événement
            for (Event e : events) {
                writer.write(String.format("%d;%s;%s;%s;%s;%s;%s;%s;%s",
                    e.getId(),
                    echapperCSV(e.getTitre()),
                    e.getDateFormatee(),
                    e.getHeureFormatee(),
                    echapperCSV(e.getDescription() != null ? e.getDescription() : ""),
                    e.getCategorie(),
                    e.getPriorite(),
                    e.isRappel() ? "Oui" : "Non",
                    echapperCSV(e.getLieu() != null ? e.getLieu() : "")
                ));
                writer.newLine();
            }
        }
    }

    /**
     * Exporte une liste d'événements vers un fichier HTML (rendu lisible).
     *
     * @param events        Liste des événements
     * @param cheminFichier Chemin du fichier HTML de destination
     * @throws IOException en cas d'erreur d'écriture
     */
    public static void exporterHTML(List<Event> events, String cheminFichier) throws IOException {
        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(cheminFichier), StandardCharsets.UTF_8))) {

            w.write("<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'>");
            w.write("<title>Agenda Personnel - Export</title>");
            w.write("<style>body{font-family:Arial,sans-serif;margin:30px;}");
            w.write("h1{color:#2c3e50;}");
            w.write("table{border-collapse:collapse;width:100%;}");
            w.write("th{background:#3498db;color:white;padding:10px;}");
            w.write("td{padding:8px;border-bottom:1px solid #ddd;}");
            w.write("tr:hover{background:#f5f5f5;}");
            w.write(".urgent{color:#e74c3c;font-weight:bold;}");
            w.write(".important{color:#f39c12;font-weight:bold;}");
            w.write("</style></head><body>");
            w.write("<h1>📅 Agenda Personnel</h1>");
            w.write("<p>Exporté le " + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")) + "</p>");
            w.write("<table><tr><th>Titre</th><th>Date</th><th>Heure</th>");
            w.write("<th>Catégorie</th><th>Priorité</th><th>Lieu</th><th>Description</th></tr>");

            for (Event e : events) {
                String css = e.isUrgent() ? "urgent" : (e.isImportant() ? "important" : "");
                w.write(String.format(
                    "<tr><td class='%s'>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>",
                    css,
                    echapperHTML(e.getTitre()),
                    e.getDateFormatee(),
                    e.getHeureFormatee(),
                    e.getCategorie(),
                    e.getPriorite(),
                    echapperHTML(e.getLieu() != null ? e.getLieu() : ""),
                    echapperHTML(e.getDescription() != null ? e.getDescription() : "")
                ));
            }
            w.write("</table></body></html>");
        }
    }

    /**
     * Génère un nom de fichier horodaté pour l'export.
     *
     * @param prefixe  Préfixe du nom (ex: "export_agenda")
     * @param extension Extension (ex: "csv", "html")
     * @return Nom de fichier avec timestamp (ex: "export_agenda_20260430_143000.csv")
     */
    public static String genererNomFichier(String prefixe, String extension) {
        return prefixe + "_" + LocalDateTime.now().format(FMT_FICHIER) + "." + extension;
    }

    /** Échappe les caractères spéciaux pour le format CSV */
    private static String echapperCSV(String valeur) {
        if (valeur == null) return "";
        if (valeur.contains(";") || valeur.contains("\"") || valeur.contains("\n")) {
            return "\"" + valeur.replace("\"", "\"\"") + "\"";
        }
        return valeur;
    }

    /** Échappe les caractères HTML spéciaux */
    private static String echapperHTML(String valeur) {
        if (valeur == null) return "";
        return valeur
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
