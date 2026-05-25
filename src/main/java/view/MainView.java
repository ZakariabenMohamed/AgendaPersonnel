package view;

import dao.EventDAO;
import model.Event;
import utils.ExportUtils;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================
 *  CLASSE : MainView  (Fenêtre principale de l'application)
 * ============================================================
 *  Architecture MVC implicite :
 *   - Modèle  : classes Event, EventDAO
 *   - Vue     : cette classe (MainView)
 *   - Contrôleur : logique intégrée dans les ActionListeners
 *
 *  Onglets de l'interface :
 *   1. Liste des événements (JTable + recherche)
 *   2. Vue Calendrier mensuelle
 *   3. Statistiques (graphiques)
 *   4. Événements importants
 * ============================================================
 */
public class MainView extends JFrame {

    // ── Couche DAO ────────────────────────────────────────────────────────
    private EventDAO eventDAO;

    // ── Modèle du tableau ─────────────────────────────────────────────────
    private EventTableModel tableModel;
    private JTable          tableEvenements;

    // ── Composants de recherche ───────────────────────────────────────────
    private JTextField    champRechercheTitre;
    private JTextField    champRechercheDate;
    private JComboBox<String> comboFiltreCategorie;
    private JComboBox<String> comboFiltrePriorite;

    // ── Panneaux spéciaux ─────────────────────────────────────────────────
    private CalendarPanel    calendarPanel;
    private StatisticsPanel  statsPanel;
    private JList<String>    listeImportants;
    private DefaultListModel<String> modelListeImportants;

    // ── Barre de statut ───────────────────────────────────────────────────
    private JLabel labelStatut;

    // ── Couleurs / Styles ─────────────────────────────────────────────────
    private static final Color BLEU_FONCE   = new Color(41, 128, 185);
    private static final Color VERT         = new Color(39, 174, 96);
    private static final Color ROUGE        = new Color(192, 57, 43);
    private static final Color ORANGE       = new Color(230, 126, 34);
    private static final Color GRIS_FOND    = new Color(245, 246, 250);
    private static final Color BLANC        = Color.WHITE;

    public MainView(EventDAO dao) {
        this.eventDAO = dao;
        initialiserFenetre();
        construireUI();
        chargerDonnees();
        verifierRappels();
    }

    /** Configure les propriétés de base de la fenêtre principale. */
    private void initialiserFenetre() {
        setTitle("📅 Agenda Personnel — Binôme Java POO");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1200, 750);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        // Confirmation avant fermeture
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (JOptionPane.showConfirmDialog(MainView.this,
                    "Voulez-vous quitter l'application ?",
                    "Confirmation", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }
        });
    }

    /** Construit tous les composants de l'interface. */
    private void construireUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(GRIS_FOND);

        // ── En-tête ────────────────────────────────────────────────────────
        add(construireEntete(), BorderLayout.NORTH);

        // ── Zone principale : onglets ──────────────────────────────────────
        JTabbedPane onglets = new JTabbedPane();
        onglets.setFont(new Font("Segoe UI", Font.BOLD, 13));
        onglets.setBackground(GRIS_FOND);

        onglets.addTab("📋  Événements",   construireOngletEvenements());
        onglets.addTab("📅  Calendrier",   construireOngletCalendrier());
        onglets.addTab("📊  Statistiques", construireOngletStatistiques());
        onglets.addTab("⭐  Importants",   construireOngletImportants());

        // Rechargement des données à chaque changement d'onglet
        onglets.addChangeListener(e -> {
            int idx = onglets.getSelectedIndex();
            if (idx == 1) chargerCalendrier();
            if (idx == 2) chargerStatistiques();
            if (idx == 3) chargerImportants();
        });

        add(onglets, BorderLayout.CENTER);

        // ── Barre de statut ────────────────────────────────────────────────
        add(construireBarreStatut(), BorderLayout.SOUTH);

        // ── Menu ──────────────────────────────────────────────────────────
        setJMenuBar(construireMenuBar());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CONSTRUCTION DES COMPOSANTS UI
    // ═══════════════════════════════════════════════════════════════════════

    private JPanel construireEntete() {
        JPanel entete = new JPanel(new BorderLayout());
        entete.setBackground(BLEU_FONCE);
        entete.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel titre = new JLabel("  📅  Agenda Personnel");
        titre.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titre.setForeground(BLANC);

        JLabel sousTitre = new JLabel(
            "Aujourd'hui : " + LocalDate.now().format(Event.FMT_DATE)
        );
        sousTitre.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sousTitre.setForeground(new Color(189, 218, 245));

        JPanel titrePanel = new JPanel(new BorderLayout());
        titrePanel.setOpaque(false);
        titrePanel.add(titre, BorderLayout.CENTER);
        titrePanel.add(sousTitre, BorderLayout.SOUTH);

        entete.add(titrePanel, BorderLayout.WEST);

        // Boutons d'action rapide dans l'en-tête
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.setOpaque(false);
        JButton btnAjouter = creerBoutonAction("➕ Ajouter", VERT);
        btnAjouter.addActionListener(e -> ouvrirFormulaireAjout());
        actionPanel.add(btnAjouter);
        entete.add(actionPanel, BorderLayout.EAST);

        return entete;
    }

    private JPanel construireOngletEvenements() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(BLANC);

        // ── Barre de recherche ────────────────────────────────────────────
        panel.add(construirePanneauRecherche(), BorderLayout.NORTH);

        // ── Tableau des événements ─────────────────────────────────────────
        tableModel = new EventTableModel();
        tableEvenements = new JTable(tableModel);
        styliserTableau(tableEvenements);

        // Double-clic → modifier
        tableEvenements.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) ouvrirFormulaireModification();
            }
        });

        JScrollPane scrollPane = new JScrollPane(tableEvenements);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane, BorderLayout.CENTER);

        // ── Barre de boutons ──────────────────────────────────────────────
        panel.add(construireBoutonsCRUD(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel construirePanneauRecherche() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        panel.setBackground(new Color(236, 240, 241));
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 210, 210)));

        // Titre de recherche
        JLabel lblRecherche = new JLabel("🔍 Recherche :");
        lblRecherche.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.add(lblRecherche);

        // Champ titre
        champRechercheTitre = new JTextField(15);
        champRechercheTitre.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        champRechercheTitre.putClientProperty("JTextField.placeholderText", "Titre...");
        styliserChampRecherche(champRechercheTitre);
        champRechercheTitre.addActionListener(e -> lancerRecherche());
        panel.add(new JLabel("Titre :"));
        panel.add(champRechercheTitre);

        // Champ date
        champRechercheDate = new JTextField(10);
        champRechercheDate.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        styliserChampRecherche(champRechercheDate);
        panel.add(new JLabel("Date :"));
        panel.add(champRechercheDate);

        // Filtre catégorie
        String[] catsAvecTout = prependArray("Toutes", Event.CATEGORIES);
        comboFiltreCategorie = new JComboBox<>(catsAvecTout);
        comboFiltreCategorie.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(new JLabel("Catégorie :"));
        panel.add(comboFiltreCategorie);

        // Filtre priorité
        String[] prioAvecTout = prependArray("Toutes", Event.PRIORITES);
        comboFiltrePriorite = new JComboBox<>(prioAvecTout);
        comboFiltrePriorite.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(new JLabel("Priorité :"));
        panel.add(comboFiltrePriorite);

        // Boutons recherche / réinitialiser
        JButton btnRechercher = creerBoutonMini("Chercher", BLEU_FONCE);
        btnRechercher.addActionListener(e -> lancerRecherche());
        panel.add(btnRechercher);

        JButton btnReset = creerBoutonMini("Réinitialiser", new Color(127, 140, 141));
        btnReset.addActionListener(e -> reinitialiserRecherche());
        panel.add(btnReset);

        return panel;
    }

    private JPanel construireBoutonsCRUD() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        panel.setBackground(new Color(248, 249, 250));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)));

        JButton btnAjouter   = creerBoutonAction("➕ Ajouter",   VERT);
        JButton btnModifier  = creerBoutonAction("✏️ Modifier",  BLEU_FONCE);
        JButton btnSupprimer = creerBoutonAction("🗑 Supprimer", ROUGE);
        JButton btnExportCSV = creerBoutonAction("📄 Export CSV", ORANGE);
        JButton btnExportHTML= creerBoutonAction("🌐 Export HTML",new Color(142, 68, 173));

        btnAjouter.addActionListener   (e -> ouvrirFormulaireAjout());
        btnModifier.addActionListener  (e -> ouvrirFormulaireModification());
        btnSupprimer.addActionListener (e -> supprimerEvenementSelectionne());
        btnExportCSV.addActionListener (e -> exporterCSV());
        btnExportHTML.addActionListener(e -> exporterHTML());

        panel.add(btnAjouter);
        panel.add(btnModifier);
        panel.add(btnSupprimer);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(btnExportCSV);
        panel.add(btnExportHTML);

        // Compteur de résultats
        labelStatut = new JLabel();
        labelStatut.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        labelStatut.setForeground(Color.GRAY);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(labelStatut);

        return panel;
    }

    private JPanel construireOngletCalendrier() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BLANC);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        calendarPanel = new CalendarPanel();
        calendarPanel.setOnMoisChange(() -> chargerCalendrier());

        panel.add(calendarPanel, BorderLayout.CENTER);

        // Légende
        JPanel legende = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        legende.setBackground(new Color(248, 249, 250));
        legende.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)));
        legende.add(new JLabel("Légende :"));
        CalendarPanel.COULEURS_CAT.forEach((cat, couleur) -> {
            JLabel lbl = new JLabel("  " + cat + "  ");
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lbl.setForeground(Color.WHITE);
            lbl.setOpaque(true);
            lbl.setBackground(couleur);
            lbl.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            legende.add(lbl);
        });
        panel.add(legende, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel construireOngletStatistiques() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BLANC);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        statsPanel = new StatisticsPanel();
        panel.add(statsPanel, BorderLayout.CENTER);

        JButton btnActualiser = creerBoutonAction("🔄 Actualiser", BLEU_FONCE);
        btnActualiser.addActionListener(e -> chargerStatistiques());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.setBackground(BLANC);
        bottom.add(btnActualiser);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel construireOngletImportants() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BLANC);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titre = new JLabel("⭐ Événements Importants & Urgents à venir");
        titre.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titre.setForeground(new Color(44, 62, 80));
        panel.add(titre, BorderLayout.NORTH);

        modelListeImportants = new DefaultListModel<>();
        listeImportants = new JList<>(modelListeImportants);
        listeImportants.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        listeImportants.setCellRenderer(new ImportantListCellRenderer());
        listeImportants.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(listeImportants);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(210, 210, 210)));
        panel.add(scroll, BorderLayout.CENTER);

        JLabel info = new JLabel("💡 Double-cliquez sur un événement pour le modifier");
        info.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        info.setForeground(Color.GRAY);
        panel.add(info, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel construireBarreStatut() {
        JPanel barre = new JPanel(new BorderLayout());
        barre.setBackground(new Color(44, 62, 80));
        barre.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));

        JLabel connexion = new JLabel("✅ Connecté à MySQL  |  Base : agenda_personnel");
        connexion.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        connexion.setForeground(new Color(189, 218, 245));
        barre.add(connexion, BorderLayout.WEST);

        JLabel version = new JLabel("Agenda Personnel v1.0  |  Java POO - JDBC");
        version.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        version.setForeground(new Color(150, 180, 200));
        barre.add(version, BorderLayout.EAST);

        return barre;
    }

    private JMenuBar construireMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Menu Fichier
        JMenu menuFichier = new JMenu("Fichier");
        menuFichier.setMnemonic(KeyEvent.VK_F);
        ajouterMenuItemAvecRaccourci(menuFichier, "➕ Nouvel événement", KeyEvent.VK_N,
                e -> ouvrirFormulaireAjout());
        menuFichier.addSeparator();
        ajouterMenuItem(menuFichier, "📄 Exporter en CSV",  e -> exporterCSV());
        ajouterMenuItem(menuFichier, "🌐 Exporter en HTML", e -> exporterHTML());
        menuFichier.addSeparator();
        ajouterMenuItem(menuFichier, "🔄 Actualiser",       e -> chargerDonnees());
        menuFichier.addSeparator();
        ajouterMenuItemAvecRaccourci(menuFichier, "❌ Quitter", KeyEvent.VK_Q,
                e -> System.exit(0));

        // Menu Édition
        JMenu menuEdition = new JMenu("Édition");
        ajouterMenuItem(menuEdition, "✏️ Modifier l'événement sélectionné",
                e -> ouvrirFormulaireModification());
        ajouterMenuItem(menuEdition, "🗑 Supprimer l'événement sélectionné",
                e -> supprimerEvenementSelectionne());
        menuEdition.addSeparator();
        ajouterMenuItem(menuEdition, "🗑 Supprimer les événements passés",
                e -> supprimerEvenementsPasses());

        // Menu Aide
        JMenu menuAide = new JMenu("Aide");
        ajouterMenuItem(menuAide, "ℹ️ À propos", e -> afficherAPropos());

        menuBar.add(menuFichier);
        menuBar.add(menuEdition);
        menuBar.add(menuAide);
        return menuBar;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  LOGIQUE MÉTIER (Contrôleur implicite)
    // ═══════════════════════════════════════════════════════════════════════

    /** Charge tous les événements depuis la base de données. */
    private void chargerDonnees() {
        try {
            List<Event> events = eventDAO.lireTous();
            tableModel.setEvents(events);
            mettreAJourStatut(events.size());
        } catch (SQLException ex) {
            afficherErreurSQL("chargement des événements", ex);
        }
    }

    /** Lance la recherche multicritères. */
    private void lancerRecherche() {
        try {
            String titre = champRechercheTitre.getText().trim();
            String dateStr = champRechercheDate.getText().trim();
            String cat  = (String) comboFiltreCategorie.getSelectedItem();
            String prio = (String) comboFiltrePriorite.getSelectedItem();

            LocalDate date = null;
            if (!dateStr.isEmpty()) {
                try {
                    date = LocalDate.parse(dateStr, Event.FMT_DATE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this,
                        "Format de date invalide. Utilisez jj/mm/aaaa",
                        "Erreur", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            List<Event> resultats = eventDAO.rechercher(titre, date, cat, prio);
            tableModel.setEvents(resultats);
            mettreAJourStatut(resultats.size());

        } catch (SQLException ex) {
            afficherErreurSQL("la recherche", ex);
        }
    }

    /** Réinitialise les filtres de recherche. */
    private void reinitialiserRecherche() {
        champRechercheTitre.setText("");
        champRechercheDate.setText("");
        comboFiltreCategorie.setSelectedIndex(0);
        comboFiltrePriorite.setSelectedIndex(0);
        chargerDonnees();
    }

    /** Ouvre le formulaire d'ajout d'un nouvel événement. */
    private void ouvrirFormulaireAjout() {
        EventFormDialog dialog = new EventFormDialog(this);
        dialog.setVisible(true);

        if (dialog.isConfirme()) {
            try {
                eventDAO.inserer(dialog.getEvent());
                chargerDonnees();
                afficherSucces("Événement ajouté avec succès !");
            } catch (SQLException ex) {
                afficherErreurSQL("l'ajout de l'événement", ex);
            }
        }
    }

    /** Ouvre le formulaire de modification de l'événement sélectionné. */
    private void ouvrirFormulaireModification() {
        int row = tableEvenements.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                "Sélectionnez un événement dans le tableau pour le modifier.",
                "Aucune sélection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Convertit l'index de ligne (vue) en index de ligne (modèle)
        // Nécessaire si le tableau est trié
        int modelRow = tableEvenements.convertRowIndexToModel(row);
        Event event = tableModel.getEventAt(modelRow);

        EventFormDialog dialog = new EventFormDialog(this, event);
        dialog.setVisible(true);

        if (dialog.isConfirme()) {
            try {
                eventDAO.modifier(dialog.getEvent());
                chargerDonnees();
                afficherSucces("Événement modifié avec succès !");
            } catch (SQLException ex) {
                afficherErreurSQL("la modification de l'événement", ex);
            }
        }
    }

    /** Supprime l'événement sélectionné après confirmation. */
    private void supprimerEvenementSelectionne() {
        int row = tableEvenements.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                "Sélectionnez un événement à supprimer.",
                "Aucune sélection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int modelRow = tableEvenements.convertRowIndexToModel(row);
        Event event  = tableModel.getEventAt(modelRow);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Supprimer l'événement :\n\"" + event.getTitre() + "\" (" + event.getDateFormatee() + ") ?",
            "Confirmation de suppression",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                eventDAO.supprimer(event.getId());
                chargerDonnees();
                afficherSucces("Événement supprimé.");
            } catch (SQLException ex) {
                afficherErreurSQL("la suppression", ex);
            }
        }
    }

    /** Supprime les événements passés après confirmation. */
    private void supprimerEvenementsPasses() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Supprimer TOUS les événements dont la date est passée ?",
            "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                int nb = eventDAO.supprimerEvenementsPasses();
                chargerDonnees();
                JOptionPane.showMessageDialog(this,
                    nb + " événement(s) passé(s) supprimé(s).",
                    "Nettoyage effectué", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                afficherErreurSQL("la suppression des événements passés", ex);
            }
        }
    }

    /** Charge les événements pour le mois affiché dans le calendrier. */
    private void chargerCalendrier() {
        try {
            YearMonth mois = calendarPanel.getMoisAffiche();
            List<Event> events = eventDAO.lireParMois(mois.getYear(), mois.getMonthValue());

            // Groupement par date
            Map<LocalDate, List<Event>> parJour = events.stream()
                .collect(Collectors.groupingBy(Event::getDate));

            calendarPanel.actualiser(parJour);

        } catch (SQLException ex) {
            afficherErreurSQL("le chargement du calendrier", ex);
        }
    }

    /** Charge les statistiques. */
    private void chargerStatistiques() {
        try {
            Map<String, Integer> statsCat  = eventDAO.compterParCategorie();
            Map<String, Integer> statsPrio = eventDAO.compterParPriorite();
            statsPanel.mettreAJour(statsCat, statsPrio);
        } catch (SQLException ex) {
            afficherErreurSQL("les statistiques", ex);
        }
    }

    /** Charge les événements importants. */
    private void chargerImportants() {
        try {
            List<Event> importants = eventDAO.lireImportants();
            modelListeImportants.clear();
            for (Event e : importants) {
                String puce = "Urgente".equals(e.getPriorite()) ? "🔴 " : "🟡 ";
                modelListeImportants.addElement(String.format(
                    "%s[%s]  %s  —  %s à %s  —  %s",
                    puce, e.getPriorite(), e.getTitre(),
                    e.getDateFormatee(), e.getHeureFormatee(), e.getCategorie()
                ));
            }
            if (importants.isEmpty()) {
                modelListeImportants.addElement("✅ Aucun événement important ou urgent à venir.");
            }
        } catch (SQLException ex) {
            afficherErreurSQL("les événements importants", ex);
        }
    }

    /**
     * Vérifie les événements du jour au démarrage et affiche les rappels.
     * Fonctionnalité supplémentaire : notification de démarrage.
     */
    private void verifierRappels() {
        try {
            List<Event> aujourdhui = eventDAO.lireAujourdhui()
                .stream().filter(Event::isRappel).collect(Collectors.toList());

            if (!aujourdhui.isEmpty()) {
                StringBuilder msg = new StringBuilder(
                    "📅 Vous avez " + aujourdhui.size() + " rappel(s) aujourd'hui :\n\n"
                );
                for (Event e : aujourdhui) {
                    msg.append(String.format("  • %s à %s  [%s]\n",
                        e.getTitre(), e.getHeureFormatee(), e.getPriorite()));
                }
                // Affichage différé (après l'affichage de la fenêtre)
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, msg.toString(),
                        "🔔 Rappels du jour", JOptionPane.INFORMATION_MESSAGE)
                );
            }
        } catch (SQLException ex) {
            // Silencieux au démarrage
        }
    }

    /** Export CSV de la liste actuellement affichée. */
    private void exporterCSV() {
        List<Event> events = getEvenementAffiche();
        if (events.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Aucun événement à exporter.");
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(ExportUtils.genererNomFichier("export_agenda", "csv")));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ExportUtils.exporterCSV(events, fc.getSelectedFile().getAbsolutePath());
                afficherSucces("Export CSV réussi : " + fc.getSelectedFile().getName());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erreur export : " + ex.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** Export HTML de la liste actuellement affichée. */
    private void exporterHTML() {
        List<Event> events = getEvenementAffiche();
        if (events.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Aucun événement à exporter.");
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(ExportUtils.genererNomFichier("export_agenda", "html")));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ExportUtils.exporterHTML(events, fc.getSelectedFile().getAbsolutePath());
                afficherSucces("Export HTML réussi : " + fc.getSelectedFile().getName());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erreur export : " + ex.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** Récupère les événements actuellement affichés dans le tableau. */
    private List<Event> getEvenementAffiche() {
        List<Event> result = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Event e = tableModel.getEventAt(i);
            if (e != null) result.add(e);
        }
        return result;
    }

    private void afficherAPropos() {
        JOptionPane.showMessageDialog(this,
            "📅 Agenda Personnel v1.0\n\n" +
            "Projet Java POO — JDBC — Swing\n" +
            "Architecture : Modèle-Vue-Contrôleur\n" +
            "Base de données : MySQL\n\n" +
            "© 2026 — TP Noté AV",
            "À propos", JOptionPane.INFORMATION_MESSAGE);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HELPERS UI
    // ═══════════════════════════════════════════════════════════════════════

    private void styliserTableau(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(28);
        table.setIntercellSpacing(new Dimension(10, 0));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(230, 230, 230));
        table.setSelectionBackground(new Color(186, 220, 255));
        table.setSelectionForeground(new Color(30, 30, 30));
        table.setFillsViewportHeight(true);

        // En-tête
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(52, 73, 94));
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 32));

        // Tri par clic sur en-tête
        table.setAutoCreateRowSorter(true);

        // Largeurs des colonnes
        int[] largeurs = {50, 250, 90, 60, 120, 100, 200};
        for (int i = 0; i < largeurs.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(largeurs[i]);
        }

        // Renderer coloré par priorité
        table.setDefaultRenderer(Object.class, new PrioriteTableCellRenderer());
    }

    private void mettreAJourStatut(int nb) {
        if (labelStatut != null) {
            labelStatut.setText(nb + " événement(s) affiché(s)");
        }
    }

    private void afficherSucces(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Succès", JOptionPane.INFORMATION_MESSAGE);
    }

    private void afficherErreurSQL(String contexte, SQLException ex) {
        JOptionPane.showMessageDialog(this,
            "Erreur lors de " + contexte + " :\n" + ex.getMessage() +
            "\n\nCode SQL : " + ex.getSQLState(),
            "Erreur Base de Données", JOptionPane.ERROR_MESSAGE);
        ex.printStackTrace();
    }

    private JButton creerBoutonAction(String texte, Color bg) {
        JButton btn = new JButton(texte);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(BLANC);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(140, 32));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(bg.darker()); }
            @Override public void mouseExited (MouseEvent e) { btn.setBackground(bg); }
        });
        return btn;
    }

    private JButton creerBoutonMini(String texte, Color bg) {
        JButton btn = new JButton(texte);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btn.setBackground(bg);
        btn.setForeground(BLANC);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void ajouterMenuItem(JMenu menu, String texte, ActionListener action) {
        JMenuItem item = new JMenuItem(texte);
        item.addActionListener(action);
        menu.add(item);
    }

    private void ajouterMenuItemAvecRaccourci(JMenu menu, String texte,
                                               int keyCode, ActionListener action) {
        JMenuItem item = new JMenuItem(texte);
        item.setAccelerator(KeyStroke.getKeyStroke(keyCode, InputEvent.CTRL_DOWN_MASK));
        item.addActionListener(action);
        menu.add(item);
    }

    private void styliserChampRecherche(JTextField field) {
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(3, 6, 3, 6)
        ));
    }

    private String[] prependArray(String prefix, String[] arr) {
        String[] result = new String[arr.length + 1];
        result[0] = prefix;
        System.arraycopy(arr, 0, result, 1, arr.length);
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CLASSES INTERNES : Renderers personnalisés
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Renderer de tableau qui colore les lignes selon la priorité.
     */
    private class PrioriteTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(table, value,
                    isSelected, hasFocus, row, column);

            if (!isSelected) {
                int modelRow = table.convertRowIndexToModel(row);
                Event event  = tableModel.getEventAt(modelRow);
                if (event != null) {
                    if (event.isUrgent()) {
                        c.setBackground(new Color(255, 235, 235)); // Rouge pastel
                    } else if (event.isImportant()) {
                        c.setBackground(new Color(255, 248, 220)); // Jaune pastel
                    } else if (event.isPasse()) {
                        c.setBackground(new Color(240, 240, 240)); // Gris (passé)
                        c.setForeground(Color.GRAY);
                    } else {
                        c.setBackground(BLANC);
                        c.setForeground(Color.BLACK);
                    }
                }
            }
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            return c;
        }
    }

    /**
     * Renderer de la liste des événements importants avec couleurs.
     */
    private static class ImportantListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {

            JLabel lbl = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lbl.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

            if (!isSelected) {
                String txt = value.toString();
                if (txt.contains("[Urgente]")) {
                    lbl.setBackground(new Color(255, 235, 235));
                } else if (txt.contains("[Importante]")) {
                    lbl.setBackground(new Color(255, 248, 220));
                }
            }
            return lbl;
        }
    }
}
