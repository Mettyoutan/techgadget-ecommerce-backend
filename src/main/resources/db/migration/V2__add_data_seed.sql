INSERT INTO users (
    id, created_at, updated_at, username, email, password, role, full_name, phone_number
) VALUES (
    1,
    NOW(),
    NOW(),
    'admin',
    'admin@gmail.com',
    '$2a$10$Leh6.aUzTcyy7EI3qEOf4OGiY1w/CuJLiVrIq76LN1yfu3/np7yLC',
    'ADMIN',
    'Admin123',
    '08172633'
);

INSERT INTO categories (
    id, created_at, updated_at, name, description
) VALUES (
    1,
    NOW(),
    NOW(),
    'Smartphones',
    'Mobile phone and devices'
);

INSERT INTO categories (
    id, created_at, updated_at, name, description
) VALUES (
    2,
    NOW(),
    NOW(),
    'Laptops',
    'Personal computers and notebooks'
);

INSERT INTO categories (
    id, created_at, updated_at, name, description
) VALUES (
    3,
    NOW(),
    NOW(),
    'Audio',
    'Headphones, speakers, and audio equipment'
);

INSERT INTO products (
    created_at,
    updated_at,
    category_id,
    name,
    description,
    price_in_rupiah,
    stock_quantity,
    image_url,
    specs
)
VALUES
(
    NOW(),
    NOW(),
    (SELECT id FROM categories WHERE name = 'Smartphones'),
    'iPhone 15 Pro',
    'Latest Apple flagship with A17 Pro chip',
    14999000,
    50,
    'https://via.placeholder.com/300?text=iPhone+15',
    '{
      "screen": "6.1 inch",
      "processor": "A17 Pro",
      "ram": "8GB",
      "storage": "256GB"
    }'::jsonb
),
(
    NOW(),
    NOW(),
    (SELECT id FROM categories WHERE name = 'Smartphones'),
    'Samsung Galaxy S24',
    'Powerful Android flagship',
    12999000,
    45,
    'https://via.placeholder.com/300?text=Samsung+S24',
    '{
      "screen": "6.2 inch",
      "processor": "Snapdragon 8 Gen 3",
      "ram": "12GB",
      "storage": "512GB"
    }'::jsonb
),
(
    NOW(),
    NOW(),
    (SELECT id FROM categories WHERE name = 'Laptops'),
    'MacBook Pro 16"',
    'Professional laptop with M3 Max chip',
    53999000,
    20,
    'https://via.placeholder.com/300?text=MacBook+Pro',
    '{
      "processor": "M3 Max",
      "ram": "36GB",
      "storage": "1TB SSD",
      "display": "16 inch Retina"
    }'::jsonb
),
(
    NOW(),
    NOW(),
    (SELECT id FROM categories WHERE name = 'Audio'),
    'AirPods Pro 2',
    'Premium wireless earbuds with noise cancellation',
    3799000,
    100,
    'https://via.placeholder.com/300?text=AirPods+Pro',
    '{
      "type": "Earbuds",
      "battery": "6 hours",
      "charging_case": "30 hours",
      "noise_cancellation": "Active"
    }'::jsonb
);