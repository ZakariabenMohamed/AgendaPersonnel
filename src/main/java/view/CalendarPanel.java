package view;

import model.Event;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.util.*;
import java.util.List;

/**
 * ============================================================
 *  CLASSE : CalendarPanel  (Vue Calendrier Mensuelle)
 * ============================================================
 *  Affiche un calendrier mensuel avec les événements positionnés
 *  sur leurs jours respectifs. Chaque catégorie a sa propre couleur.
 *  Navigation avant/arrière entre les mois.
 * ============================================================
 */
public class CalendarPanel extends JPanel {

    // Couleurs par catégorie (identiques à StatisticsPanel)
    public static final Map<String, Color> COULEURS_CAT = new LinkedHashMap<>();
    static {
        COULEURS_CAT.put("Personnel",      new Color(52, 152, 219));
        COULEURS_CAT.put("Professionnel",  new Color(46, 204, 113));
        COULEURS_CAT.put("Études",         new Color(155, 89, 182));
        COULEURS_CAT.put("Autre",          new Color(149, 165, 166));
    }

    private YearMonth moisAffiche;
    private Map<LocalDate, List<Event>> evenementsParJour = new HashMap<>();

    // Callbacks
    private Runnable onMoisChange;

    // Navigation
    private JLabel labelMois;

    private static final String[] JOURS_SEMAINE = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
    private static final Color BLEU_ENTETE  = new Color(41, 128, 185);
    private static final Color VERT_WEEKEND = new Color(232, 245, 232);

    public CalendarPanel() {
        this.moisAffiche = YearMonth.now();
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        initialiserUI();
    }

    private void initialiserUI() {
        // ── Barre de navigation ────────────────────────────────────────────
        JPanel navPanel = new JPanel(new BorderLayout());
        navPanel.setBackground(BLEU_ENTETE);
        navPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JButton btnPrecedent = creerBoutonNav("◀");
        btnPrecedent.addActionListener(e -> changerMois(-1));

        JButton btnSuivant = creerBoutonNav("▶");
        btnSuivant.addActionListener(e -> changerMois(+1));

        JButton btnAujourdhui = creerBoutonNav("Aujourd'hui");
        btnAujourdhui.addActionListener(e -> {
            moisAffiche = YearMonth.now();
            actualiser(evenementsParJour);
        });

        labelMois = new JLabel("", SwingConstants.CENTER);
        labelMois.setFont(new Font("Segoe UI", Font.BOLD, 15));
        labelMois.setForeground(Color.WHITE);
        mettreAJourLabelMois();

        JPanel gauche = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        gauche.setOpaque(false);
        gauche.add(btnPrecedent);
        gauche.add(btnSuivant);
        gauche.add(btnAujourdhui);

        navPanel.add(gauche, BorderLayout.WEST);
        navPanel.add(labelMois, BorderLayout.CENTER);

        add(navPanel, BorderLayout.NORTH);
        add(construireGrilleCalendrier(), BorderLayout.CENTER);
    }

