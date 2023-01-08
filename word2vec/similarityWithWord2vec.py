import gensim
import sys

if __name__=="__main__":
    
    word = sys.argv[1]
    
    model = gensim.models.Word2Vec.load("word2vec_aave.model")

    # Trouver les mots les plus similaires à 'texte'
    mots_similaires = model.wv.most_similar(word)

    # Afficher les mots similaires
    print(mots_similaires)