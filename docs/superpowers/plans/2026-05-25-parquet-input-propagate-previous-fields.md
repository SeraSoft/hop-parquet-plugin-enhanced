# Propagazione campi del transform precedente (ParquetInputEnhanced) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A runtime, quando "Accept file names from previous transform" è OFF, propagare i campi del transform precedente combinandoli con le righe Parquet tramite prodotto cartesiano, in modo coerente con la lista mostrata da "Output fields" (issue #39).

**Architecture:** La logica di combinazione viene estratta in un metodo statico puro `combineWithPreviousRows` (testabile senza I/O Parquet). `processRow()` bufferizza tutte le righe in ingresso e pre-popola l'`outputRowMeta` con i campi precedenti prima di aggiungere i campi Parquet. `getRowsFromParquetFile()` emette il prodotto cartesiano quando esistono righe precedenti, altrimenti il comportamento attuale.

**Tech Stack:** Java 11, Apache Hop 2.16.2, JUnit Jupiter 5, Mockito.

---

## Contesto di riferimento (leggere prima di iniziare)

- Modulo: `transforms/parquet-enhanced/`
- Classi coinvolte (package `org.apache.hop.parquet.transforms.input`):
  - `ParquetInputEnhanced` — logica di esecuzione (`processRow`, `getRowsFromParquetFile`)
  - `ParquetInputEnhancedData` — holder runtime
- Comportamento attuale rilevante:
  - In `processRow()` (blocco `if (first)`), dopo l'eventuale ramo `acceptingFilenames`, l'output viene ricostruito da zero:
    ```java
    data.outputRowMeta = new RowMeta(); // start from scratch!
    meta.getFields(data.outputRowMeta, getTransformName(), null, null, this, metadataProvider);
    handleMissingFiles();
    ```
  - In `getRowsFromParquetFile()`, ogni riga Parquet (più gli eventuali campi info-file) viene emessa con:
    ```java
    putRow(data.outputRowMeta, row);
    r = data.reader.read();
    ```
    dove `row = r.getData()` è dimensionato per i soli campi Parquet + info-file.
- `getInputRowSets()`, `getRowFrom(IRowSet)`, `putRow(IRowMeta, Object[])` sono ereditati da `BaseTransform`.

## Build / test commands

- Compile: `mvn -q -pl transforms/parquet-enhanced -am clean test-compile -s .m2/settings.xml`
- Test mirato: `mvn -q -pl transforms/parquet-enhanced test -Dtest=ParquetInputEnhancedTest -s .m2/settings.xml`
- Full verify: `mvn clean verify -s .m2/settings.xml`
- Richiede le variabili d'ambiente `GITHUB_ACTOR` e `GITHUB_TOKEN`.

---

## Task 1: Campi runtime per il buffer delle righe precedenti

**Files:**
- Modify: `transforms/parquet-enhanced/src/main/java/org/apache/hop/parquet/transforms/input/ParquetInputEnhancedData.java`

- [ ] **Step 1: Aggiungere i due campi al data holder**

In `ParquetInputEnhancedData`, aggiungere gli import e i due campi pubblici (dopo il campo `public IRowSet rowSet;`).

Import in cima al file (dopo gli import esistenti `java.*`):
```java
import java.util.List;
```
(`IRowMeta` è già importato indirettamente? Verificare: aggiungere se assente)
```java
import org.apache.hop.core.row.IRowMeta;
```

Nuovi campi nella classe:
```java
  /** Buffer of all rows coming from the previous transform (cartesian product source). */
  public List<Object[]> previousRows;

  /** Row metadata of the rows coming from the previous transform. */
  public IRowMeta previousRowMeta;
```

- [ ] **Step 2: Verificare la compilazione**

