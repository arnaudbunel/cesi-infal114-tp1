# Fonctionnalite : indicateur de commande volumineuse

Besoin : lorsqu'une commande contient plus de 10 articles au total (toutes
lignes confondues), l'API doit pouvoir signaler qu'il s'agit d'une
"commande volumineuse" pour permettre un traitement logistique particulier.

## Version A

`model/Order.java` :

```java
public boolean isLargeOrder() {
    return getTotalItemCount() > 10;
}

public int getTotalItemCount() {
    int count = 0;
    for (OrderLine line : lines) {
        count += line.getQuantity();
    }
    return count;
}
```

`controller/OrderController.java` :

```java
@GetMapping("/{id}/large-order")
public boolean isLargeOrder(@PathVariable Long id) {
    Order order = orderService.getOrderById(id);
    return order.isLargeOrder();
}
```

Aucune autre classe n'est modifiee. Le calcul du nombre d'articles est
centralise dans l'entite `Order` et reutilise partout ou c'est necessaire.
