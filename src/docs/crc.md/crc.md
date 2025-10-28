# CRC Cards

## Main
Responsibilities:
- Load inventory CSV, list products
- Add to cart (validate stock), show cart
- Compute subtotal/tax/total
- Checkout and write receipt under data/receipts
Collaborators: Product

## Product
Responsibilities:
- Hold item data (sku, name, price, stock, category)
Collaborators: Main
