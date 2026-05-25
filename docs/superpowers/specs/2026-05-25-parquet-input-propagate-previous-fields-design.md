# Design — Propagazione campi del transform precedente in ParquetInputEnhanced (issue #39)

- **Data:** 2026-05-25
- **Issue:** #39 — "Funzione 'Output fields' restituisce lista non coerente con runtime"
- **Transform interessato:** `ParquetInputEnhanced` (input)

## Problema

Eseguendo "Output fields" nel dialog del transform compaiono anche i campi
provenienti dal transform precedente, ma a runtime quei campi non vengono
propagati. La riproduzione avviene quando il transform legge i file da percorsi
espliciti, cioè con l'indicatore **"Accept file names from previous transform"
disattivato (OFF)**.

### Causa tecnica

- **Design-time:** Hop invoca `ParquetInputEnhancedMeta.getFields(inputRowMeta, ...)`
  passando un `inputRowMeta` che contiene già i campi del transform precedente.
  Il metodo si limita ad *aggiungere* i campi Parquet → "Output fields" mostra
  `campi precedenti + campi Parquet`.
- **Runtime:** in `ParquetInputEnhanced.processRow()` la riga di output è
  ricostruita da zero (`data.outputRowMeta = new RowMeta()`), quindi i campi del
  transform precedente non vengono mai propagati.

## Decisione

Allineare il runtime al design-time **propagando** i campi del transform
precedente, combinandoli con le righe Parquet tramite **prodotto cartesiano**.

### Scope

- Si applica **solo** quando "Accept file names from previous transform" è **OFF**
  (percorsi espliciti). È lo scenario dell'issue #39.
- La modalità **ON** resta **invariata**: le righe in ingresso continuano a essere
  consumate solo per costruire la lista file e non vengono propagate.

## Comportamento target

- Con un hop in ingresso collegato (modalità OFF): l'output è il prodotto
  cartesiano `righe_in_ingresso × righe_Parquet`. Ogni riga emessa è
  `[campi precedenti] + [campi Parquet] + [campi info-file]`.
- Senza hop in ingresso: comportamento invariato → solo righe Parquet.
- Ordine dei campi: precedenti → Parquet → info-file. Coerente con il design-time,
  dove `getFields` aggiunge i campi Parquet a quelli già presenti in `inputRowMeta`.

## Modifiche

### Runtime

1. **`ParquetInputEnhancedData`** — nuovi campi:
   - `List<Object[]> previousRows`
   - `IRowMeta previousRowMeta`

2. **`ParquetInputEnhanced.processRow()`** (blocco `first`, ramo `else` con
   acceptingFilenames OFF):
   - Se esiste un input row set, leggere in loop (`getRowFrom`) e **bufferizzare**
     tutte le righe in ingresso, salvando il relativo `RowMeta` in
     `data.previousRowMeta`.
   - Pre-popolare `data.outputRowMeta` con `previousRowMeta` **prima** di chiamare
     `meta.getFields(...)`, così l'ordine resta precedenti→Parquet→info-file.

3. **`ParquetInputEnhanced.getRowsFromParquetFile()`** — all'emissione:
   - Se `previousRows` non è vuoto, per ogni riga precedente costruire la riga
     combinata (`RowDataUtil.createResizedCopy(prev, outputRowMeta.size())` e copia
     della porzione Parquet/info-file dopo i campi precedenti), poi `putRow`.
   - Altrimenti `putRow` invariato (solo riga Parquet).

### Design-time

Nessuna modifica a `getFields`: in modalità OFF mostra già
`campi precedenti + Parquet`, che ora sarà coerente con il runtime.

## Test

Test che verifichino:

1. N righe in ingresso × M righe Parquet → N×M righe in output, con i campi
   concatenati nell'ordine corretto (precedenti → Parquet → info-file).
2. Nessun hop in ingresso → solo righe Parquet (comportamento invariato).

## Note e limiti

- Il buffering di tutte le righe in ingresso è intrinseco al prodotto cartesiano
  (possibile uso di memoria elevato con input molto grandi). Accettabile e atteso
  per questa semantica.
- Aggiungere una voce sotto `[Unreleased]` nel `CHANGELOG.md` (cambiamento visibile
  all'utente).
