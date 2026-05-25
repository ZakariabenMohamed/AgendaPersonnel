-- ============================================================
--  SCRIPT SQL - Agenda Personnel
--  Base de données : MySQL 8+
--  Auteurs : Zakaria ben Mohamed & Feryel ben Yahia
--  Date : 2026
-- ============================================================

-- Création (ou réinitialisation) de la base de données
DROP DATABASE IF EXISTS agenda_personnel;
CREATE DATABASE agenda_personnel
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE agenda_personnel;

-- ============================================================
--  TABLE : evenements
--  Stocke tous les événements de l'agenda personnel
-- ============================================================
CREATE TABLE evenements (
    id          INT AUTO_INCREMENT PRIMARY KEY,          -- Clé primaire auto-incrémentée
    titre       VARCHAR(200)    NOT NULL,                -- Titre de l'événement
    date_event  DATE            NOT NULL,                -- Date de l'événement (YYYY-MM-DD)
    heure_event TIME            NOT NULL,                -- Heure de l'événement (HH:MM:SS)
    description TEXT,                                    -- Description détaillée (optionnelle)
    categorie   ENUM('Personnel','Professionnel','Etudes','Autre') NOT NULL DEFAULT 'Personnel',
    priorite    ENUM('Normale','Importante','Urgente')   NOT NULL DEFAULT 'Normale',
    rappel      BOOLEAN         NOT NULL DEFAULT FALSE,  -- Activer le rappel ?
    lieu        VARCHAR(255),                            -- Lieu de l'événement (optionnel)
    date_creation TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    date_modif    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Index pour accélérer les recherches fréquentes
    INDEX idx_date        (date_event),
    INDEX idx_categorie   (categorie),
    INDEX idx_priorite    (priorite),
    INDEX idx_titre       (titre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
--  DONNÉES DE TEST - Pour démarrer avec un agenda pré-rempli
-- ============================================================
INSERT INTO evenements (titre, date_event, heure_event, description, categorie, priorite, rappel, lieu)
VALUES
    ('Réunion équipe projet',    CURDATE(),              '09:00:00', 'Réunion hebdomadaire de suivi de projet', 'Professionnel', 'Importante', TRUE,  'Salle de conférence A'),
    ('Cours JDBC',               CURDATE(),              '14:00:00', 'TP noté Programmation Orientée Objet',     'Etudes',        'Urgente',    TRUE,  'Amphi B'),
    ('Déjeuner famille',         DATE_ADD(CURDATE(), INTERVAL 1 DAY), '12:30:00', 'Déjeuner dominical en famille', 'Personnel', 'Normale', FALSE, 'Maison'),
    ('Rendu TP Java',            DATE_ADD(CURDATE(), INTERVAL 3 DAY), '23:59:00', 'Remise du TP Agenda Personnel', 'Etudes',  'Urgente',    TRUE,  'Moodle'),
    ('Examen Base de Données',   DATE_ADD(CURDATE(), INTERVAL 7 DAY), '08:00:00', 'Examen final SGBD',             'Etudes',  'Urgente',    TRUE,  'Salle 101'),
    ('Sport - Salle de gym',     DATE_ADD(CURDATE(), INTERVAL 2 DAY), '18:00:00', 'Séance de musculation',         'Personnel','Normale',    FALSE, 'Gym City'),
    ('Appel client important',   DATE_ADD(CURDATE(), INTERVAL 5 DAY), '10:00:00', 'Présentation offre commerciale','Professionnel','Importante',TRUE,'Bureau'),
    ('Anniversaire Maman',       DATE_ADD(CURDATE(), INTERVAL 14 DAY),'00:00:00', 'Ne pas oublier le gâteau !',   'Personnel', 'Importante', TRUE,  'Maison familiale'),
    ('Conférence Tech',          DATE_ADD(CURDATE(), INTERVAL 10 DAY),'09:00:00', 'DevFest Tunisia 2026',          'Professionnel','Normale',  FALSE, 'Parc des Expositions'),
    ('Révision Algo',            DATE_ADD(CURDATE(), INTERVAL 4 DAY), '20:00:00', 'Révision algorithmique avancée','Etudes',   'Normale',    FALSE, 'Bibliothèque');

-- ============================================================
--  TABLE : backup_log  (Fonctionnalité Backup/Restore)
-- ============================================================
CREATE TABLE backup_log (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    fichier     VARCHAR(500) NOT NULL,
    date_backup TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    type_op     ENUM('BACKUP','RESTORE') NOT NULL,
    statut      ENUM('SUCCÈS','ÉCHEC')   NOT NULL
);

-- Affichage de confirmation
SELECT CONCAT('✅ Base de données créée avec ', COUNT(*), ' événements de test.') AS message
FROM evenements;
