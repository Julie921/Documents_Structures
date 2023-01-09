import gensim.downloader as api

# info = api.info()  # return dict with info about available models/datasets

glove_model = api.load("glove-twitter-25")  # load glove vectors trained on tweets
print("Finished loading the model")

similars = glove_model.most_similar("hello")  # show words that similar to word 'cat'

print(similars)