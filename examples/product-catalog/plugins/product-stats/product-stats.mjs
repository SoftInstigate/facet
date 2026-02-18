/**
 * Product Stats Service
 *
 * A RESTHeart JavaScript plugin (runs on GraalVM) that computes aggregated
 * statistics for the product catalog by querying MongoDB directly via mclient.
 *
 * Demonstrates:
 *   - Writing a RESTHeart service in JavaScript instead of Java
 *   - Using the MongoDB Java driver from JavaScript via GraalVM interop
 *   - Returning computed/derived data that plain CRUD cannot provide
 *   - Pairing with a Facet template for server-side rendered HTML
 *
 * The response JSON is rendered as HTML by Facet when the request has
 * Accept: text/html, using templates/shop/stats/index.html.
 */

const BsonDocument = Java.type("org.bson.BsonDocument");

export const options = {
    name: "productStatsService",
    description: "Aggregated statistics for the product catalog",
    uri: "/shop/stats",
    secured: true,
    matchPolicy: "EXACT"
};

export function handle(request, response) {
    const db = mclient.getDatabase("shop");
    const coll = db.getCollection("products", BsonDocument.class);

    let total = 0;
    let outOfStock = 0;
    let priceSum = 0;
    let totalValue = 0;
    let minPrice = null;
    let maxPrice = null;
    const categoryMap = {};
    const lowStock = [];

    const it = coll.find().iterator();

    while (it.hasNext()) {
        const doc = it.next();

        const price  = doc.containsKey("price")    ? doc.getNumber("price").doubleValue()   : 0;
        const stock  = doc.containsKey("stock")    ? doc.getNumber("stock").intValue()      : 0;
        const cat    = doc.containsKey("category") ? doc.getString("category").getValue()   : "Unknown";
        const name   = doc.containsKey("name")     ? doc.getString("name").getValue()       : "Unknown";

        total++;
        priceSum   += price;
        totalValue += price * stock;

        if (stock === 0) outOfStock++;
        if (stock > 0 && stock <= 5) lowStock.push({ name, stock });

        if (minPrice === null || price < minPrice) minPrice = price;
        if (maxPrice === null || price > maxPrice) maxPrice = price;

        if (!categoryMap[cat]) categoryMap[cat] = { count: 0, totalValue: 0 };
        categoryMap[cat].count++;
        categoryMap[cat].totalValue += price * stock;
    }

    const avgPrice = total > 0 ? priceSum / total : 0;

    const categories = Object.entries(categoryMap)
        .map(([name, data]) => ({
            name,
            count: data.count,
            totalValue: round2(data.totalValue)
        }))
        .sort((a, b) => b.count - a.count);

    const stats = {
        total,
        inStock:    total - outOfStock,
        outOfStock,
        totalValue: round2(totalValue),
        avgPrice:   round2(avgPrice),
        minPrice:   minPrice === null ? 0 : round2(minPrice),
        maxPrice:   maxPrice === null ? 0 : round2(maxPrice),
        categories,
        lowStock
    };

    response.setContent(JSON.stringify(stats));
    response.setContentTypeAsJson();
}

function round2(n) {
    return Math.round(n * 100) / 100;
}
