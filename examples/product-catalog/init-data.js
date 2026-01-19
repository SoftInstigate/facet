// MongoDB initialization script for Product Catalog example
// This script runs automatically when the MongoDB container first starts

print('Initializing Product Catalog example data...');

// Switch to shop database
db = db.getSiblingDB('shop');

// Create products collection if it doesn't exist
db.createCollection('products');

// Insert sample products
// Using placehold.co for placeholder images (external service, no local assets needed)
db.products.insertMany([
  {
    name: 'Laptop Pro 15"',
    description: 'High-performance laptop with 16GB RAM and 512GB SSD',
    price: 1299.99,
    category: 'Electronics',
    stock: 15,
    imageUrl: 'https://placehold.co/400x300/4A5568/FFF?text=Laptop',
    tags: ['computer', 'portable', 'business'],
    createdAt: new Date()
  },
  {
    name: 'Wireless Mouse',
    description: 'Ergonomic wireless mouse with adjustable DPI',
    price: 29.99,
    category: 'Electronics',
    stock: 50,
    imageUrl: 'https://placehold.co/400x300/4A5568/FFF?text=Mouse',
    tags: ['accessory', 'wireless'],
    createdAt: new Date()
  },
  {
    name: 'Mechanical Keyboard',
    description: 'RGB mechanical keyboard with Cherry MX switches',
    price: 149.99,
    category: 'Electronics',
    stock: 30,
    imageUrl: 'https://placehold.co/400x300/4A5568/FFF?text=Keyboard',
    tags: ['accessory', 'gaming', 'rgb'],
    createdAt: new Date()
  },
  {
    name: 'Office Desk Chair',
    description: 'Ergonomic office chair with lumbar support',
    price: 299.99,
    category: 'Furniture',
    stock: 8,
    imageUrl: 'https://placehold.co/400x300/059669/FFF?text=Chair',
    tags: ['furniture', 'office', 'ergonomic'],
    createdAt: new Date()
  },
  {
    name: 'Standing Desk',
    description: 'Adjustable height standing desk with electric motor',
    price: 499.99,
    category: 'Furniture',
    stock: 5,
    imageUrl: 'https://placehold.co/400x300/059669/FFF?text=Desk',
    tags: ['furniture', 'office', 'adjustable'],
    createdAt: new Date()
  },
  {
    name: 'Coffee Maker Deluxe',
    description: 'Programmable coffee maker with thermal carafe',
    price: 89.99,
    category: 'Appliances',
    stock: 20,
    imageUrl: 'https://placehold.co/400x300/DC2626/FFF?text=Coffee+Maker',
    tags: ['kitchen', 'beverage'],
    createdAt: new Date()
  },
  {
    name: '4K Monitor 27"',
    description: 'Ultra HD monitor with IPS panel and HDR support',
    price: 449.99,
    category: 'Electronics',
    stock: 12,
    imageUrl: 'https://placehold.co/400x300/4A5568/FFF?text=Monitor',
    tags: ['display', 'monitor', '4k'],
    createdAt: new Date()
  },
  {
    name: 'USB-C Hub',
    description: '7-in-1 USB-C hub with HDMI, USB 3.0, and SD card reader',
    price: 39.99,
    category: 'Electronics',
    stock: 40,
    imageUrl: 'https://placehold.co/400x300/4A5568/FFF?text=USB-C+Hub',
    tags: ['accessory', 'adapter'],
    createdAt: new Date()
  },
  {
    name: 'Desk Lamp LED',
    description: 'Adjustable LED desk lamp with touch controls',
    price: 34.99,
    category: 'Furniture',
    stock: 25,
    imageUrl: 'https://placehold.co/400x300/059669/FFF?text=Lamp',
    tags: ['lighting', 'desk', 'led'],
    createdAt: new Date()
  },
  {
    name: 'Noise Cancelling Headphones',
    description: 'Over-ear headphones with active noise cancellation',
    price: 199.99,
    category: 'Electronics',
    stock: 18,
    imageUrl: 'https://placehold.co/400x300/4A5568/FFF?text=Headphones',
    tags: ['audio', 'wireless', 'noise-cancelling'],
    createdAt: new Date()
  }
]);

print('Inserted ' + db.products.countDocuments() + ' products into shop.products collection');

// Create useful indexes
db.products.createIndex({ category: 1 });
db.products.createIndex({ price: 1 });
db.products.createIndex({ name: 'text', description: 'text' });

print('Created indexes on products collection');
print('Product Catalog initialization complete!');
