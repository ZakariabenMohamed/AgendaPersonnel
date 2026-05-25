package dao;

import database.DatabaseManager;
import model.Event;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================================
 *  CLASSE : EventDAO  (Data Access Object)
 * ============================================================
 *  Sépare la logique d'accès aux données du reste de l'application.
 *  Toutes les opérations SQL (CRUD + recherche) passent ici.
 *
 *  ╔═══════════════════════════════════════════════════════╗
 *  ║  RAPPEL COURS JDBC                                    ║
 *  ║  ─────────────────────────────────────────────────    ║
 *  ║  PreparedStatement vs Statement :                     ║
 *  ║   • Statement       → requêtes statiques (sans param) ║
 *  ║   • PreparedStatement → requêtes paramétrées          ║
 *  ║     - Évite les injections SQL (sécurité ++)          ║
 *  ║     - Meilleure performance (requête pré-compilée)    ║
 *  ║     - Paramètres via pstmt.setXxx(index, valeur)      ║
 *  ║                                                       ║
 *  ║  ResultSet TYPE_SCROLL_INSENSITIVE :                  ║
 *  ║   • Permet de se déplacer librement dans le curseur   ║
 *  ║     (first(), last(), absolute(n), relative(n))       ║
 *  ║   • CONCUR_READ_ONLY → lecture seule (par défaut)     ║
 *  ║   • CONCUR_UPDATABLE → modification via le ResultSet  ║
 *  ╚═══════════════════════════════════════════════════════╝
 * ============================================================
 */
public class EventDAO {

    /** Noms des colonnes SQL – centralisés pour éviter les fautes de frappe */
    private static final String TABLE = "evenements";
    private static final String COL_ID          = "id";
    private static final String COL_TITRE       = "titre";
    private static final String COL_DATE        = "date_event";
    private static final String COL_HEURE       = "heure_event";
    private static final String COL_DESC        = "description";
    private static final String COL_CAT         = "categorie";
    private static final String COL_PRIO        = "priorite";
    private static final String COL_RAPPEL      = "rappel";
    private static final String COL_LIEU        = "lieu";

    // ── Référence vers le gestionnaire de connexion ────────────────────────
    private final DatabaseManager dbManager;

