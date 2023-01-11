############### IMPORTATION DES MODULES ###############

import sparknlp
from sparknlp.training import CoNLLU

# import pyspark.sql.functions as f
from sparknlp.annotator import PerceptronApproach

import sys
#######################################################

# df = CoNLLU().readDataset(sc, "corpus/test/converted_aave_test.conllu") #création d'un dataframe

# text_only=df.select(df.text) #on sélectionne que la colonne du text

# text_and_normalized=text_only.withColumn("normalized", f.lower(text_only.text)) #ajout de la colonne "normalized" où tout est en minuscules

# text_tokens=text_and_normalized.withColumn("tokens", f.split(text_and_normalized.normalized, " ")) #ajout de la colonne "tokens"

# text_tokens.show()

# ################# PIPELINE #########################

def main():
    """Pour lancer le script : 
    
    ```bash
    python3 trainSparkNLP.py train.conllu dev.conllu path/folder/to/save/model
    ```
    
    **Note : il faut que les corpus soient déjà nettoyés**
    """
    
    sc = sparknlp.start() #session de connexion
    
    conll_train = sys.argv[1] # corpus de train
    conll_dev = sys.argv[2] # corpus de dev pour tester le modèle
    model_dir = sys.argv[3] # le dossier dans lequel on veut sauvegarder le modèle
    
    # lecture des corpus : on store dans un dataframe
    df_train = CoNLLU().readDataset(sc, conll_train) # corpus de train
    df_dev = CoNLLU().readDataset(sc, conll_dev) # corpus de dev
    
    # pos tagger basé sur un perceptron
    approach = PerceptronApproach() \
    .setInputCols(["document", "form"]) \
    .setOutputCol("pos") \
    .setPosColumn("upos")

    trained_pos_model = approach.fit(df_train) # entraînement
    annotated_df = trained_pos_model.transform(df_dev) # pos tagging sur le dev avec notre modèle entraîné

    annotated_df.selectExpr("explode(pos.result)").show(truncate=False) # on affiche les pos prédites avec une pos par ligne (explode)

    tags = annotated_df.selectExpr("explode(pos.result)").collect() # les pos prédites pour notre corpus de dev

    # sauvegarde du modèle entraîné
    trained_pos_model.save(model_dir)


if __name__=="__main__":
    main()