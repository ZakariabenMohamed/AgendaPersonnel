package model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * ============================================================
 *  CLASSE MODÈLE : Event (Événement de l'agenda)
 * ============================================================
 *  Représente un événement dans l'agenda personnel.
 *  Suit le patron de conception JavaBean :
 *   - attributs privés
 *   - constructeurs public
 *   - getters / setters
 *  
 *  Lien JDBC : Cette classe correspond directement à une ligne
 *  de la table SQL "evenements". Le mapping
 *  ResultSet → Event est réalisé dans la classe EventDAO.
 * ============================================================
 */
public class Event {

    // ── Constantes : catégories et priorités disponibles ──────────────────
    public static final String[] CATEGORIES = {"Personnel", "Professionnel", "Études", "Autre"};
    public static final String[] PRIORITES  = {"Normale", "Importante", "Urgente"};

    // Formateurs de date/heure pour l'affichage dans l'interface
    public static final DateTimeFormatter FMT_DATE  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter FMT_HEURE = DateTimeFormatter.ofPattern("HH:mm");

    // ── Attributs privés (encapsulation) ──────────────────────────────────
    private int       id;          // Clé primaire de la table SQL
    private String    titre;       // Titre de l'événement (obligatoire)
    private LocalDate date;        // Date (java.time.LocalDate)
    private LocalTime heure;       // Heure (java.time.LocalTime)
    private String    description; // Description détaillée (optionnelle)
    private String    categorie;   // "Personnel" | "Professionnel" | "Études" | "Autre"
    private String    priorite;    // "Normale" | "Importante" | "Urgente"
    private boolean   rappel;      // Activer le rappel au démarrage
    private String    lieu;        // Lieu de l'événement (optionnel)

    // ── CONSTRUCTEUR COMPLET ───────────────────────────────────────────────
    /**
     * Constructeur utilisé principalement lors de la lecture depuis la BDD
     * (ResultSet → Event dans EventDAO).
     */
    public Event(int id, String titre, LocalDate date, LocalTime heure,
                 String description, String categorie, String priorite,
                 boolean rappel, String lieu) {
        this.id          = id;
        this.titre       = titre;
        this.date        = date;
        this.heure       = heure;
        this.description = description;
        this.categorie   = categorie;
        this.priorite    = priorite;
        this.rappel      = rappel;
        this.lieu        = lieu;
    }

    // ── CONSTRUCTEUR SANS ID (pour la création d'un nouvel événement) ──────
    /**
     * Constructeur utilisé lors de l'insertion (l'id est auto-incrémenté par MySQL).
     */
    public Event(String titre, LocalDate date, LocalTime heure,
                 String description, String categorie, String priorite,
                 boolean rappel, String lieu) {
        this(0, titre, date, heure, description, categorie, priorite, rappel, lieu);
    }

    // ── CONSTRUCTEUR PAR DÉFAUT ────────────────────────────────────────────
    public Event() {
        this.date      = LocalDate.now();
        this.heure     = LocalTime.now().withSecond(0).withNano(0);
        this.categorie = "Personnel";
        this.priorite  = "Normale";
        this.rappel    = false;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  GETTERS & SETTERS
    // ═══════════════════════════════════════════════════════════════════════

    public int getId()                  { return id; }
    public void setId(int id)           { this.id = id; }

    public String getTitre()            { return titre; }
    public void setTitre(String titre)  { this.titre = titre; }

    public LocalDate getDate()          { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getHeure()           { return heure; }
    public void setHeure(LocalTime heure) { this.heure = heure; }

    public String getDescription()              { return description; }
    public void setDescription(String desc)     { this.description = desc; }

    public String getCategorie()                { return categorie; }
    public void setCategorie(String categorie)  { this.categorie = categorie; }

    public String getPriorite()                 { return priorite; }
    public void setPriorite(String priorite)    { this.priorite = priorite; }

    public boolean isRappel()              { return rappel; }
    public void setRappel(boolean rappel)  { this.rappel = rappel; }

    public String getLieu()              { return lieu; }
    public void setLieu(String lieu)     { this.lieu = lieu; }

    // ── MÉTHODES UTILITAIRES ───────────────────────────────────────────────

    /** Retourne la date formatée pour l'affichage (dd/MM/yyyy) */
    public String getDateFormatee() {
        return date != null ? date.format(FMT_DATE) : "";
    }

    /** Retourne l'heure formatée pour l'affichage (HH:mm) */
    public String getHeureFormatee() {
        return heure != null ? heure.format(FMT_HEURE) : "";
    }

    /** Vérifie si l'événement est aujourd'hui */
    public boolean isAujourdhui() {
        return date != null && date.equals(LocalDate.now());
    }

    /** Vérifie si l'événement est passé */
    public boolean isPasse() {
        if (date == null) return false;
        if (date.isBefore(LocalDate.now())) return true;
        if (date.equals(LocalDate.now()) && heure != null)
            return heure.isBefore(LocalTime.now());
        return false;
    }

    /** Vérifie si l'événement est urgent (priorité "Urgente") */
    public boolean isUrgent() {
        return "Urgente".equals(priorite);
    }

    /** Vérifie si l'événement est important ou urgent */
    public boolean isImportant() {
        return "Importante".equals(priorite) || "Urgente".equals(priorite);
    }

    @Override
    public String toString() {
        return String.format("Event{id=%d, titre='%s', date=%s, heure=%s, cat=%s, prio=%s}",
                id, titre, getDateFormatee(), getHeureFormatee(), categorie, priorite);
    }
}