    public EventDAO() throws SQLException {
        this.dbManager = DatabaseManager.getInstance();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  C R U D  —  CREATE
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Insère un nouvel événement dans la base de données.
     *
     * Utilise un PreparedStatement avec Statement.RETURN_GENERATED_KEYS
     * pour récupérer l'id auto-incrémenté attribué par MySQL.
     *
     * @param event L'événement à insérer (id ignoré, généré par MySQL)
     * @return L'id généré par MySQL, ou -1 en cas d'échec
     * @throws SQLException en cas d'erreur SQL
     */
    public int inserer(Event event) throws SQLException {
        // PreparedStatement avec marqueurs "?" pour chaque valeur
        String sql = "INSERT INTO " + TABLE +
                     " (titre, date_event, heure_event, description, categorie, priorite, rappel, lieu)" +
                     " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        /*
         * Statement.RETURN_GENERATED_KEYS : demande à JDBC de retourner
         * les clés auto-générées (ici l'id AUTO_INCREMENT de MySQL)
         */
        try (PreparedStatement pstmt = dbManager.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindEventToStatement(pstmt, event); // Remplace les "?" par les valeurs

            int lignesAffectees = pstmt.executeUpdate();

            if (lignesAffectees > 0) {
                // Récupération de l'id auto-incrémenté
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int newId = generatedKeys.getInt(1);
                        event.setId(newId);
                        return newId;
                    }
                }
            }
        }
        return -1;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  C R U D  —  READ (Lecture)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Récupère TOUS les événements, triés par date puis heure.
     *
     * Utilise un ResultSet TYPE_SCROLL_INSENSITIVE pour permettre
     * de naviguer librement dans le résultat (pédagogique).
     *
     * @return Liste de tous les événements
     * @throws SQLException en cas d'erreur SQL
     */
    public List<Event> lireTous() throws SQLException {
        String sql = "SELECT * FROM " + TABLE + " ORDER BY date_event ASC, heure_event ASC";

        /*
         * TYPE_SCROLL_INSENSITIVE : le curseur peut avancer ET reculer,
         * mais ne reflète pas les modifications faites après l'ouverture.
         * CONCUR_READ_ONLY : lecture seule (suffisant ici).
         */
        try (Statement stmt = dbManager.getConnection().createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = stmt.executeQuery(sql)) {

            List<Event> events = new ArrayList<>();

            // rs.next() déplace le curseur vers la ligne suivante
            while (rs.next()) {
                events.add(rsToEvent(rs)); // mapping ResultSet → Event
            }
            return events;
        }
    }

    /**
     * Récupère un événement par son id.
     *
     * @param id L'identifiant de l'événement
     * @return L'événement trouvé, ou null si inexistant
     * @throws SQLException en cas d'erreur SQL
     */
    public Event lireParId(int id) throws SQLException {
        String sql = "SELECT * FROM " + TABLE + " WHERE id = ?";

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rsToEvent(rs);
            }
        }
        return null;
    }

    /**
     * Récupère les événements d'aujourd'hui (pour les notifications).
     *
     * @return Liste des événements du jour
     * @throws SQLException en cas d'erreur SQL
     */
    public List<Event> lireAujourdhui() throws SQLException {
        String sql = "SELECT * FROM " + TABLE +
                     " WHERE date_event = CURDATE() ORDER BY heure_event ASC";

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            List<Event> events = new ArrayList<>();
            while (rs.next()) events.add(rsToEvent(rs));
            return events;
        }
    }

    /**
     * Récupère les événements importants ou urgents (pour le panneau prioritaire).
     *
     * @return Liste des événements importants/urgents à venir
     * @throws SQLException en cas d'erreur SQL
     */
    public List<Event> lireImportants() throws SQLException {
        String sql = "SELECT * FROM " + TABLE +
                     " WHERE priorite IN ('Importante','Urgente') AND date_event >= CURDATE()" +
                     " ORDER BY priorite DESC, date_event ASC, heure_event ASC";

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            List<Event> events = new ArrayList<>();
            while (rs.next()) events.add(rsToEvent(rs));
            return events;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  C R U D  —  UPDATE (Modification)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Met à jour un événement existant dans la base de données.
     *
     * @param event L'événement avec les nouvelles valeurs (l'id identifie la ligne)
     * @return true si la mise à jour a réussi
     * @throws SQLException en cas d'erreur SQL
     */
    public boolean modifier(Event event) throws SQLException {
        String sql = "UPDATE " + TABLE +
                     " SET titre=?, date_event=?, heure_event=?, description=?," +
                     "     categorie=?, priorite=?, rappel=?, lieu=?" +
                     " WHERE id=?";

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            bindEventToStatement(pstmt, event); // Paramètres 1 à 8
            pstmt.setInt(9, event.getId());     // Paramètre 9 : l'id de la clause WHERE

            return pstmt.executeUpdate() > 0;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  C R U D  —  DELETE (Suppression)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Supprime un événement par son id.
     *
     * @param id L'identifiant de l'événement à supprimer
     * @return true si la suppression a réussi
     * @throws SQLException en cas d'erreur SQL
     */
    public boolean supprimer(int id) throws SQLException {
        String sql = "DELETE FROM " + TABLE + " WHERE id = ?";

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Supprime tous les événements passés (ménage automatique).
     *
     * @return Nombre de lignes supprimées
     * @throws SQLException en cas d'erreur SQL
     */
    public int supprimerEvenementsPasses() throws SQLException {
        String sql = "DELETE FROM " + TABLE + " WHERE date_event < CURDATE()";

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            return pstmt.executeUpdate();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  RECHERCHE MULTICRITÈRES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Recherche multicritères dynamique.
     * Les critères null ou vides sont ignorés (clause WHERE optionnelle).
     *
     * @param titre     Sous-chaîne du titre (recherche LIKE) - null = ignoré
     * @param date      Date exacte - null = ignoré
     * @param categorie Catégorie exacte - null ou "" = ignoré
     * @param priorite  Priorité exacte - null ou "" = ignoré
     * @return Liste des événements correspondants
     * @throws SQLException en cas d'erreur SQL
     */
    public List<Event> rechercher(String titre, LocalDate date,
                                   String categorie, String priorite) throws SQLException {

        // Construction dynamique de la requête SQL
        StringBuilder sql = new StringBuilder("SELECT * FROM " + TABLE + " WHERE 1=1");
        List<Object> params = new ArrayList<>();

        // Ajout conditionnel de chaque critère
        if (titre != null && !titre.trim().isEmpty()) {
            sql.append(" AND titre LIKE ?");
            params.add("%" + titre.trim() + "%"); // LIKE %titre% = contient
        }
        if (date != null) {
            sql.append(" AND date_event = ?");
            params.add(Date.valueOf(date));
        }
        if (categorie != null && !categorie.isEmpty() && !categorie.equals("Toutes")) {
            sql.append(" AND categorie = ?");
            params.add(categorie);
        }
        if (priorite != null && !priorite.isEmpty() && !priorite.equals("Toutes")) {
            sql.append(" AND priorite = ?");
            params.add(priorite);
        }

        sql.append(" ORDER BY date_event ASC, heure_event ASC");

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql.toString())) {
            // Bind des paramètres dynamiques
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof String)     pstmt.setString(i + 1, (String) param);
                else if (param instanceof Date)  pstmt.setDate(i + 1, (Date) param);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                List<Event> events = new ArrayList<>();
                while (rs.next()) events.add(rsToEvent(rs));
                return events;
            }
        }
    }

    /**
     * Récupère les événements d'un mois donné (pour la vue calendrier).
     *
     * @param annee  L'année (ex: 2026)
     * @param mois   Le mois (1 = janvier, 12 = décembre)
     * @return Liste des événements du mois
     * @throws SQLException en cas d'erreur SQL
     */
    public List<Event> lireParMois(int annee, int mois) throws SQLException {
        String sql = "SELECT * FROM " + TABLE +
                     " WHERE YEAR(date_event) = ? AND MONTH(date_event) = ?" +
                     " ORDER BY date_event ASC, heure_event ASC";

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, annee);
            pstmt.setInt(2, mois);

            try (ResultSet rs = pstmt.executeQuery()) {
                List<Event> events = new ArrayList<>();
                while (rs.next()) events.add(rsToEvent(rs));
                return events;
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STATISTIQUES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Compte le nombre d'événements par catégorie.
     * Utilisé pour les graphiques statistiques.
     *
     * @return Map<catégorie, count>
     * @throws SQLException en cas d'erreur SQL
     */
    public Map<String, Integer> compterParCategorie() throws SQLException {
        String sql = "SELECT categorie, COUNT(*) as nb FROM " + TABLE + " GROUP BY categorie";

        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            Map<String, Integer> stats = new HashMap<>();
            while (rs.next()) {
                stats.put(rs.getString("categorie"), rs.getInt("nb"));
            }
            return stats;
        }
    }

    /**
     * Compte le nombre d'événements par priorité.
     *
     * @return Map<priorité, count>
     * @throws SQLException en cas d'erreur SQL
     */
    public Map<String, Integer> compterParPriorite() throws SQLException {
        String sql = "SELECT priorite, COUNT(*) as nb FROM " + TABLE + " GROUP BY priorite";

        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            Map<String, Integer> stats = new HashMap<>();
            while (rs.next()) {
                stats.put(rs.getString("priorite"), rs.getInt("nb"));
            }
            return stats;
        }
    }

    /**
     * Compte le nombre total d'événements.
     *
     * @return Le nombre total d'événements dans la base
     * @throws SQLException en cas d'erreur SQL
     */
    public int compterTotal() throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + TABLE;

        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  MÉTHODES PRIVÉES  —  Utilitaires internes
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Mappe une ligne du ResultSet vers un objet Event.
     * Conversion SQL → Java :
     *   DATE    → LocalDate  (via toLocalDate())
     *   TIME    → LocalTime  (via toLocalTime())
     *   BOOLEAN → boolean
     *
     * @param rs Le ResultSet positionné sur la ligne courante
     * @return Un objet Event rempli avec les valeurs de la ligne
     * @throws SQLException si une colonne est introuvable
     */
    private Event rsToEvent(ResultSet rs) throws SQLException {
        return new Event(
            rs.getInt(COL_ID),
            rs.getString(COL_TITRE),
            rs.getDate(COL_DATE).toLocalDate(),       // java.sql.Date → LocalDate
            rs.getTime(COL_HEURE).toLocalTime(),      // java.sql.Time → LocalTime
            rs.getString(COL_DESC),
            rs.getString(COL_CAT),
            rs.getString(COL_PRIO),
            rs.getBoolean(COL_RAPPEL),
            rs.getString(COL_LIEU)
        );
    }

    /**
     * Lie les champs d'un Event aux paramètres "?" d'un PreparedStatement.
     * Utilisé par inserer() et modifier() pour éviter la duplication de code.
     *
     * Conversion Java → SQL :
     *   LocalDate → java.sql.Date  (via Date.valueOf())
     *   LocalTime → java.sql.Time  (via Time.valueOf())
     *
     * @param pstmt Le PreparedStatement à remplir
     * @param event L'événement source des valeurs
     * @throws SQLException si un paramètre est invalide
     */
    private void bindEventToStatement(PreparedStatement pstmt, Event event) throws SQLException {
        pstmt.setString(1, event.getTitre());
        pstmt.setDate  (2, Date.valueOf(event.getDate()));     // LocalDate → java.sql.Date
        pstmt.setTime  (3, Time.valueOf(event.getHeure()));    // LocalTime → java.sql.Time
        pstmt.setString(4, event.getDescription());
        pstmt.setString(5, event.getCategorie());
        pstmt.setString(6, event.getPriorite());
        pstmt.setBoolean(7, event.isRappel());
        pstmt.setString(8, event.getLieu());
    }
}
