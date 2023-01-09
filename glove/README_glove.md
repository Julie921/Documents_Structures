# Vectorisation avec GloVe

`gensim` met à disposition plusieurs modèle pré-entraînés. On peut voir la liste des modèles comme ça :

```python
import gensim.downloader as api

info = api.info()  # return dict with info about available models/datasets
```

Les modèles qui pourriaent nous intéresser pour du fine-tuning sont : 

- 'glove-twitter-25',
- 'glove-twitter-50',
- 'glove-twitter-100',
- 'glove-twitter-200',

# Avantages et inconvénients de GloVe par rapport à word2vec

>One advantage of GloVe is that it can capture both the local context and the global statistical information of a word, whereas word2vec only captures local context. This makes GloVe more effective at capturing the meaning of rare and ambiguous words, and it can also handle polysemous words more effectively. However, word2vec is generally faster to train and requires less memory, so it may be more practical for large-scale applications.

Vu que c'est très long de même charger le modèle GloVe, on peut téléchager le `.bin` pour le charger localement. 

