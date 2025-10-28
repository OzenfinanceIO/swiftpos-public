# SwiftPOS — Week 1 Proposal

## Project Pitch (what it is)
SwiftPOS is a Point of Sale app for small shops. It loads inventory from a CSV file, lets a user add items to a cart, calculates tax, and prints a text receipt.

## GUI Mockup
![GUI](src/docs/swiftpos_gui.png)

## CRC (quick)
- **Main**: handles flow, user input, cart, totals, writes receipt  
  *Uses:* Product  
- **Product**: sku, name, price, stock, category

## UML (mermaid)
```mermaid
classDiagram
  class Main {
    +loadInventory()
    +listProducts()
    +addToCart()
    +showCart()
    +checkout()
    +writeReceipt()
  }
  class Product {
    +sku:String
    +name:String
    +price:double
    +stock:int
    +category:String
  }
  Main --> Product
