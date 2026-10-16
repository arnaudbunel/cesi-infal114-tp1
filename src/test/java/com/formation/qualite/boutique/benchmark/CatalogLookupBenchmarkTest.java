package com.formation.qualite.boutique.benchmark;

import static org.assertj.core.api.Assertions.assertThat;

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

    private static final int REQUESTED_PRODUCTS = 200;

    @Test
    void compareLinearScanVersusMapLookup() {
        int[] catalogSizes = {100, 1_000, 10_000, 50_000};

        for (int size : catalogSizes) {
            List<Product> catalog = buildCatalog(size);
            List<Long> requestedIds = sampleIds(catalog, REQUESTED_PRODUCTS);

            long start = System.nanoTime();
            List<Product> foundByLinearScan = findByLinearScan(catalog, requestedIds);
            long linearDuration = elapsedMillisSince(start);

            Map<Long, Product> index = indexById(catalog);
            start = System.nanoTime();
            List<Product> foundByMap = findByMap(index, requestedIds);
            long mapDuration = elapsedMillisSince(start);

            // Les deux strategies doivent trouver exactement les memes produits.
            assertThat(foundByMap).isEqualTo(foundByLinearScan);

            System.out.printf(
                    "Catalogue de %d produits - scan lineaire : %d ms / recherche par Map : %d ms%n",
                    size, linearDuration, mapDuration);
        }
    }

    private static List<Product> buildCatalog(int size) {
        List<Product> catalog = new ArrayList<>();
        for (long i = 0; i < size; i++) {
            Product product = new Product("Produit " + i, 10.0, 100);
            product.setId(i);
            catalog.add(product);
        }
        return catalog;
    }

    private static List<Long> sampleIds(List<Product> catalog, int count) {
        List<Long> ids = new ArrayList<>();
        int step = Math.max(1, catalog.size() / count);
        for (int i = 0; i < catalog.size() && ids.size() < count; i += step) {
            ids.add(catalog.get(i).getId());
        }
        return ids;
    }

    private static List<Product> findByLinearScan(List<Product> catalog, List<Long> requestedIds) {
        List<Product> found = new ArrayList<>();
        for (Long id : requestedIds) {
            Product match = null;
            for (Product candidate : catalog) {
                if (candidate.getId().equals(id)) {
                    match = candidate;
                    break;
                }
            }
            if (match == null) {
                throw new IllegalStateException("Produit introuvable : " + id);
            }
            found.add(match);
        }
        return found;
    }

    private static Map<Long, Product> indexById(List<Product> catalog) {
        Map<Long, Product> index = new HashMap<>();
        for (Product product : catalog) {
            index.put(product.getId(), product);
        }
        return index;
    }

    private static List<Product> findByMap(Map<Long, Product> index, List<Long> requestedIds) {
        List<Product> found = new ArrayList<>();
        for (Long id : requestedIds) {
            Product match = index.get(id);
            if (match == null) {
                throw new IllegalStateException("Produit introuvable : " + id);
            }
            found.add(match);
        }
        return found;
    }

    private static long elapsedMillisSince(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }
}
