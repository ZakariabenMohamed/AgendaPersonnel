# 📅 Agenda Personnel — Projet Java POO/JDBC

**Binôme :** Zakaria ben Mohamed & Feryel ben Yahia
**Date :** Avril 2026  
**Cours :** Programmation Orientée Objet — JDBC

---

## 🏗️ Architecture du projet

```
AgendaPersonnel/
├── database.sql                          ← Script de création MySQL
├── README.md                             ← Ce fichier
└── src/main/java/
    ├── Main.java                         ← Point d'entrée
    ├── model/
    │   └── Event.java                    ← Entité / JavaBean
    ├── database/
    │   └── DatabaseManager.java          ← Connexion JDBC (Singleton)
    ├── dao/
    │   └── EventDAO.java                 ← CRUD + Recherche SQL
    ├── view/
    │   ├── MainView.java                 ← Fenêtre principale (Swing)
    │   ├── EventFormDialog.java          ← Formulaire ajout/modif
    │   ├── EventTableModel.java          ← Modèle JTable personnalisé
    │   ├── CalendarPanel.java            ← Vue calendrier mensuelle
    │   └── StatisticsPanel.java          ← Graphiques Java2D
    └── utils/
        └── ExportUtils.java              ← Export CSV / HTML
```

---

## ⚙️ Prérequis

| Logiciel          | Version minimale |
| ----------------- | ---------------- |
| JDK               | 17+              |
| MySQL Server      | 8.0+             |
| mysql-connector-j | 8.0+             |

---

## 🚀 Installation

### 1. Base de données MySQL

```sql
-- Dans MySQL Workbench, HeidiSQL ou la console :
SOURCE /chemin/vers/database.sql;
```

Ou copiez-collez le contenu de `database.sql` dans votre client MySQL.

### 2. Pilote JDBC

Téléchargez **mysql-connector-j-8.x.x.jar** depuis :  
https://dev.mysql.com/downloads/connector/j/

**IntelliJ IDEA :**  
`File → Project Structure → Libraries → + → Java → sélectionner le JAR`

**NetBeans :**  
Clic droit sur `Libraries → Add JAR/Folder`

### 3. Configuration de la connexion

Modifiez `src/main/java/database/DatabaseManager.java` :

```java
private static final String HOST     = "localhost";
private static final String PORT     = "3306";
private static final String DATABASE = "agenda_personnel";
private static final String USER     = "root";
private static final String PASSWORD = "zakaria33..w";  // ← ICI
```

### 4. Lancement

```
Exécuter : Main.java
```

---

## ✨ Fonctionnalités

### Obligatoires ✅

- [x] CRUD complet (Créer, Lire, Modifier, Supprimer)
- [x] Recherche multicritères (titre, date, catégorie, priorité)
- [x] Visualisation des événements importants (onglet dédié)
- [x] Stockage MySQL via JDBC
- [x] Interface graphique Swing

### Supplémentaires ✅

- [x] Vue calendrier mensuelle avec couleurs par catégorie
- [x] Export CSV (compatible Excel) et HTML
- [x] Notifications/rappels au démarrage
- [x] Statistiques graphiques (camembert + barres) en Java2D pur
- [x] Tri par colonnes (clic sur en-tête)
- [x] Nettoyage des événements passés
- [x] Splash screen au démarrage
- [x] Menu avec raccourcis clavier

---

## 🎨 Interface

| Couleur       | Signification       |
| ------------- | ------------------- |
| 🔵 Bleu       | Personnel           |
| 🟢 Vert       | Professionnel       |
| 🟣 Violet     | Études              |
| ⚪ Gris       | Autre               |
| 🔴 Fond rouge | Priorité Urgente    |
| 🟡 Fond jaune | Priorité Importante |
| Grisé         | Événement passé     |

---

## 📚 Concepts JDBC utilisés

| Concept            | Classe/Méthode                  | Utilisé dans      |
| ------------------ | ------------------------------- | ----------------- |
| Chargement pilote  | `Class.forName()`               | DatabaseManager   |
| Connexion          | `DriverManager.getConnection()` | DatabaseManager   |
| Requête statique   | `Statement`                     | stats, delete all |
| Requête paramétrée | `PreparedStatement`             | tout le CRUD      |
| Clé générée        | `RETURN_GENERATED_KEYS`         | inserer()         |
| Curseur défilant   | `TYPE_SCROLL_INSENSITIVE`       | lireTous()        |
| Mapping objet      | `ResultSet → Event`             | rsToEvent()       |
| Exceptions         | `SQLException`                  | partout           |
