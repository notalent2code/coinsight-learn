-- Notification Service Database Schema

-- Create notification_templates table
CREATE TABLE IF NOT EXISTS notification_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    subject VARCHAR(255) NOT NULL,
    body_template TEXT NOT NULL,
    notification_type VARCHAR(20) NOT NULL CHECK (notification_type IN ('EMAIL', 'SMS', 'PUSH')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    notification_type VARCHAR(20) NOT NULL CHECK (notification_type IN ('EMAIL', 'SMS', 'PUSH')),
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255),
    content TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'DELIVERED')),
    sent_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

-- Create processed_messages table for idempotency
CREATE TABLE IF NOT EXISTS processed_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(50) NOT NULL,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_status ON notifications(status);
CREATE INDEX IF NOT EXISTS idx_notifications_user_status ON notifications(user_id, status);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);

-- Create indexes for processed_messages
CREATE INDEX IF NOT EXISTS idx_processed_messages_message_id ON processed_messages(message_id);
CREATE INDEX IF NOT EXISTS idx_processed_messages_processed_at ON processed_messages(processed_at);

-- Insert default notification templates
INSERT INTO notification_templates (name, subject, body_template, notification_type) VALUES
('BUDGET_WARNING', 'Budget Alert: {{budgetName}}', 
 'Hi {{userName}}, your budget "{{budgetName}}" has reached {{percentage}}% ({{currentSpent}} out of {{budgetLimit}}). Consider reviewing your spending.', 
 'EMAIL'),
('BUDGET_EXCEEDED', 'Budget Exceeded: {{budgetName}}', 
 'Hi {{userName}}, your budget "{{budgetName}}" has been exceeded! You have spent {{currentSpent}} out of {{budgetLimit}}.', 
 'EMAIL'),
('BUDGET_INFO', 'Budget Update: {{budgetName}}', 
 'Hi {{userName}}, your budget "{{budgetName}}" has been updated. Current spending: {{currentSpent}} out of {{budgetLimit}}.', 
 'EMAIL'),
('WEEKLY_SUMMARY', 'Weekly Spending Summary', 
 'Hi {{userName}}, here is your weekly spending summary: Total spent: {{totalSpent}}, Top category: {{topCategory}}.', 
 'EMAIL')
ON CONFLICT (name) DO NOTHING;

-- Create function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger for notification_templates table
CREATE TRIGGER update_notification_templates_updated_at BEFORE UPDATE ON notification_templates
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();