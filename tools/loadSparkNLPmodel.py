import sys
import sparknlp
from sparknlp.annotator import PerceptronModel
from sparknlp.training import CoNLLU

def main():
    """ Pour lancer le script : 
    
    ```bash
    python3 loadSparkNLPmodel.py path/to/model/directory test.conllu
    ```
    """
    model_dir = sys.argv[1]
    test_corpus = sys.argv[2]
    
    sc = sparknlp.start() #session de connexion
    df_dev = CoNLLU().readDataset(sc, test_corpus) # corpus de dev
    
    loaded_model = PerceptronModel.load(model_dir) # chargement du modèle entraîné
    annotated_test_corpus = loaded_model.transform(df_dev) # annotation en utilisant notre modèle
    
    annotated_test_corpus.selectExpr("explode(pos.result)").show(truncate=False) # on affiche les pos prédites avec une pos par ligne (explode)
    
    predicted_tags = annotated_test_corpus.selectExpr("explode(pos.result)").collect() 
    print(type(predicted_tags))
    

if __name__=="__main__":
    main()