package view;

import model.Event;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * ============================================================
 *  CLASSE : EventFormDialog  (Formulaire Ajout / Modification)
 * ============================================================
 *  Boîte de dialogue modale pour saisir ou modifier un Event.
 *  Utilisée pour les opérations CREATE et UPDATE.
 *
 *  Validation des champs au moment de la soumission :
 *   - Titre obligatoire
 *   - Date au format dd/MM/yyyy valide
 *   - Heure au format HH:mm valide
 * ============================================================
 */
public class EventFormDialog extends JDialog {

    // ── Résultat de la boîte de dialogue ──────────────────────────────────
    private Event     eventResultat; // null si annulé
    private boolean   confirme = false;

    // ── Champs du formulaire ───────────────────────────────────────────────
    private JTextField      champTitre;
    private JTextField      champDate;
    private JTextField      champHeure;
    private JTextArea       champDescription;
    private JComboBox<String> comboCategorie;
    private JComboBox<String> comboPriorite;
    private JCheckBox       checkRappel;
    private JTextField      champLieu;

    // ── Couleurs & Styles ─────────────────────────────────────────────────
    private static final Color BLEU_FONCE  = new Color(41, 128, 185);
    private static final Color BLANC       = Color.WHITE;
    private static final Color GRIS_CLAIR  = new Color(245, 245, 245);
    private static final Font  FONT_LABEL  = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font  FONT_CHAMP  = new Font("Segoe UI", Font.PLAIN, 13);

    /**
     * Constructeur — ouvre le dialogue en mode Création.
     */
    public EventFormDialog(Frame parent) {
        this(parent, null);
    }

    /**
     * Constructeur — ouvre le dialogue en mode Modification.
     *
     * @param parent La fenêtre parente
     * @param event  L'événement à modifier (null = création)
     */
    public EventFormDialog(Frame parent, Event event) {
        super(parent, event == null ? "➕ Nouvel événement" : "✏️ Modifier l'événement", true);
        initialiserUI(event);
        pack();
        setMinimumSize(new Dimension(520, 500));
        setLocationRelativeTo(parent);
    }

