# Entraînement de Stanza

```bash
git clone https://github.com/stanfordnlp/stanza`
git clone https://github.com/stanfordnlp/stanza-train #pour tester
```

## Setting Environment Variable

Modifier le fichier `config.sh` avec les bons chemins dans `stanza-train`.

```bash
source config.sh
```
## Conversion des données dans le bon format

```python
python3 -m stanza.utils.datasets.prepare_${pos}_treebank ${corpus}
```

https://stanfordnlp.github.io/stanza/training.html