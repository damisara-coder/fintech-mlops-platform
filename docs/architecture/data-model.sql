-- =========================================================================
-- Schéma Relationnel PostgreSQL - Plateforme FinTech MLOps (Tâche P1.1)
-- Base de données : fintech_db
-- =========================================================================

-- Extension pour UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- -------------------------------------------------------------------------
-- 1. Table des Clients (customers)
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
    id VARCHAR(64) PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(30),
    country VARCHAR(2) NOT NULL,
    risk_profile VARCHAR(20) DEFAULT 'STANDARD', -- STANDARD, LOW, HIGH
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- -------------------------------------------------------------------------
-- 2. Table des Comptes Bancaires (accounts)
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS accounts (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    account_number VARCHAR(34) UNIQUE NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',
    balance NUMERIC(15, 2) DEFAULT 0.00,
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, SUSPENDED, FROZEN
    opened_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    account_age_days INT GENERATED ALWAYS AS (DATE_PART('day', NOW() - opened_at)) STORED
);

-- -------------------------------------------------------------------------
-- 3. Table des Transactions (transactions)
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transactions (
    id VARCHAR(64) PRIMARY KEY, -- ex: TX-9F219
    account_id VARCHAR(64) NOT NULL REFERENCES accounts(id),
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',
    channel VARCHAR(20) NOT NULL, -- card, wire, sepa
    merchant_name VARCHAR(150),
    merchant_category VARCHAR(50),
    ip_address VARCHAR(45),
    ip_country VARCHAR(2),
    ip_risk NUMERIC(5, 4) DEFAULT 0.0,
    status VARCHAR(30) DEFAULT 'PENDING', -- PENDING, EVALUATED, FAILED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index pour optimiser les filtres UI et les agrégations du Dashboard
CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON transactions(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_transactions_account ON transactions(account_id);

-- -------------------------------------------------------------------------
-- 4. Table des Décisions de Risque (decisions)
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS decisions (
    id VARCHAR(64) PRIMARY KEY,
    transaction_id VARCHAR(64) UNIQUE NOT NULL REFERENCES transactions(id),
    decision VARCHAR(30) NOT NULL, -- APPROVED, BLOCKED, HUMAN_REVIEW
    probability NUMERIC(5, 4) NOT NULL,
    uncertainty_score NUMERIC(5, 4) NOT NULL,
    is_uncertain BOOLEAN NOT NULL DEFAULT FALSE,
    risk_score INT NOT NULL, -- 0 à 100
    risk_band VARCHAR(20) NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL
    model_version VARCHAR(50) NOT NULL, -- ex: v4.2
    latency_ms INT,
    review_status VARCHAR(30) DEFAULT 'NONE', -- NONE, PENDING_REVIEW, REVIEWED_APPROVED, REVIEWED_REJECTED
    reviewer_notes TEXT,
    decided_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index pour la Human Review Queue (triée par incertitude décroissante)
CREATE INDEX IF NOT EXISTS idx_decisions_uncertainty ON decisions(uncertainty_score DESC) WHERE decision = 'HUMAN_REVIEW';
CREATE INDEX IF NOT EXISTS idx_decisions_decision ON decisions(decision);
CREATE INDEX IF NOT EXISTS idx_decisions_risk_band ON decisions(risk_band);

-- -------------------------------------------------------------------------
-- 5. Table du Journal d'Audit Immuable à Hash Chaîné (audit_log)
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    audit_id VARCHAR(64) UNIQUE NOT NULL, -- ex: a-88213
    transaction_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(50) NOT NULL, -- TRANSACTION_CREATED, RISK_EVALUATED, HUMAN_REVIEW_SUBMITTED
    payload_json JSONB NOT NULL,
    previous_hash VARCHAR(64) NOT NULL,
    current_hash VARCHAR(64) NOT NULL,
    actor VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index sur transaction et horodatage pour traçabilité réglementaire
CREATE INDEX IF NOT EXISTS idx_audit_tx ON audit_log(transaction_id);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_log(timestamp DESC);
