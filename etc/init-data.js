// Minimal seed data for Facet quickstart
// Runs on first Mongo container initialization

const db = db.getSiblingDB('mydb');

if (db.products.countDocuments() === 0) {
  db.products.insertMany([
    {
      name: "Starter Laptop",
      price: 999,
      category: "Electronics",
      createdAt: new Date()
    },
    {
      name: "Office Chair",
      price: 199,
      category: "Furniture",
      createdAt: new Date()
    },
    {
      name: "Trail Backpack",
      price: 89,
      category: "Outdoors",
      createdAt: new Date()
    }
  ]);
}
