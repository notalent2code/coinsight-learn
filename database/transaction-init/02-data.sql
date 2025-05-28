-- Transaction Service Initial Data

-- Insert default transaction categories
INSERT INTO transaction_categories (name, type) VALUES
-- Income categories
('Salary', 'income'),
('Freelance', 'income'),
('Investment Returns', 'income'),
('Business Income', 'income'),
('Rental Income', 'income'),
('Gift/Bonus', 'income'),
('Other Income', 'income'),

-- Expense categories
('Food & Dining', 'expense'),
('Transportation', 'expense'),
('Shopping', 'expense'),
('Entertainment', 'expense'),
('Bills & Utilities', 'expense'),
('Healthcare', 'expense'),
('Education', 'expense'),
('Travel', 'expense'),
('Insurance', 'expense'),
('Home & Garden', 'expense'),
('Fitness & Sports', 'expense'),
('Groceries', 'expense'),
('Rent/Mortgage', 'expense'),
('Fuel', 'expense'),
('Coffee & Snacks', 'expense'),
('Other Expense', 'expense')
ON CONFLICT (name) DO NOTHING;
