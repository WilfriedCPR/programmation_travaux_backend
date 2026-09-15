-- Evolution V3 SONABEL - script idempotent exécuté avant Hibernate validate.
ALTER TABLE IF EXISTS agent ADD COLUMN IF NOT EXISTS actif BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS app_notification (
    id VARCHAR(255) PRIMARY KEY,
    agent_id VARCHAR(255) NOT NULL,
    titre VARCHAR(180) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    type VARCHAR(60) NOT NULL,
    lien VARCHAR(500),
    date_creation TIMESTAMP NOT NULL,
    date_lecture TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_notification_agent_date ON app_notification(agent_id, date_creation DESC);
CREATE INDEX IF NOT EXISTS idx_notification_agent_unread ON app_notification(agent_id, date_lecture);

CREATE TABLE IF NOT EXISTS push_subscription (
    id VARCHAR(255) PRIMARY KEY,
    agent_id VARCHAR(255) NOT NULL,
    endpoint TEXT NOT NULL UNIQUE,
    p256dh TEXT,
    auth_key TEXT,
    date_creation TIMESTAMP NOT NULL,
    derniere_utilisation TIMESTAMP NOT NULL,
    actif BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS idx_push_subscription_agent ON push_subscription(agent_id, actif);

CREATE TABLE IF NOT EXISTS activity_log (
    id VARCHAR(255) PRIMARY KEY,
    type VARCHAR(80) NOT NULL,
    description VARCHAR(1200) NOT NULL,
    acteur VARCHAR(255),
    reference VARCHAR(255),
    devis_id VARCHAR(255),
    agent_id VARCHAR(255),
    date_creation TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_activity_log_date ON activity_log(date_creation DESC);
CREATE INDEX IF NOT EXISTS idx_activity_log_agent ON activity_log(agent_id, date_creation DESC);


-- Libère les affectations encore actives sur des devis déjà clôturés/supprimés.
-- L'historique est conservé grâce à date_retrait_affectation.
UPDATE affectation_devis a
SET active = FALSE,
    date_retrait_affectation = COALESCE(a.date_retrait_affectation, CURRENT_TIMESTAMP)
FROM tr_devis d
WHERE a.devis_id = d.id
  AND a.active = TRUE
  AND LOWER(COALESCE(d.devis_status, '')) IN ('clos', 'cloture', 'inactif', 'supprime');

-- Evolution V4 : traçabilité directe des affectations.
ALTER TABLE IF EXISTS affectation_devis ADD COLUMN IF NOT EXISTS affecte_par VARCHAR(255);
ALTER TABLE IF EXISTS affectation_devis ADD COLUMN IF NOT EXISTS retire_par VARCHAR(255);
