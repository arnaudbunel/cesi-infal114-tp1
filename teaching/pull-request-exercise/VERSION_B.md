# Fonctionnalite : indicateur de commande volumineuse

Besoin : lorsqu'une commande contient plus de 10 articles au total (toutes
lignes confondues), l'API doit pouvoir signaler qu'il s'agit d'une
"commande volumineuse" pour permettre un traitement logistique particulier.

## Version B

`controller/OrderController.java` :

```java
@GetMapping("/{id}/large-order")
public boolean isLargeOrder(@PathVariable Long id) {
    Order order = orderService.getOrderById(id);
    int count = 0;
    for (OrderLine line : order.getLines()) {
        count = count + line.getQuantity();
    }
    return count > 10;
}
```

`service/OrderService.java` (methode `generateOrderSummary`, modifiee pour
inclure l'information dans le texte genere) :

```java
public String generateOrderSummary(Long id) {
    Order order = getOrderById(id);
    InvoiceFormatter formatter = new InvoiceFormatter();

    int totalQuantity = 0;
    for (OrderLine line : order.getLines()) {
        totalQuantity = totalQuantity + line.getQuantity();
    }

    StringBuilder sb = new StringBuilder();
    sb.append(formatter.formatHeader(order)).append("\n");
    for (OrderLine line : order.getLines()) {
        sb.append(formatter.formatLine(line)).append("\n");
    }
    sb.append("Total : ").append(order.getFinalAmount()).append(" EUR\n");
    if (totalQuantity > 10) {
        sb.append("Commande volumineuse\n");
    }

    return sb.toString();
}
```

`service/InvoiceService.java` (le texte de facture doit lui aussi
mentionner l'information) :

```java
int articleCount = 0;
for (OrderLine line : order.getLines()) {
    articleCount = articleCount + line.getQuantity();
}
if (articleCount > 10) {
    invoice.append("Commande volumineuse\n");
}
```

Le seuil de 10 articles et la boucle de comptage sont ecrits
independamment dans le controleur, dans `OrderService` et dans
`InvoiceService`.
