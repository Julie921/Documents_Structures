# Vectorisation avec word2vec

J'ai écris un script `trainWord2vec.py` qui prend comme argument en corpus en texte brut et qui permet de créer un modèle word2vec, de l'entraîner sur le corpus donné en argument et de sauvegarder le modèle entraîner dans `word2vec_aave.model`.

Il serait intéressant d'essayer de load un modèle pré-entraîné sur un corpus Twitter et seulement après le fine-tune sur nos données.

Le script `similarityWithWord2vec.py` prend en argument un mot et affichage sur le terminal la liste des mots les plus similaires selon le modèle `word2vec_aave.model` avec les scores de similarité. d