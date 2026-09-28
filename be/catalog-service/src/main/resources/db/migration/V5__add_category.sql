CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Seed default categories
INSERT INTO categories (id, name, created_at) VALUES 
    (gen_random_uuid(), 'Electronics', NOW()),
    (gen_random_uuid(), 'Vehicles', NOW()),
    (gen_random_uuid(), 'Fashion', NOW()),
    (gen_random_uuid(), 'Collectibles', NOW()),
    (gen_random_uuid(), 'Home & Garden', NOW()),
    (gen_random_uuid(), 'OTHER', NOW());
