import stanza
from stanza.utils.conll import CoNLL
import os

nlp = stanza.Pipeline(lang='en', processors='tokenize,pos',tokenize_pretokenized=True)
print(nlp)

# Charger les données de test CoNLL
#test_data = '../data/udbase/UD_English-TEST/en_test-ud-test.conllu'
doc = CoNLL.conll2doc("../data/udbase/UD_English-TEST/en_test-ud-test.conllu")

# Effectuer l'étiquetage POS sur les données de test en utilisant le modèle personnalisé    
doc = nlp(doc)

# Afficher les résultats de l'étiquetage POS
for sent in doc.sentences:
     for word in sent.words:
        print(f'{word.text}\t{word.pos}')


# doc = nlp('Barack Obama was born in Hawaii.')
# print(*[f'word: {word.text}\tupos: {word.upos}\txpos: {word.xpos}\tfeats: {word.feats if word.feats else "_"}' for sent in doc.sentences for word in sent.words], sep='\n')
#CoNLL.write_doc2conll(doc2, "output.conllu")


une fonction qui prend en entrée un nom de conll et qui entraine et sauvegarde le modele
Une fonction qui prend en entrée un nom de conll et un modèle et qui prédit les pos du conll et renvoie les pos prédites dans une List[str]

def train_model(dossier):
    """
    Fonction qui prend en entrée un nom de dossier contenant trains/dev : UD_English-TEST
    et qui sort un modèle.
    Je pars du principe que tu es dans le dossier `stanza-main`
    """
    os.system("source scripts/config.sh")
    os.system("python3 -m stanza.utils.datasets.prepare_pos_treebank UD_English-TEST")
    os.system("python3 -m stanza.utils.training.run_pos UD_English-TEST --max_steps 200 --wordvec_pretrain_file ../../../stanza_resources/ar/pretrain/padt.pt")
    os.system("rm saved-models/pos")

def change_name(train_conllu,dev_conllu):
    os.system(f"mv {train_conllu} en_test-ud-train.conllu")
    os.system(f"mv {dev_conllu} en_test-ud-dev.conllu")

def deplace_files(train_conllu,dev_conllu):
    """
    fonction qui place les fichiers au bon endroit. Forcer l'emplacement pour que les futurs fichiers aux mêmes noms
    écrasent les anciens :)
    """
    pass 

def test_my_model(test_conllu):
    """
    test_conllu => chemin
    """
    
    result = []
    nlp = stanza.Pipeline(lang='en', processors='tokenize,pos',tokenize_pretokenized=True)

    # Charger les données de test CoNLL
    doc = CoNLL.conll2doc(test_conllu)

    # Effectuer l'étiquetage POS sur les données de test en utilisant le modèle personnalisé    
    doc = nlp(doc)

    # Afficher les résultats de l'étiquetage POS
    for sent in doc.sentences:
        for word in sent.words:
            result.append(str(word.pos))
            #print(f'{word.text}\t{word.pos}')

    return result