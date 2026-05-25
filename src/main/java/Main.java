import dao.EventDAO;
import database.DatabaseManager;
import view.MainView;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

/**
 * ============================================================
 *  CLASSE : Main  (Point d'entrée de l'application)
 * ============================================================
 *  Séquence de démarrage :
 *   1. Configuration du Look & Feel Swing
 *   2. Affichage d'un splash screen (optionnel)
 *   3. Connexion à la base de données MySQL (JDBC)
 *   4. Création des couches DAO
 *   5. Lancement de la fenêtre principale
 *   6. Fermeture propre des ressources à la fin
 *
 *  Tous les composants UI sont créés sur l'EDT
 *  (Event Dispatch Thread) via SwingUtilities.invokeLater()
 *  — bonne pratique impérative en Swing.
 * ============================================================
 */
public class Main {

    public static void main(String[] args) {
        // ── 1. Configuration du Look & Feel ─────────────────────────────
        configurerLookAndFeel();

        // ── 2. Lancement sur l'EDT Swing ────────────────────────────────
        SwingUtilities.invokeLater(() -> demarrerApplication());
    }

    /**
     * Démarre l'application :
     *  - Connexion JDBC
     *  - Création des DAO
     *  - Affichage de la fenêtre principale
     */
    private static void demarrerApplication() {
        // Fenêtre de chargement
        JWindow splash = afficherSplash();

        try {
            // ── 3. Connexion à la base de données ─────────────────────────
            System.out.println("═══════════════════════════════════════════");
            System.out.println("   AGENDA PERSONNEL  —  Démarrage JDBC");
            System.out.println("═══════════════════════════════════════════");

            DatabaseManager dbManager = DatabaseManager.getInstance();
            dbManager.afficherInfosConnexion();

            // ── 4. Création du DAO ────────────────────────────────────────
            EventDAO eventDAO = new EventDAO();
            System.out.println("✅ EventDAO initialisé — " +
                               eventDAO.compterTotal() + " événement(s) en base.");

            // ── 5. Fermeture du splash et lancement de la vue ─────────────
            if (splash != null) splash.dispose();

            MainView mainView = new MainView(eventDAO);
            mainView.setVisible(true);

            // ── 6. Fermeture propre de la connexion à la sortie ───────────
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("🔒 Fermeture de la connexion JDBC...");
                try {
                    DatabaseManager.getInstance().fermerConnexion();
                } catch (SQLException e) {
                    System.err.println("Erreur à la fermeture : " + e.getMessage());
                }
            }));

        } catch (SQLException ex) {
            if (splash != null) splash.dispose();

            // Message d'erreur détaillé pour aider au diagnostic
            String erreurMsg =
                "❌ Impossible de se connecter à la base de données MySQL.\n\n" +
                "Causes possibles :\n" +
                "  1. MySQL n'est pas démarré → lancez MySQL Server\n" +
                "  2. Mauvais utilisateur/mot de passe → vérifiez DatabaseManager.java\n" +
                "  3. Base 'agenda_personnel' inexistante → exécutez database.sql\n" +
                "  4. Pilote JDBC absent → ajoutez mysql-connector-j-8.x.jar\n\n" +
                "Détail technique : " + ex.getMessage();

            JOptionPane.showMessageDialog(null, erreurMsg,
                "Erreur de connexion", JOptionPane.ERROR_MESSAGE);

            System.err.println(erreurMsg);
            System.exit(1);
        }
    }

    /**
     * Configure le Look & Feel Swing.
     * Essaie d'abord Nimbus (moderne), puis le L&F système, enfin le défaut.
     */
    private static void configurerLookAndFeel() {
        try {
            // Essai Nimbus (L&F moderne inclus dans le JDK depuis Java 6)
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());

                    // Personnalisation des couleurs Nimbus
                    UIManager.put("nimbusBase",        new Color(41, 128, 185));
                    UIManager.put("nimbusBlueGrey",    new Color(60, 80, 100));
                    UIManager.put("control",           new Color(245, 246, 250));
                    UIManager.put("text",              new Color(30, 30, 30));
                    UIManager.put("Table.background",  Color.WHITE);
                    UIManager.put("Table[Enabled+Selected].textForeground", new Color(30,30,30));

                    System.out.println("✅ Look & Feel : Nimbus");
                    return;
                }
            }
            // Fallback : L&F du système d'exploitation
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            System.out.println("✅ Look & Feel : Système");

        } catch (Exception e) {
            System.out.println("⚠️  Look & Feel par défaut (Metal)");
        }
    }

    /**
     * Affiche une fenêtre de chargement pendant la connexion MySQL.
     * Améliore l'expérience utilisateur lors du démarrage.
     *
     * @return La JWindow du splash screen (à disposer après le chargement)
     */
    private static JWindow afficherSplash() {
        try {
            JWindow splash = new JWindow();
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(new Color(41, 128, 185));
            panel.setBorder(BorderFactory.createLineBorder(new Color(31, 97, 141), 3));

            JLabel titre = new JLabel("📅 Agenda Personnel", SwingConstants.CENTER);
            titre.setFont(new Font("Segoe UI", Font.BOLD, 24));
            titre.setForeground(Color.WHITE);
            titre.setBorder(BorderFactory.createEmptyBorder(30, 40, 10, 40));

            JLabel chargement = new JLabel("Connexion à la base de données...", SwingConstants.CENTER);
            chargement.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            chargement.setForeground(new Color(189, 218, 245));
            chargement.setBorder(BorderFactory.createEmptyBorder(0, 40, 30, 40));

            panel.add(titre, BorderLayout.CENTER);
            panel.add(chargement, BorderLayout.SOUTH);

            splash.setContentPane(panel);
            splash.setSize(400, 150);
            splash.setLocationRelativeTo(null);
            splash.setVisible(true);

            return splash;
        } catch (Exception e) {
            return null;
        }
    }
}
