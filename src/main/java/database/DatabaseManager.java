package database;

import java.sql.*;
import java.util.Properties;

/**
 * ============================================================
 *  CLASSE : DatabaseManager  (Gestionnaire de connexion JDBC)
 * ============================================================
 *  Responsabilité unique : gérer la connexion à la base MySQL.
 *
 *  ╔══════════════════════════════════════════════════════╗
 *  ║  RAPPEL COURS JDBC                                   ║
 *  ║  ─────────────────────────────────────────────────   ║
 *  ║  1. Charger le pilote  → Class.forName(...)          ║
 *  ║  2. Ouvrir connexion   → DriverManager.getConnection ║
 *  ║  3. Créer Statement    → con.createStatement()       ║
 *  ║  4. Exécuter requête   → stmt.executeQuery(sql)      ║
 *  ║  5. Traiter ResultSet  → rs.next(), rs.getString()   ║
 *  ║  6. Fermer ressources  → rs/stmt/con.close()         ║
 *  ╚══════════════════════════════════════════════════════╝
 *
 *  Patron Singleton : une seule instance de connexion
 *  partagée dans toute l'application.
 * ============================================================
 */
public class DatabaseManager {

    // ── Paramètres de connexion ────────────────────────────────────────────
    // ⚠️  MODIFIEZ ces valeurs selon votre configuration MySQL locale
    private static final String HOST     = "localhost";
    private static final String PORT     = "3306";
    private static final String DATABASE = "agenda_personnel";
    private static final String USER     = "root";
    private static final String PASSWORD = "";  // Mettez votre mot de passe MySQL ici

    /**
     * URL de connexion JDBC (format standard MySQL) :
     * jdbc:mysql://<hôte>:<port>/<base>?<paramètres>
     *
     * useSSL=false         → désactive SSL pour le développement local
     * serverTimezone=UTC   → évite les erreurs de fuseau horaire
     * allowPublicKeyRetrieval=true → nécessaire avec MySQL 8+
     */
    private static final String URL = String.format(
        "jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8",
        HOST, PORT, DATABASE
    );

    /** Nom de classe du pilote JDBC MySQL (mysql-connector-java) */
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    // ── Patron Singleton ───────────────────────────────────────────────────
    private static DatabaseManager instance;   // L'unique instance
    private Connection connection;             // L'unique connexion JDBC

    /**
     * Constructeur privé : empêche l'instanciation directe (Singleton).
     * Charge le pilote JDBC et ouvre la connexion.
     *
     * @throws SQLException si la connexion échoue
     */
    private DatabaseManager() throws SQLException {
        try {
            /*
             * ── ÉTAPE 1 : Chargement du pilote JDBC ──────────────────
             * Class.forName() charge dynamiquement le pilote MySQL.
             * Avec MySQL Connector/J 8+, cette ligne est optionnelle
             * (chargement automatique via ServiceLoader), mais elle
             * est conservée ici à des fins pédagogiques.
             */
            Class.forName(DRIVER);
            System.out.println("✅ Pilote JDBC MySQL chargé : " + DRIVER);

            /*
             * ── ÉTAPE 2 : Création de la connexion ───────────────────
             * DriverManager.getConnection() établit la connexion TCP
             * avec le serveur MySQL. Elle prend en paramètre :
             *  - l'URL JDBC (hôte, port, base)
             *  - le nom d'utilisateur
             *  - le mot de passe
             */
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connexion à la base de données établie !");

        } catch (ClassNotFoundException e) {
            throw new SQLException(
                "❌ Pilote JDBC MySQL introuvable. Ajoutez mysql-connector-j-8.x.jar au classpath.\n" + e.getMessage()
            );
        }
    }

    /**
     * Récupère l'unique instance du DatabaseManager (Singleton thread-safe).
     * Crée la connexion si elle n'existe pas encore ou si elle est fermée.
     *
     * @return l'instance unique de DatabaseManager
     * @throws SQLException si la connexion échoue
     */
    public static synchronized DatabaseManager getInstance() throws SQLException {
        if (instance == null || instance.connection == null || instance.connection.isClosed()) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Retourne l'objet Connection JDBC.
     * Utilisé par EventDAO pour créer des Statement et PreparedStatement.
     *
     * @return la connexion active à MySQL
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Ferme proprement la connexion JDBC.
     * À appeler lors de la fermeture de l'application.
     */
    public void fermerConnexion() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("🔒 Connexion à la base de données fermée.");
            }
        } catch (SQLException e) {
            System.err.println("⚠️  Erreur lors de la fermeture de la connexion : " + e.getMessage());
        }
    }

    /**
     * Teste si la connexion est toujours active (ping MySQL).
     *
     * @return true si la connexion est valide, false sinon
     */
    public boolean testerConnexion() {
        try {
            return connection != null && connection.isValid(3); // timeout 3 secondes
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Affiche les métadonnées de la connexion (à des fins de débogage/pédagogie).
     */
    public void afficherInfosConnexion() {
        try {
            DatabaseMetaData meta = connection.getMetaData();
            System.out.println("─── Infos connexion JDBC ───────────────────────");
            System.out.println("  Produit  : " + meta.getDatabaseProductName());
            System.out.println("  Version  : " + meta.getDatabaseProductVersion());
            System.out.println("  Driver   : " + meta.getDriverName() + " " + meta.getDriverVersion());
            System.out.println("  URL      : " + meta.getURL());
            System.out.println("────────────────────────────────────────────────");
        } catch (SQLException e) {
            System.err.println("Impossible de récupérer les métadonnées : " + e.getMessage());
        }
    }
}
