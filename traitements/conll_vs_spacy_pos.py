from typing import List, Set
import spacy
from spacy import Language
from spacy.tokens import Doc
#from sklearn.metrics import classification_report, confusion_matrix, ConfusionMatrixDisplay

# Fonction qui prend en entrée un fichier.
# Elle renvoie une liste de lignes.
def lire_corpus(corpus_file: str) -> List[str]:
    result = []
    with open(corpus_file) as corpus:
        for line in corpus:
            result.append(line)
    return result



#Fonction qui prend en entrée un fichier CONLL
#Elle renvoie une liste de phrase
#Chaque phrase est une liste de tuple (token,pos)
def lire_conll(corpus_file: str) -> List[List[tuple]]:
    
    lignes = []
    with open(f"{corpus_file}.conllu", "r", encoding="utf-8") as conll:
        for line in conll:
            lignes.append(line)

    liste_phrase = [] # liste de liste dans laquelle chaque sous-liste est une phrase
    phrase = [] # liste de tokens par phrase

    for ligne in lignes:
        if ligne[0]=="#" or ligne[0]=="\n": # changement de phrase dans le conll
            if phrase: # si phrase n'est pas vide
                liste_phrase.append(phrase)
                phrase = []
        else:
            ligne = ligne.split("\t")
            id, token, _, pos, _, _, _, _, _, _= ligne
            if "-" not in id: # mots contractés
                phrase.append((token,pos)) # on récupère la forme du token et la pos
    return liste_phrase



# Fonction de Pierre
# Fonction qui prend en entrée un fichier CONLL
# Elle envoie une liste de liste de strings
# La grande liste correspond à toutes les phrases
# Une sous-liste = liste des tokens d'une phrase donnée
# def lire_conll(corpus_file:str) ->List[List[str]]:
#     sentences = []
#     sentence = []
#     with open(corpus_file) as corpus:
#         for line in corpus:
#             split = line.split("\t")
#             if line[0].isdigit() and "_" != split[2]: #ou if line[0] != "#" :
#                 sentence.append(split[1])
#             else:
#                 if len(sentence) > 0:
#                     sentences.append(sentence)
#                 sentence = []
#     return sentences



# Fonction qui prend en entrée le modèle "Language" de spacy et un corpus brut sous la forme d'une liste de string
# Ca imprime les tokens 
def process_corpus_brut(model: Language, corpus: List[str]):
    for sentence in corpus:
        doc = model(sentence)
        for token in doc:
            print(token.text)



# Fonction qui prend en entrée le chemin vers un fichier CONLL
# Elle renvoie une liste des tokens de ce CONLL
# Ca sert à récupérer le lexique connu du ficheir de train
def get_lexique(corpus_path:str) -> Set[str]:
    
    lexique = []
    
    with open(f"{corpus_path}.conllu", "r", encoding="utf-8") as f:
        lignes = f.readlines()
        
    for ligne in lignes:
        if ligne[0]=="#" or ligne[0]=="\n":
            pass
        else:
            ligne = ligne.split("\t")
            id, token, lemme, pos, _, _, _, _, _, _= ligne
            if "-" not in id:
                lexique.append(token)
                
    return set(lexique) # pour enlever les doublons 



# Fonction qui prend en entrée le modèle "Language" de spacy, un corpus sous la forme d'une liste de liste de tuples (token, pos) et le lexique présent dans le train (ensemble)
# vu qu'on prend en entrée les tokens extraits du conll, on *force* la tokenization du conll
# Ca renvoie une liste de tuples (token du conll, pos du conll, pos de spacy, True/False pour oov)
def process_tokenized_corpus(model: Language, corpus: List[List[tuple]], lexique: Set[str]) -> List[str]:
    
    y_pred = [] #liste qui contiendra les tuples (token, pos de conll, pos de spacy, True/False pour oov)
    y_true = []
    for phrase in corpus: #dans corpus, chaque sous-liste correspond à une phrase
        
        # listes tampon pour chaque phrase
        tokens = []
        spacy_pos = []
        conll_pos = []
        
        for token in phrase:
            tokens.append(token[0]) # text du token du conll
            y_true.append(token[1])

        doc = Doc(model.vocab, tokens) 
        model(doc) # analyse de la phrase par spacy en utilisant la tokenization du conll
        
        for token in doc: # pour chaque token de la phrase
            y_pred.append(token.tag_) # on récupère la pos de spacy
            
       # for pos in spacy_pos, :
            #y_pred.append(pos) # on met tout dans une liste de tuples
        
    return y_pred, y_true



# Fonction qui prend en entrée une liste de tuples (token, pos de conll, pos de spacy, True/False pour oov)
# Elle renvoie le score d'accuracy
def total_accuracy(conll_vs_spacy: List[tuple]) -> float:
    
    somme_bon = 0 # somme des mots dont la pos est la même pour spacy et le conll
    
    for tuple in conll_vs_spacy:
        if tuple[1]==tuple[2]: # tuple[1] = pos du conll, tuple[2] = pos de spacy
            somme_bon+=1
            
    accuracy = somme_bon/len(conll_vs_spacy) * 100
    
    return accuracy



# Fonction qui renvoie le score d'accuracy des mots inconnus et celui des mots connus
# Elle prend en entrée une liste de tuples (token, pos de conll, pos de spacy, True/False pour oov)
def known_oov_accuracy(conll_vs_spacy: List[tuple]) -> float:
    
    oov_correct = 0 # nombre de mots inconnus bien annotés par spacy
    total_oov = 0 # nombre total de mots inconnus
    
    known_correct = 0 # nombre de mots connus bien annotés
    total_known = 0 # nombre total de mots connus
    
    for tuple in conll_vs_spacy:
        if tuple[3]==True: #si le mot est inconnu (out of vocabulary)
            total_oov+=1
            if tuple[1]==tuple[2]:
                oov_correct+=1
        if tuple[3]==False: # si le mot est connu
            total_known+=1
            if tuple[1]==tuple[2]:
                known_correct+=1
    
    oov_accuracy = oov_correct/total_oov * 100
    known_accuracy = known_correct/total_known *100
    
    return oov_accuracy, known_accuracy



# Définition de la fonction main
def main():
    
    model = spacy.load("fr_core_news_lg")
    
    test_corpus_path = "./fr_sequoia-ud-test.conllu"
    test_corpus = lire_conll(test_corpus_path) # liste de liste de tuples (token, pos) extraits du conll
    
    train_corpus_path = "fr_sequoia-ud-train.conllu"
    lexique = get_lexique(train_corpus_path)
    
    conll_vs_spacy = process_tokenized_corpus(model, test_corpus, lexique) #donne une liste avec des tuples (token, pos de conll, pos de spacy, True/False pour oov)
    
    total_accuracy_score = total_accuracy(conll_vs_spacy)
    print(f"total accuracy : {total_accuracy_score}")
    
    oov_accuracy, known_accuracy = known_oov_accuracy(conll_vs_spacy)
    
    print(f"oov accuracy : {oov_accuracy}")
    print(f"known accuracy : {known_accuracy}")
 
 
        
if __name__ == '__main__':
    
    main()