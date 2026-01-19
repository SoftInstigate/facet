// MongoDB initialization script for Product Catalog example
// This script runs automatically when the MongoDB container first starts

print('Initializing Product Catalog example data...');

// Switch to shop database
db = db.getSiblingDB('shop');

// Create products collection if it doesn't exist
db.createCollection('products');

// Insert sample products
db.products.insertMany([
  {
    name: 'Laptop Pro 15"',
    description: 'High-performance laptop with 16GB RAM and 512GB SSD',
    price: 1299.99,
    category: 'Electronics',
    stock: 15,
    imageUrl: '/assets/images/laptop.jpg',
    tags: ['computer', 'portable', 'business'],
    createdAt: new Date()
  },
  {
    name: 'Wireless Mouse',
    description: 'Ergonomic wireless mouse with adjustable DPI',
    price: 29.99,
    category: 'Electronics',
    stock: 50,
    imageUrl: '/assets/images/mouse.jpg',
    tags: ['accessory', 'wireless'],
    createdAt: new Date()
  },
  {
    name: 'Mechanical Keyboard',
    description: 'RGB mechanical keyboard with Cherry MX switches',
    price: 149.99,
    category: 'Electronics',
    stock: 30,
    imageUrl: '/assets/images/keyboard.jpg',
    tags: ['accessory', 'gaming', 'rgb'],
    createdAt: new Date()
  },
  {
    name: 'Office Desk Chair',
    description: 'Ergonomic office chair with lumbar support',
    price: 299.99,
    category: 'Furniture',
    stock: 8,
    imageUrl: '/assets/images/chair.jpg',
    tags: ['furniture', 'office', 'ergonomic'],
    createdAt: new Date()
  },
  {
    name: 'Standing Desk',
    description: 'Adjustable height standing desk with electric motor',
    price: 499.99,
    category: 'Furniture',
    stock: 5,
    imageUrl: '/assets/images/desk.jpg',
    tags: ['furniture', 'office', 'adjustable'],
    createdAt: new Date()
  },
  {
    name: 'Coffee Maker Deluxe',
    description: 'Programmable coffee maker with thermal carafe',
    price: 89.99,
    category: 'Appliances',
    stock: 20,
    imageUrl: '/assets/images/coffee.jpg',
    tags: ['kitchen', 'beverage'],
    createdAt: new Date()
  },
  {
    name: '4K Monitor 27"',
    description: 'Ultra HD monitor with IPS panel and HDR support',
    price: 449.99,
    category: 'Electronics',
    stock: 12,
    imageUrl: '/assets/images/monitor.jpg',
    tags: ['display', 'monitor', '4k'],
    createdAt: new Date()
  },
  {
    name: 'USB-C Hub',
    description: '7-in-1 USB-C hub with HDMI, USB 3.0, and SD card reader',
    price: 39.99,
    category: 'Electronics',
    stock: 40,
    imageUrl: '/assets/images/hub.jpg',
    tags: ['accessory', 'adapter'],
    createdAt: new Date()
  },
  {
    name: 'Desk Lamp LED',
    description: 'Adjustable LED desk lamp with touch controls',
    price: 34.99,
    category: 'Furniture',
    stock: 25,
    imageUrl: '/assets/images/lamp.jpg',
    tags: ['lighting', 'desk', 'led'],
    createdAt: new Date()
  },
  {
    name: 'Noise Cancelling Headphones',
    description: 'Over-ear headphones with active noise cancellation',
    price: 199.99,
    category: 'Electronics',
    stock: 18,
    imageUrl: '/assets/images/headphones.jpg',
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
