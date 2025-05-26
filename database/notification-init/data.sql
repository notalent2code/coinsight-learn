-- Create notification service database
CREATE DATABASE notification_service;

-- Notification tables
CREATE TABLE
  notification_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    name VARCHAR(255) NOT NULL UNIQUE,
    subject VARCHAR(255) NOT NULL,
    body_template TEXT NOT NULL,
    notification_type VARCHAR(20) NOT NULL CHECK (notification_type IN ('EMAIL', 'SMS', 'PUSH')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  );

CREATE TABLE
  notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    user_id UUID NOT NULL,
    notification_type VARCHAR(20) NOT NULL,
    recipient VARCHAR(255) NOT NULL, -- email, phone, etc.
    subject VARCHAR(255),
    content TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (
      status IN ('PENDING', 'SENT', 'FAILED', 'DELIVERED')
    ),
    sent_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB -- for additional context
  );

-- Insert default templates
INSERT INTO
  notification_templates (name, subject, body_template, notification_type)
VALUES
  (
    'BUDGET_WARNING',
    'Budget Alert: {{budgetName}}',
    'Hi {{userName}}, your budget "{{budgetName}}" has reached {{percentage}}% ({{currentSpent}} out of {{budgetLimit}}). Consider reviewing your spending.',
    'EMAIL'
  ),
  (
    'BUDGET_EXCEEDED',
    'Budget Exceeded: {{budgetName}}',
    'Hi {{userName}}, your budget "{{budgetName}}" has been exceeded! You have spent {{currentSpent}} out of {{budgetLimit}}.',
    'EMAIL'
  ),
  (
    'WEEKLY_SUMMARY',
    'Weekly Spending Summary',
    'Hi {{userName}}, here is your weekly spending summary: Total spent: {{totalSpent}}, Top category: {{topCategory}}.',
    'EMAIL'
  );