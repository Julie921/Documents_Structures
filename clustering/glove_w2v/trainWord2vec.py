import gensim
import sys
import dendoun.extraction as dde

if __name__=="__main__":
    
    my_conll = sys.argv[1]
        
    corpus_clean = dde.get_only_tokens_text(my_conll)
    
    model = gensim.models.Word2Vec(min_count=1, vector_size=250, window=10, workers=4)
    model.build_vocab(corpus_clean)  # prepare the model vocabulary
    model.train(corpus_clean, total_examples=model.corpus_count, epochs=model.epochs)  # train word vectors
    
    model.save("word2vec_aave.model")
    