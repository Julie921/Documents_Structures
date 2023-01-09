import gensim
import sys
import re

def clean(tokens, pattern_clean):
    return [token.lower() for token in tokens if re.match(pattern_clean, token)]

def tokenize(text):
    return [token for token in text.split()]

def preprocess(text, pattern_clean):
    tokens = tokenize(text)
    tokens_cleaned = clean(tokens, pattern_clean)
    return tokens_cleaned

if __name__=="__main__":
    
    pattern_clean = re.compile("\w+")
    
    corpus = sys.argv[1]
    
    with open(corpus, "r", encoding="utf-8") as f: 
        text = f.readlines()
        
    corpus_clean = [preprocess(sentence, pattern_clean) for sentence in text]
    
    model = gensim.models.Word2Vec(min_count=1, vector_size=250, window=10, workers=4)
    model.build_vocab(corpus_clean)  # prepare the model vocabulary
    model.train(corpus_clean, total_examples=model.corpus_count, epochs=model.epochs)  # train word vectors
    
    model.save("word2vec_aave.model")
    