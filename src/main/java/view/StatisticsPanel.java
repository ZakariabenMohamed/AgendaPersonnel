package view;

import model.Event;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * ============================================================
 *  CLASSE : StatisticsPanel  (Panneau Statistiques)
 * ============================================================
 *  Affiche un graphique en camembert (Pie Chart) dessiné
 *  manuellement avec l'API Java2D (Graphics2D).
 *  Pas de librairie externe nécessaire.
 * ============================================================
 */
public class StatisticsPanel extends JPanel {

    // Couleurs par catégorie (cohérentes avec le tableau principal)
    private static final Map<String, Color> COULEURS_CAT = new LinkedHashMap<>();
    static {
        COULEURS_CAT.put("Personnel",      new Color(52, 152, 219));  // Bleu
        COULEURS_CAT.put("Professionnel",  new Color(46, 204, 113));  // Vert
        COULEURS_CAT.put("Études",         new Color(155, 89, 182)); // Violet
        COULEURS_CAT.put("Autre",          new Color(149, 165, 166)); // Gris
    }

    private static final Color[] COULEURS_PRIO = {
        new Color(46, 204, 113),   // Normale  → Vert
        new Color(243, 156, 18),   // Importante → Orange
        new Color(231, 76, 60)     // Urgente  → Rouge
    };

    private Map<String, Integer> statsCategorie = new HashMap<>();
    private Map<String, Integer> statsPriorite  = new HashMap<>();
    private int total = 0;

    public StatisticsPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(0, 280));
    }

    /** Met à jour les statistiques et redessine le panneau */
    public void mettreAJour(Map<String, Integer> statsCategorie,
                             Map<String, Integer> statsPriorite) {
        this.statsCategorie = statsCategorie;
        this.statsPriorite  = statsPriorite;
        this.total = statsCategorie.values().stream().mapToInt(Integer::intValue).sum();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (total == 0) {
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            g2.setColor(Color.GRAY);
            String msg = "Aucune donnée à afficher";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
            return;
        }

        // ── Titre ──────────────────────────────────────────────────────────
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2.setColor(new Color(44, 62, 80));
        g2.drawString("Répartition par catégorie  (" + total + " événements)", 20, 25);

        // ── Camembert ──────────────────────────────────────────────────────
        int pieX = 20, pieY = 40, pieW = 180, pieH = 180;
        dessinerCamembert(g2, statsCategorie, COULEURS_CAT, pieX, pieY, pieW, pieH);

        // ── Légende catégories ────────────────────────────────────────────
        dessinerLegende(g2, statsCategorie, COULEURS_CAT, pieX + pieW + 20, pieY);

        // ── Barres de priorité ────────────────────────────────────────────
        if (!statsPriorite.isEmpty()) {
            int barX = w / 2 + 20;
            int barY = 40;
            int barW = w - barX - 20;
            int barH = 180;
            dessinerBarresPriorite(g2, barX, barY, barW, barH);
        }
    }

    private void dessinerCamembert(Graphics2D g2, Map<String, Integer> stats,
                                    Map<String, Color> couleurs,
                                    int x, int y, int w, int h) {
        double startAngle = 0;
        List<String> keys = new ArrayList<>(stats.keySet());

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            int val = stats.getOrDefault(key, 0);
            double angle = 360.0 * val / total;

            Color couleur = couleurs.containsKey(key)
                    ? couleurs.get(key)
                    : COULEURS_PRIO[i % COULEURS_PRIO.length];

            g2.setColor(couleur);
            g2.fill(new Arc2D.Double(x, y, w, h, startAngle, angle, Arc2D.PIE));

            // Bordure blanche entre les tranches
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new Arc2D.Double(x, y, w, h, startAngle, angle, Arc2D.PIE));

            startAngle += angle;
        }
    }

    private void dessinerLegende(Graphics2D g2, Map<String, Integer> stats,
                                  Map<String, Color> couleurs, int x, int y) {
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        int decalage = 0;

        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            String key = entry.getKey();
            int val    = entry.getValue();
            double pct = (double) val / total * 100;

            Color c = couleurs.containsKey(key) ? couleurs.get(key) : Color.GRAY;

            // Carré de couleur
            g2.setColor(c);
            g2.fillRoundRect(x, y + decalage, 14, 14, 4, 4);

            // Texte
            g2.setColor(new Color(60, 60, 60));
            g2.drawString(String.format("%s : %d (%.0f%%)", key, val, pct),
                    x + 20, y + decalage + 12);

            decalage += 22;
        }
    }

    private void dessinerBarresPriorite(Graphics2D g2, int x, int y, int w, int h) {
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2.setColor(new Color(44, 62, 80));
        g2.drawString("Priorités", x, y - 5);

        String[] labels = {"Normale", "Importante", "Urgente"};
        int barHeight = 35;
        int gap = 15;
        int totalH = (barHeight + gap) * 3;
        int startY = y + (h - totalH) / 2;

        int maxVal = statsPriorite.values().stream().mapToInt(Integer::intValue).max().orElse(1);

        for (int i = 0; i < labels.length; i++) {
            String label = labels[i];
            int val = statsPriorite.getOrDefault(label, 0);
            int barW = (int) ((double) val / maxVal * (w - 100));

            int barY = startY + i * (barHeight + gap);

            // Fond gris
            g2.setColor(new Color(235, 235, 235));
            g2.fillRoundRect(x + 90, barY, w - 90, barHeight, 8, 8);

            // Barre colorée
            g2.setColor(COULEURS_PRIO[i]);
            if (barW > 0)
                g2.fillRoundRect(x + 90, barY, barW, barHeight, 8, 8);

            // Label
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.setColor(new Color(60, 60, 60));
            g2.drawString(label, x, barY + barHeight / 2 + 4);

            // Valeur
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.setColor(Color.WHITE);
            if (barW > 25)
                g2.drawString(String.valueOf(val), x + 95, barY + barHeight / 2 + 4);
            else {
                g2.setColor(new Color(60, 60, 60));
                g2.drawString(String.valueOf(val), x + 95 + barW, barY + barHeight / 2 + 4);
            }
        }
    }
}
