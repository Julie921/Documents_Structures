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

