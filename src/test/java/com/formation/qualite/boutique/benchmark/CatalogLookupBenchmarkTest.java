package com.formation.qualite.boutique.benchmark;

import com.formation.qualite.boutique.model.Product;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Compare empiriquement, pour differentes tailles de catalogue, le cout
 * d'une recherche produit par parcours lineaire (approche actuelle de
 * OrderService.createOrder) et par Map indexee sur l'identifiant.
 *
 * Desactive par defaut pour ne pas alourdir "mvn test". Retirer
 * l'annotation @Disabled pour l'executer dans le cadre de l'exercice
 * d'analyse de complexite algorithmique.
 */
@Disabled("Benchmark manuel pour l'exercice Big O - retirer l'annotation pour l'executer")
class CatalogLookupBenchmarkTest {

    @Test
    void compareLinearScanVersusMapLookup() {
        int[] catalogSizes = {100, 1_000, 10_000, 50_000};

        for (int size : catalogSizes) {
            List<Product> catalog = buildCatalog(size);
            List<Long> requestedIds = sampleIds(catalog, 200);

            long linearDuration = timeLinearScan(catalog, requestedIds);
            long mapDuration = timeMapLookup(catalog, requestedIds);

            System.out.printf(
                    "Catalogue de %d produits - scan lineaire : %d ms / recherche par Map : %d ms%n",
                    size, linearDuration, mapDuration);
        }
    }

    private List<Product> buildCatalog(int size) {
        List<Product> catalog = new ArrayList<>();
        for (long i = 0; i < size; i++) {
            Product product = new Product("Produit " + i, 10.0, 100);
            product.setId(i);
            catalog.add(product);
        }
        return catalog;
    }

    private List<Long> sampleIds(List<Product> catalog, int count) {
        List<Long> ids = new ArrayList<>();
        int step = Math.max(1, catalog.size() / count);
        for (int i = 0; i < catalog.size() && ids.size() < count; i += step) {
            ids.add(catalog.get(i).getId());
        }
        return ids;
    }

    private long timeLinearScan(List<Product> catalog, List<Long> requestedIds) {
        long start = System.nanoTime();
        for (Long id : requestedIds) {
            Product found = null;
            for (Product candidate : catalog) {
                if (candidate.getId().equals(id)) {
                    found = candidate;
                    break;
                }
            }
            if (found == null) {
                throw new IllegalStateException("Produit introuvable : " + id);
            }
        }
        return (System.nanoTime() - start) / 1_000_000;
    }

    private long timeMapLookup(List<Product> catalog, List<Long> requestedIds) {
        Map<Long, Product> index = new HashMap<>();
        for (Product product : catalog) {
            index.put(product.getId(), product);
        }

        long start = System.nanoTime();
        for (Long id : requestedIds) {
            Product found = index.get(id);
            if (found == null) {
                throw new IllegalStateException("Produit introuvable : " + id);
            }
        }
        return (System.nanoTime() - start) / 1_000_000;
    }
}
