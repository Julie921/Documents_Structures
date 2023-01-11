import stanza
from stanza.utils.conll import CoNLL
import os

def train_model():
    """
    Fonction qui prend en entrée un nom de dossier contenant trains/dev : UD_English-TEST
    et qui sort un modèle.
    Je pars du principe que tu es dans le dossier `stanza-main`
    """
    os.system("source ./tools/stanfordnlp/stanza-train/config/config.sh")
    os.system("python3 -m stanza.utils.datasets.prepare_pos_treebank UD_English-TEST")
    os.system("python3 -m stanza.utils.training.run_pos UD_English-TEST --max_steps 200 --wordvec_pretrain_file ../../../stanza_resources/ar/pretrain/padt.pt")

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
    doc = CoNLL.conll2doc(f"{test_conllu}.conllu")

    # Effectuer l'étiquetage POS sur les données de test en utilisant le modèle personnalisé    
    doc = nlp(doc)

    # Afficher les résultats de l'étiquetage POS
    for sent in doc.sentences:
        for word in sent.words:
            result.append(str(word.pos))
            #print(f'{word.text}\t{word.pos}')


    return result
