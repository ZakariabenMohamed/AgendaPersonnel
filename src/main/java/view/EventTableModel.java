package view;

import model.Event;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 *  CLASSE : EventTableModel  (Modèle de données pour JTable)
 * ============================================================
 *  Extends AbstractTableModel pour fournir un modèle de données
 *  personnalisé à la JTable de la vue principale.
 *
 *  Avantages d'un modèle personnalisé vs DefaultTableModel :
 *   - Contrôle total sur les types de colonnes (getColumnClass)
 *   - Les cellules peuvent être non-éditables (isCellEditable)
 *   - Meilleure intégration avec les objets Event
 * ============================================================
 */
public class EventTableModel extends AbstractTableModel {

    // Noms des colonnes affichées dans le JTable
    private static final String[] COLONNES = {
        "#", "Titre", "Date", "Heure", "Catégorie", "Priorité", "Lieu"
    };

    // Types des colonnes (utilisé par JTable pour le rendu par défaut)
    @SuppressWarnings("rawtypes")
    private static final Class[] TYPES = {
        Integer.class, String.class, String.class, String.class,
        String.class, String.class, String.class
    };

    private List<Event> events;

    public EventTableModel() {
        this.events = new ArrayList<>();
    }

    public EventTableModel(List<Event> events) {
        this.events = new ArrayList<>(events);
    }

    // ── AbstractTableModel : méthodes obligatoires ─────────────────────────

    @Override public int getRowCount()    { return events.size(); }
    @Override public int getColumnCount() { return COLONNES.length; }
    @Override public String getColumnName(int col) { return COLONNES[col]; }
    @Override public Class<?> getColumnClass(int col) { return TYPES[col]; }

    @Override
    public Object getValueAt(int row, int col) {
        Event e = events.get(row);
        return switch (col) {
            case 0 -> e.getId();
            case 1 -> e.getTitre();
            case 2 -> e.getDateFormatee();
            case 3 -> e.getHeureFormatee();
            case 4 -> e.getCategorie();
            case 5 -> e.getPriorite();
            case 6 -> e.getLieu() != null ? e.getLieu() : "";
            default -> "";
        };
    }

    /** Les cellules du tableau ne sont pas directement éditables */
    @Override
    public boolean isCellEditable(int row, int col) { return false; }

    // ── Méthodes utilitaires pour mettre à jour les données ───────────────

    /** Remplace toute la liste et notifie la JTable */
    public void setEvents(List<Event> events) {
        this.events = new ArrayList<>(events);
        fireTableDataChanged(); // Notifie la JTable de rafraîchir l'affichage
    }

    /** Retourne l'Event à la ligne donnée */
    public Event getEventAt(int row) {
        if (row >= 0 && row < events.size()) return events.get(row);
        return null;
    }

    /** Ajoute un événement et rafraîchit */
    public void ajouterEvent(Event event) {
        events.add(event);
        fireTableRowsInserted(events.size() - 1, events.size() - 1);
    }

    /** Supprime l'événement à la ligne donnée */
    public void supprimerEvent(int row) {
        if (row >= 0 && row < events.size()) {
            events.remove(row);
            fireTableRowsDeleted(row, row);
        }
    }

    /** Met à jour un événement dans le modèle */
    public void mettreAJourEvent(int row, Event event) {
        if (row >= 0 && row < events.size()) {
            events.set(row, event);
            fireTableRowsUpdated(row, row);
        }
    }

    public int getTaille() { return events.size(); }
}