    /** Construit et arrange tous les composants Swing du formulaire. */
    private void initialiserUI(Event eventExistant) {
        setBackground(BLANC);

        // ── Panneau principal ──────────────────────────────────────────────
        JPanel panelPrincipal = new JPanel(new BorderLayout(0, 0));
        panelPrincipal.setBackground(BLANC);

        // ── En-tête coloré ────────────────────────────────────────────────
        JPanel panelEntete = new JPanel();
        panelEntete.setBackground(BLEU_FONCE);
        panelEntete.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        panelEntete.setLayout(new FlowLayout(FlowLayout.LEFT));

        JLabel labelTitreDialog = new JLabel(
            eventExistant == null ? "  📅  Nouvel Événement" : "  ✏️  Modifier l'Événement"
        );
        labelTitreDialog.setFont(new Font("Segoe UI", Font.BOLD, 16));
        labelTitreDialog.setForeground(BLANC);
        panelEntete.add(labelTitreDialog);

        // ── Grille des champs ──────────────────────────────────────────────
        JPanel panelChamps = new JPanel(new GridBagLayout());
        panelChamps.setBackground(BLANC);
        panelChamps.setBorder(BorderFactory.createEmptyBorder(20, 25, 10, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(6, 5, 6, 5);
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.anchor  = GridBagConstraints.WEST;

        int row = 0;

        // Titre
        champTitre = creerChampTexte(30);
        ajouterLigne(panelChamps, gbc, row++, "Titre *", champTitre);

        // Date + Heure sur la même ligne
        JPanel panelDateHeure = new JPanel(new GridLayout(1, 2, 10, 0));
        panelDateHeure.setBackground(BLANC);
        champDate  = creerChampTexte(10);
        champDate.setText(LocalDate.now().format(Event.FMT_DATE));
        champHeure = creerChampTexte(6);
        champHeure.setText("09:00");
        JPanel pd = labelledPanel("Date * (jj/mm/aaaa)", champDate);
        JPanel ph = labelledPanel("Heure * (HH:mm)", champHeure);
        panelDateHeure.add(pd);
        panelDateHeure.add(ph);
        gbc.gridx=0; gbc.gridy=row++; gbc.gridwidth=2;
        panelChamps.add(panelDateHeure, gbc);
        gbc.gridwidth=1;

        // Catégorie + Priorité sur la même ligne
        JPanel panelCatPrio = new JPanel(new GridLayout(1, 2, 10, 0));
        panelCatPrio.setBackground(BLANC);
        comboCategorie = creerCombo(Event.CATEGORIES);
        comboPriorite  = creerCombo(Event.PRIORITES);
        JPanel pc = labelledPanel("Catégorie", comboCategorie);
        JPanel pp = labelledPanel("Priorité",  comboPriorite);
        panelCatPrio.add(pc);
        panelCatPrio.add(pp);
        gbc.gridx=0; gbc.gridy=row++; gbc.gridwidth=2;
        panelChamps.add(panelCatPrio, gbc);
        gbc.gridwidth=1;

        // Lieu
        champLieu = creerChampTexte(30);
        ajouterLigne(panelChamps, gbc, row++, "Lieu", champLieu);

        // Description
        champDescription = new JTextArea(4, 30);
        champDescription.setFont(FONT_CHAMP);
        champDescription.setLineWrap(true);
        champDescription.setWrapStyleWord(true);
        champDescription.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        JScrollPane scroll = new JScrollPane(champDescription);
        ajouterLigne(panelChamps, gbc, row++, "Description", scroll);

        // Rappel
        checkRappel = new JCheckBox("Activer le rappel au démarrage");
        checkRappel.setFont(FONT_CHAMP);
        checkRappel.setBackground(BLANC);
        checkRappel.setForeground(new Color(60, 60, 60));
        gbc.gridx=0; gbc.gridy=row++; gbc.gridwidth=2;
        panelChamps.add(checkRappel, gbc);
        gbc.gridwidth=1;

        // ── Pré-remplissage en mode modification ───────────────────────────
        if (eventExistant != null) {
            champTitre.setText(eventExistant.getTitre());
            champDate.setText(eventExistant.getDateFormatee());
            champHeure.setText(eventExistant.getHeureFormatee());
            champDescription.setText(eventExistant.getDescription());
            comboCategorie.setSelectedItem(eventExistant.getCategorie());
            comboPriorite.setSelectedItem(eventExistant.getPriorite());
            checkRappel.setSelected(eventExistant.isRappel());
            champLieu.setText(eventExistant.getLieu() != null ? eventExistant.getLieu() : "");
        }

        // ── Boutons OK / Annuler ───────────────────────────────────────────
        JPanel panelBoutons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 12));
        panelBoutons.setBackground(GRIS_CLAIR);
        panelBoutons.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)));

        JButton btnAnnuler = new JButton("Annuler");
        styliserBouton(btnAnnuler, new Color(149, 165, 166), BLANC);
        btnAnnuler.addActionListener(e -> dispose());

        JButton btnOK = new JButton(eventExistant == null ? "  Ajouter  " : "  Modifier  ");
        styliserBouton(btnOK, BLEU_FONCE, BLANC);
        btnOK.addActionListener(e -> soumettreFormulaire(eventExistant));

        // Touche Entrée → soumettre le formulaire
        getRootPane().setDefaultButton(btnOK);

        panelBoutons.add(btnAnnuler);
        panelBoutons.add(btnOK);

        // ── Assemblage ────────────────────────────────────────────────────
        panelPrincipal.add(panelEntete,  BorderLayout.NORTH);
        panelPrincipal.add(panelChamps, BorderLayout.CENTER);
        panelPrincipal.add(panelBoutons, BorderLayout.SOUTH);

        setContentPane(panelPrincipal);
    }

    /**
     * Valide et collecte les données du formulaire.
     * En cas d'erreur, affiche un message et arrête la soumission.
     */
    private void soumettreFormulaire(Event eventExistant) {
        // ── Validation ────────────────────────────────────────────────────
        String titre = champTitre.getText().trim();
        if (titre.isEmpty()) {
            afficherErreur("Le titre est obligatoire.");
            champTitre.requestFocus();
            return;
        }
        if (titre.length() > 200) {
            afficherErreur("Le titre ne peut pas dépasser 200 caractères.");
            return;
        }

        LocalDate date;
        try {
            date = LocalDate.parse(champDate.getText().trim(), Event.FMT_DATE);
        } catch (DateTimeParseException ex) {
            afficherErreur("Format de date invalide. Utilisez jj/mm/aaaa (ex: 30/04/2026).");
            champDate.requestFocus();
            return;
        }

        LocalTime heure;
        try {
            heure = LocalTime.parse(champHeure.getText().trim(),
                    DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException ex) {
            afficherErreur("Format d'heure invalide. Utilisez HH:mm (ex: 14:30).");
            champHeure.requestFocus();
            return;
        }

        // ── Construction de l'objet Event ─────────────────────────────────
        int id = (eventExistant != null) ? eventExistant.getId() : 0;

        eventResultat = new Event(
            id,
            titre,
            date,
            heure,
            champDescription.getText().trim(),
            (String) comboCategorie.getSelectedItem(),
            (String) comboPriorite.getSelectedItem(),
            checkRappel.isSelected(),
            champLieu.getText().trim()
        );

        confirme = true;
        dispose(); // Ferme la boîte de dialogue
    }

    // ── Getters ───────────────────────────────────────────────────────────

    /** @return L'Event saisi, ou null si l'utilisateur a annulé */
    public Event getEvent()      { return eventResultat; }
    public boolean isConfirme()  { return confirme; }

    // ── Helpers UI ────────────────────────────────────────────────────────

    private JTextField creerChampTexte(int colonnes) {
        JTextField f = new JTextField(colonnes);
        f.setFont(FONT_CHAMP);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        return f;
    }

    private JComboBox<String> creerCombo(String[] items) {
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setFont(FONT_CHAMP);
        combo.setBackground(BLANC);
        return combo;
    }

    private void ajouterLigne(JPanel panel, GridBagConstraints gbc,
                               int row, String labelTexte, JComponent champ) {
        JLabel label = new JLabel(labelTexte + " :");
        label.setFont(FONT_LABEL);
        label.setForeground(new Color(70, 70, 70));

        gbc.gridx=0; gbc.gridy=row; gbc.gridwidth=1; gbc.weightx=0.3;
        panel.add(label, gbc);

        gbc.gridx=1; gbc.weightx=0.7;
        panel.add(champ, gbc);
    }

    private JPanel labelledPanel(String labelText, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setBackground(BLANC);
        JLabel l = new JLabel(labelText + " :");
        l.setFont(FONT_LABEL);
        l.setForeground(new Color(70, 70, 70));
        p.add(l, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private void styliserBouton(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(120, 36));

        // Effet survol
        btn.addMouseListener(new MouseAdapter() {
            Color original = bg;
            @Override public void mouseEntered(MouseEvent e) {
                btn.setBackground(bg.darker());
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(original);
            }
        });
    }

    private void afficherErreur(String message) {
        JOptionPane.showMessageDialog(this, message, "Erreur de saisie",
                JOptionPane.WARNING_MESSAGE);
    }
}
