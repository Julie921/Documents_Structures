############### IMPORTATION DES MODULES ###############

import sparknlp
from sparknlp.training import CoNLLU

import pyspark.sql.functions as f


from pyspark.ml.feature import Word2Vec

#######################################################

sc = sparknlp.start() #session de connexion

# df = CoNLLU().readDataset(sc, "corpus/test/converted_aave_test.conllu") #création d'un dataframe

# text_only=df.select(df.text) #on sélectionne que la colonne du text

# text_and_normalized=text_only.withColumn("normalized", f.lower(text_only.text)) #ajout de la colonne "normalized" où tout est en minuscules

# text_tokens=text_and_normalized.withColumn("tokens", f.split(text_and_normalized.normalized, " ")) #ajout de la colonne "tokens"

# text_tokens.show()

# ################# PIPELINE #########################

df_train = CoNLLU().readDataset(sc, "./Documents_Structures/clustered_clean_concatenated.conllu") #création d'un dataframe
df_dev = CoNLLU().readDataset(sc, "./Documents_Structures/corpus/dev/aave_dev.conllu") #création d'un dataframe


from sparknlp.annotator import PerceptronApproach

approach = PerceptronApproach() \
    .setInputCols(["document", "form"]) \
    .setOutputCol("pos") \
    .setPosColumn("upos")

trained_pos_model = approach.fit(df_train) # entraînement
annotated_df = trained_pos_model.transform(df_dev)

annotated_df.selectExpr("explode(pos.result)").show(truncate=False) # on affiche les pos prédites

tags = annotated_df.selectExpr("explode(pos.result)").collect() # les pos prédites pour notre corpus de dev