Run: `mvn -q -pl transforms/parquet-enhanced -am clean test-compile -s .m2/settings.xml`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add transforms/parquet-enhanced/src/main/java/org/apache/hop/parquet/transforms/input/ParquetInputEnhancedData.java
git commit -m "feat(input): add previous-rows buffer fields to ParquetInputEnhancedData (#39)"
```

---

## Task 2: Helper statico `combineWithPreviousRows` (TDD)

Logica pura di prodotto cartesiano riga-per-riga, isolata per essere testabile senza I/O Parquet.

**Files:**
- Modify: `transforms/parquet-enhanced/src/main/java/org/apache/hop/parquet/transforms/input/ParquetInputEnhanced.java`
- Test: `transforms/parquet-enhanced/src/test/java/org/apache/hop/parquet/transforms/input/ParquetInputEnhancedTest.java` (Create)

- [ ] **Step 1: Scrivere il test che fallisce**

Creare il file di test con header licenza ASF (copiare l'header da `ParquetInputEnhancedMetaTest.java`) e il seguente contenuto:

```java
package org.apache.hop.parquet.transforms.input;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParquetInputEnhancedTest {

  @Test
  void cartesianProductCombinesEachPreviousRowWithParquetRow() {
    // 2 previous rows, 2 previous fields each
    List<Object[]> previousRows = new ArrayList<>();
    previousRows.add(new Object[] {"a1", "b1"});
    previousRows.add(new Object[] {"a2", "b2"});

    // parquet row holds 3 values (parquet + file-info) at indices [0..3)
    Object[] parquetRow = new Object[] {"p1", "p2", "p3"};

    int previousFieldCount = 2;
    int outputSize = 5; // 2 previous + 3 parquet

    List<Object[]> combined =
        ParquetInputEnhanced.combineWithPreviousRows(
            previousRows, previousFieldCount, parquetRow, outputSize);

    assertEquals(2, combined.size());
    assertArrayEquals(new Object[] {"a1", "b1", "p1", "p2", "p3"}, combined.get(0));
    assertArrayEquals(new Object[] {"a2", "b2", "p1", "p2", "p3"}, combined.get(1));
  }

  @Test
  void combinedRowsAreSizedToOutputSize() {
    List<Object[]> previousRows = new ArrayList<>();
    previousRows.add(new Object[] {"a"});

    Object[] parquetRow = new Object[] {"p"};

    List<Object[]> combined =
        ParquetInputEnhanced.combineWithPreviousRows(previousRows, 1, parquetRow, 2);

    assertEquals(1, combined.size());
    assertEquals(2, combined.get(0).length);
    assertEquals("a", combined.get(0)[0]);
    assertEquals("p", combined.get(0)[1]);
  }

  @Test
  void parquetRowMayBeLargerThanUsedPortion() {
    // parquetRow oversized (read support allocates spare capacity); only the
    // first (outputSize - previousFieldCount) values must be copied.
    List<Object[]> previousRows = new ArrayList<>();
    previousRows.add(new Object[] {"a"});

    Object[] parquetRow = new Object[] {"p1", "p2", null, null};

    List<Object[]> combined =
        ParquetInputEnhanced.combineWithPreviousRows(previousRows, 1, parquetRow, 3);

    assertEquals(1, combined.size());
    assertArrayEquals(new Object[] {"a", "p1", "p2"}, combined.get(0));
  }

  @Test
  void emptyPreviousRowsProducesEmptyResult() {
    List<Object[]> combined =
        ParquetInputEnhanced.combineWithPreviousRows(
            new ArrayList<>(), 0, new Object[] {"p"}, 1);
    assertTrue(combined.isEmpty());
  }
}
```

- [ ] **Step 2: Eseguire il test per verificarne il fallimento**

Run: `mvn -q -pl transforms/parquet-enhanced test -Dtest=ParquetInputEnhancedTest -s .m2/settings.xml`
Expected: FAIL in compilazione — il metodo `ParquetInputEnhanced.combineWithPreviousRows` non esiste.

- [ ] **Step 3: Implementare il metodo statico minimo**

In `ParquetInputEnhanced`, assicurarsi degli import `java.util.ArrayList` e `java.util.List` (aggiungerli se assenti) e inserire il metodo (es. subito dopo `getRowsFromParquetFile`):

```java
  /**
   * Builds the cartesian product between the buffered previous-transform rows and a single Parquet
   * row. Each resulting row is sized to {@code outputSize}: the first {@code previousFieldCount}
   * positions are filled from a previous row, the remaining positions from the (used portion of the)
   * Parquet row.
   */
  static List<Object[]> combineWithPreviousRows(
      List<Object[]> previousRows, int previousFieldCount, Object[] parquetRow, int outputSize) {
    int parquetPortion = outputSize - previousFieldCount;
    List<Object[]> result = new ArrayList<>(previousRows.size());
    for (Object[] previousRow : previousRows) {
      Object[] combined = new Object[outputSize];
      System.arraycopy(previousRow, 0, combined, 0, previousFieldCount);
      System.arraycopy(parquetRow, 0, combined, previousFieldCount, parquetPortion);
      result.add(combined);
    }
    return result;
  }
