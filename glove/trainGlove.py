# One advantage of GloVe is that it can capture both the local context and the global statistical information of a word, 
# whereas word2vec only captures local context. 
# This makes GloVe more effective at capturing the meaning of rare and ambiguous words, 
# and it can also handle polysemous words more effectively. 
# However, word2vec is generally faster to train and requires less memory, 
# so it may be more practical for large-scale applications.


import gensim.downloader as api

# info = api.info()  # return dict with info about available models/datasets

glove_model = api.load("glove-twitter-25")  # load glove vectors trained on tweets
print("Finished loading the model")

similars = glove_model.most_similar("hello")  # show words that similar to word 'cat'

print(similars)