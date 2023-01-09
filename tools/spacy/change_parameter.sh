#!/usr/bin/env bash -c "#! utf-8"


# python3 -m spacy train config.cfg --output ./output/ --paths.train corpus/aave_best_cmuc.spacy --paths.dev corpus/devwire_cmuc.spacy

# Les paramètres qu'on peut tester : 
# --training.batch-sizer #
# --training.dropout #
# --training.optimizer.learn_rate #
# -- training.max_epochs
# --training.max_steps
# --training.eval_frequency

# Charger les données d'entraînement et de test
train_data=$2
dev_data=$3
output=$1

# Définir les différents paramètres à tester
parameters=(
  "--nlp.batch_size 100"
  "--nlp.batch_size 500"
  "--nlp.batch_size 1000"
)

# Pour chaque jeu de paramètres, entraîner un modèle et évaluer sa performance
for params in "${parameters[@]}"; do
  # Entraîner le modèle avec les paramètres donnés
  echo "Paramètres : $params";
  python3 -m spacy train config.cfg --output $output --paths.train $train_data --paths.dev $dev_data $params;
  #python -m spacy train config.cfg --paths.train ./corpus/aave_best_cmuc.spacy --paths.dev ./corpus/devwire_cmuc.spacy --nlp.batch_size 128

done;