```

- [ ] **Step 4: Eseguire il test per verificarne il successo**

Run: `mvn -q -pl transforms/parquet-enhanced test -Dtest=ParquetInputEnhancedTest -s .m2/settings.xml`
Expected: PASS (4 test verdi).

- [ ] **Step 5: Commit**

```bash
git add transforms/parquet-enhanced/src/main/java/org/apache/hop/parquet/transforms/input/ParquetInputEnhanced.java transforms/parquet-enhanced/src/test/java/org/apache/hop/parquet/transforms/input/ParquetInputEnhancedTest.java
git commit -m "feat(input): add combineWithPreviousRows cartesian helper (#39)"
```

---

## Task 3: Bufferizzare le righe precedenti in `processRow()`

**Files:**
- Modify: `transforms/parquet-enhanced/src/main/java/org/apache/hop/parquet/transforms/input/ParquetInputEnhanced.java` (metodo `processRow`, blocco `if (first)`)

- [ ] **Step 1: Aggiungere gli import necessari**

Verificare/aggiungere in cima a `ParquetInputEnhanced.java`:
```java
import java.util.ArrayList;
import java.util.List;
import org.apache.hop.core.row.IRowSet;
```
(`RowMeta` è già importato; in caso contrario aggiungere `import org.apache.hop.core.row.RowMeta;`.)

- [ ] **Step 2: Inserire il buffering e pre-popolare l'outputRowMeta**

Nel metodo `processRow`, sostituire il blocco esistente:

```java
      if (meta.isAcceptingFilenames()) {
        // Read the files from the specified input stream...
        data.files.getFiles().clear();
        // ... (corpo invariato del ramo acceptingFilenames) ...
      }

      data.outputRowMeta = new RowMeta(); // start from scratch!
      meta.getFields(data.outputRowMeta, getTransformName(), null, null, this, metadataProvider);

      handleMissingFiles();
```

con:

```java
      if (meta.isAcceptingFilenames()) {
        // Read the files from the specified input stream...
        data.files.getFiles().clear();
        // ... (corpo invariato del ramo acceptingFilenames) ...
      } else {
        // Not accepting filenames: buffer all incoming rows so each Parquet row can be emitted as a
        // cartesian product with the previous-transform rows (issue #39).
        data.previousRows = new ArrayList<>();
        List<IRowSet> inputRowSets = getInputRowSets();
        if (!inputRowSets.isEmpty()) {
          IRowSet previousRowSet = inputRowSets.get(0);
          Object[] previousRow = getRowFrom(previousRowSet);
          while (previousRow != null) {
            if (data.previousRowMeta == null) {
              data.previousRowMeta = previousRowSet.getRowMeta().clone();
            }
            data.previousRows.add(previousRow);
            previousRow = getRowFrom(previousRowSet);
          }
        }
      }

      data.outputRowMeta = new RowMeta(); // start from scratch!
      if (data.previousRowMeta != null) {
        // Previous fields first, then Parquet + file-info fields (matches design-time getFields).
        data.outputRowMeta.addRowMeta(data.previousRowMeta);
      }
      meta.getFields(data.outputRowMeta, getTransformName(), null, null, this, metadataProvider);

      handleMissingFiles();