    /**
     * Met à jour le calendrier avec les événements du mois.
     *
     * @param evenementsParJour Map date → liste d'événements
     */
    public void actualiser(Map<LocalDate, List<Event>> evenementsParJour) {
        this.evenementsParJour = evenementsParJour;
        mettreAJourLabelMois();

        // Reconstruction de la grille
        Component center = ((BorderLayout) getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (center != null) remove(center);
        add(construireGrilleCalendrier(), BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private JPanel construireGrilleCalendrier() {
        JPanel grille = new JPanel(new GridLayout(0, 7, 1, 1));
        grille.setBackground(new Color(220, 220, 220)); // couleur des lignes
        grille.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        // ── En-têtes des jours ─────────────────────────────────────────────
        for (int i = 0; i < 7; i++) {
            JLabel lbl = new JLabel(JOURS_SEMAINE[i], SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lbl.setOpaque(true);
            lbl.setBackground(new Color(52, 73, 94));
            lbl.setForeground(Color.WHITE);
            lbl.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));
            grille.add(lbl);
        }

        // ── Cellules du mois ──────────────────────────────────────────────
        LocalDate premier = moisAffiche.atDay(1);
        int debutSemaine = premier.getDayOfWeek().getValue() - 1; // 0=Lun, 6=Dim

        // Cases vides avant le 1er
        for (int i = 0; i < debutSemaine; i++) {
            grille.add(celluleVide());
        }

        // Jours du mois
        for (int jour = 1; jour <= moisAffiche.lengthOfMonth(); jour++) {
            LocalDate date = moisAffiche.atDay(jour);
            List<Event> evts = evenementsParJour.getOrDefault(date, Collections.emptyList());
            grille.add(construireCellule(date, evts));
        }

        return grille;
    }

    private JPanel construireCellule(LocalDate date, List<Event> events) {
        JPanel cellule = new JPanel();
        cellule.setLayout(new BoxLayout(cellule, BoxLayout.Y_AXIS));
        cellule.setOpaque(true);

        boolean estAujourdhui = date.equals(LocalDate.now());
        boolean estWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY
                          || date.getDayOfWeek() == DayOfWeek.SUNDAY;

        // Fond de la cellule
        if (estAujourdhui)    cellule.setBackground(new Color(214, 234, 248));
        else if (estWeekend)  cellule.setBackground(VERT_WEEKEND);
        else                  cellule.setBackground(Color.WHITE);

        cellule.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));

        // Numéro du jour
        JLabel lblJour = new JLabel(String.valueOf(date.getDayOfMonth()));
        lblJour.setFont(new Font("Segoe UI",
                estAujourdhui ? Font.BOLD : Font.PLAIN, 12));
        lblJour.setForeground(estAujourdhui ? new Color(41, 128, 185) : new Color(50, 50, 50));
        cellule.add(lblJour);

        // Événements (max 2 visibles + "..." si plus)
        int maxVisible = 2;
        for (int i = 0; i < Math.min(events.size(), maxVisible); i++) {
            Event e = events.get(i);
            cellule.add(creerPastilleEvenement(e));
        }
        if (events.size() > maxVisible) {
            JLabel plus = new JLabel("+" + (events.size() - maxVisible) + " autres");
            plus.setFont(new Font("Segoe UI", Font.ITALIC, 9));
            plus.setForeground(Color.GRAY);
            cellule.add(plus);
        }

        cellule.setPreferredSize(new Dimension(0, 70));
        return cellule;
    }

    private JLabel creerPastilleEvenement(Event e) {
        JLabel lbl = new JLabel(" " + e.getHeureFormatee() + " " + tronquer(e.getTitre(), 10));
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        lbl.setForeground(Color.WHITE);
        lbl.setOpaque(true);

        Color bg = COULEURS_CAT.getOrDefault(e.getCategorie(), new Color(149, 165, 166));
        lbl.setBackground(bg);
        lbl.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 3));
        lbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));
        lbl.setToolTipText(e.getTitre() + " — " + e.getCategorie() + " — " + e.getPriorite());
        return lbl;
    }

    private JPanel celluleVide() {
        JPanel p = new JPanel();
        p.setBackground(new Color(248, 248, 248));
        return p;
    }

    private void changerMois(int delta) {
        moisAffiche = moisAffiche.plusMonths(delta);
        if (onMoisChange != null) onMoisChange.run();
        else actualiser(evenementsParJour);
    }

    private void mettreAJourLabelMois() {
        if (labelMois != null) {
            String[] MOIS = {"Janvier","Février","Mars","Avril","Mai","Juin",
                             "Juillet","Août","Septembre","Octobre","Novembre","Décembre"};
            labelMois.setText(MOIS[moisAffiche.getMonthValue() - 1] + " " + moisAffiche.getYear());
        }
    }

    private JButton creerBoutonNav(String texte) {
        JButton btn = new JButton(texte);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(52, 152, 219));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private String tronquer(String s, int max) {
        return (s != null && s.length() > max) ? s.substring(0, max) + "…" : s;
    }

    public YearMonth getMoisAffiche() { return moisAffiche; }
    public void setOnMoisChange(Runnable r) { this.onMoisChange = r; }
}
