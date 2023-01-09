# Vectorisation avec word2vec

J'ai écris un script `trainWord2vec.py` qui prend comme argument en corpus en texte brut et qui permet de créer un modèle word2vec, de l'entraîner sur le corpus donné en argument et de sauvegarder le modèle entraîner dans `word2vec_aave.model`.

Il serait intéressant d'essayer de load un modèle pré-entraîné sur un corpus Twitter et seulement après le fine-tune sur nos données.

Le script `similarityWithWord2vec.py` prend en argument un mot et affichage sur le terminal la liste des mots les plus similaires selon le modèle `word2vec_aave.model` avec les scores de similarité. 

# Vectorisation avec GloVe

`gensim` met à disposition plusieurs modèle pré-entraînés. On peut voir la liste des modèles comme ça :

```python
import gensim.downloader as api

info = api.info()  # return dict with info about available models/datasets
```

Les modèles GloVe qui pourriaent nous intéresser pour du fine-tuning sont : 

- 'glove-twitter-25',
- 'glove-twitter-50',
- 'glove-twitter-100',
- 'glove-twitter-200',

Les vecteurs GloVe peuvent être téléchargés ici : https://nlp.stanford.edu/projects/glove/. En fait, `gensim` propose une implémentation de word2vec mais pas de GloVe. Du coup on peut charger des vecteurs pré-entraînés de GloVe, mais si on veut les entraîner, il faut les convertir en vecteurs word2vec. On peut les convertir dans le terminal avec : 

```bash
python3 -m gensim.scripts.glove2word2vec -i <file_name_input> -o <file_name_output>.txt
```

et après load les vecteurs comme ça : 

```python
keyed_vectors = gensim.models.KeyedVectors.load_word2vec_format(<file.txt>, binary=False)
```

EDIT : En fait ce qu'on peut télécharger, c'est pas des modèles entiers, c'est des `KeyedVectors` et il nous faut le modèle entier pour continuer à entraîner. Du coup je pense que réentraîner avec les modèles pré-entraînés de GloVe, **c'est mort**.

# Avantages et inconvénients de GloVe par rapport à word2vec

>One advantage of GloVe is that it can capture both the local context and the global statistical information of a word, whereas word2vec only captures local context. This makes GloVe more effective at capturing the meaning of rare and ambiguous words, and it can also handle polysemous words more effectively. However, word2vec is generally faster to train and requires less memory, so it may be more practical for large-scale applications.

