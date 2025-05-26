-- Create budget service database
CREATE DATABASE budget_service;

-- Budget tables
CREATE TABLE
  budgets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    user_id UUID NOT NULL,
    category_id INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    period VARCHAR(20) NOT NULL CHECK (period IN ('WEEKLY', 'MONTHLY', 'YEARLY')),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    current_spent DECIMAL(12, 2) DEFAULT 0.00,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  );

CREATE TABLE
  budget_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    budget_id UUID NOT NULL REFERENCES budgets (id),
    threshold_percentage INTEGER NOT NULL,
    alert_type VARCHAR(20) NOT NULL CHECK (alert_type IN ('EMAIL', 'SMS', 'PUSH')),
    is_enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (budget_id) REFERENCES budgets (id) ON DELETE CASCADE
  );

CREATE TABLE
  budget_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    budget_id UUID NOT NULL REFERENCES budgets (id),
    transaction_id UUID NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    previous_spent DECIMAL(12, 2) NOT NULL,
    new_spent DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (budget_id) REFERENCES budgets (id) ON DELETE CASCADE
  );

-- Indexes for performance
CREATE INDEX idx_budgets_user_id ON budgets (user_id);

CREATE INDEX idx_budgets_category_id ON budgets (category_id);

CREATE INDEX idx_budget_alerts_budget_id ON budget_alerts (budget_id);