```

> NOTA: lasciare invariato l'intero corpo del ramo `if (meta.isAcceptingFilenames())` (il commento `// ... invariato ...` qui indica solo di non toccarlo).

- [ ] **Step 3: Verificare la compilazione**

Run: `mvn -q -pl transforms/parquet-enhanced -am clean test-compile -s .m2/settings.xml`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add transforms/parquet-enhanced/src/main/java/org/apache/hop/parquet/transforms/input/ParquetInputEnhanced.java
git commit -m "feat(input): buffer previous-transform rows in processRow (#39)"
```

---

## Task 4: Emettere il prodotto cartesiano in `getRowsFromParquetFile()`

**Files:**
- Modify: `transforms/parquet-enhanced/src/main/java/org/apache/hop/parquet/transforms/input/ParquetInputEnhanced.java` (metodo `getRowsFromParquetFile`)

- [ ] **Step 1: Sostituire l'emissione della riga**

Nel ciclo `while (r != null && !isStopped())` di `getRowsFromParquetFile`, individuare la riga di emissione esistente:

```java
        putRow(data.outputRowMeta, row);
        r = data.reader.read();
```

e sostituirla con:

```java
        if (data.previousRows == null || data.previousRows.isEmpty()) {
          putRow(data.outputRowMeta, row);
        } else {
          for (Object[] combinedRow :
              combineWithPreviousRows(
                  data.previousRows,
                  data.previousRowMeta.size(),
                  row,
                  data.outputRowMeta.size())) {
            putRow(data.outputRowMeta, combinedRow);
          }
        }
        r = data.reader.read();
```

- [ ] **Step 2: Verificare compilazione e test esistenti**

Run: `mvn -q -pl transforms/parquet-enhanced test -s .m2/settings.xml`
Expected: BUILD SUCCESS, tutti i test verdi (compresi i 4 di `ParquetInputEnhancedTest`).

- [ ] **Step 3: Commit**

```bash
git add transforms/parquet-enhanced/src/main/java/org/apache/hop/parquet/transforms/input/ParquetInputEnhanced.java
git commit -m "feat(input): emit cartesian product of previous rows and Parquet rows (#39)"
```

---

## Task 5: Voce CHANGELOG

**Files:**
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Aggiungere la voce sotto `[Unreleased]`**

Sotto la sezione `[Unreleased]`, nella categoria `### Fixed` (crearla se assente), aggiungere:

```markdown
- ParquetInputEnhanced: i campi provenienti dal transform precedente sono ora propagati a runtime (prodotto cartesiano con le righe Parquet) quando "Accept file names from previous transform" è disattivato, allineando l'output a quanto mostrato da "Output fields" (#39).
```

- [ ] **Step 2: Commit**

```bash
git add CHANGELOG.md
git commit -m "docs: changelog entry for issue #39"
```

---

## Verifica manuale finale (post-implementazione)

Non testabile via unit test (richiede la GUI/runtime Hop): in una pipeline reale, collegare un transform che genera N righe a `ParquetInputEnhanced` con percorsi espliciti (acceptingFilenames OFF) su un file con M righe; verificare che l'output contenga N×M righe con i campi nell'ordine precedenti→Parquet→info-file. Verificare inoltre che, senza hop in ingresso, l'output resti di sole righe Parquet.

## Note finali

- Aggiornare la checklist dell'issue #39 su GitHub man mano che i task vengono completati.
- Tutto il lavoro su branch dedicato; PR con `Closes #39`.
